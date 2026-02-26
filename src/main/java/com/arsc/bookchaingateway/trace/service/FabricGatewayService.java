package com.arsc.bookchaingateway.trace.service;


import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import jakarta.annotation.PostConstruct; // 适配 Spring Boot 3.x
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Service
public class FabricGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(FabricGatewayService.class);

    // 基础配置
    private static final String MSP_ID = "Org1MSP";
    private static final String CHANNEL_NAME = "mychannel";
    private static final String CHAINCODE_NAME = "booktrace";

    // WSL2 中 Peer 节点的地址
    // WSL2 默认会将 localhost 端口映射到 Windows，所以直接用 localhost 即可
    private static final String PEER_ENDPOINT = "localhost:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private Gateway gateway;
    private Network network;
    private Contract contract;

    @PostConstruct
    public void init() throws Exception {
        logger.info("正在初始化 Fabric 网关连接...");

        // 1. 从 resources/network 读取证书和私钥流
        InputStream tlsCertStream = new ClassPathResource("network/tls-ca.crt").getInputStream();
        InputStream userCertStream = new ClassPathResource("network/user-cert.pem").getInputStream();
        InputStream userKeyStream = new ClassPathResource("network/user-key.pem").getInputStream();

        // 🌟 将 InputStream 转换为官方要求的 Reader
        Reader certReader = new InputStreamReader(userCertStream, StandardCharsets.UTF_8);
        Reader keyReader = new InputStreamReader(userKeyStream, StandardCharsets.UTF_8);

        // 2. 建立 gRPC TLS 安全通道
        ManagedChannel channel = Grpc.newChannelBuilder(PEER_ENDPOINT,
                        TlsChannelCredentials.newBuilder().trustManager(tlsCertStream).build())
                .overrideAuthority(OVERRIDE_AUTH)
                .build();

        // 3. 构建身份 (Identity) 和签名器 (Signer)
        X509Certificate certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity(MSP_ID, certificate); // 使用 X509Identity 构造器

        PrivateKey privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        // 4. 连接网关
        gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .connect();

        // 5. 获取网络通道和智能合约实例
        network = gateway.getNetwork(CHANNEL_NAME);
        contract = network.getContract(CHAINCODE_NAME);

        logger.info("成功连接到 Fabric 区块链网络！");
        // 启动事件监听线程
        startEventListener();
    }

    /**
     * 调用智能合约将新图书上链 (初始录入)
     */
    public String createBook(String bookId, String bookName, String publisher, String currentLocation,
                             String operator, String operatorRole) throws Exception {
        logger.debug("正在向区块链提交【图书上链】交易: bookId={}, bookName={}, publisher={}, location={},operator={}, operatorRole={}",
                bookId, bookName, publisher, currentLocation, operator, operatorRole);
        // submitTransaction 提交写入操作，参数顺序必须和智能合约里的 createBook 方法参数一致
        byte[] result = contract.submitTransaction("createBook", bookId, bookName, publisher, currentLocation, operator, operatorRole);
        String resultStr =  new String(result, StandardCharsets.UTF_8);
        logger.info("图书上链交易成功: bookId={}", bookId);
        return resultStr;
    }
    /**
     * 调用智能合约查询图书 (只读，速度快)
     */
    public String queryBook(String bookId) throws Exception {
        logger.debug("正在查询图书信息: bookId={}", bookId);
        // evaluateTransaction 用于查询操作，不产生新区块
        byte[] result = contract.evaluateTransaction("queryBook", bookId);
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * 调用智能合约更新图书位置 (写入账本，需要全网共识)
     */
    public String updateBookLocation(String bookId, String newLocation, String newStatus,
                                     String operator, String operatorRole) throws Exception {
        logger.debug("正在向区块链提交更新交易: bookId={}, newLocation={}, newStatus={},operator={}, operatorRole={}",
                bookId, newLocation, newStatus, operator, operatorRole);
        // submitTransaction 用于写入/修改操作，会自动处理节点背书和排序流程
        byte[] result = contract.submitTransaction("updateBookLocation", bookId, newLocation, newStatus, operator, operatorRole);
        String resultStr = new String(result, StandardCharsets.UTF_8);
        logger.info("图书更新交易成功: bookId={}", bookId);
        return resultStr;
    }

    /**
     * 调用智能合约获取图书完整的流转轨迹
     */
    public String getBookHistory(String bookId) throws Exception {
        logger.debug("正在查询图书历史溯源数据: bookId={}", bookId);
        byte[] result = contract.evaluateTransaction("getBookHistory", bookId);
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * 调用智能合约删除图书
     */
    public String deleteBook(String bookId) throws Exception {
        logger.debug("正在向区块链提交删除交易: bookId={}", bookId);
        // submitTransaction 提交删除操作
        contract.submitTransaction("deleteBook", bookId);
        String result = "图书 [" + bookId + "] 已成功从当前账本状态中删除！";
        logger.info("图书删除交易成功: bookId={}", bookId);
        return result;
    }

    /**
     * 启动区块链事件监听器 (后台独立线程)
     */
    private void startEventListener() {
        logger.info("正在启动区块链全局事件监听器...");

        // 开启一个新线程，防止阻塞主程序的启动
        new Thread(() -> {
            try {
                // 获取当前智能合约产生的所有事件流
                CloseableIterator<ChaincodeEvent> eventIter = network.getChaincodeEvents(CHAINCODE_NAME);

                logger.info("监听器已就绪，正在等待区块链网络广播...");

                // 死循环持续监听
                while (eventIter.hasNext()) {
                    ChaincodeEvent event = eventIter.next();
                    String eventName = event.getEventName();
                    String payload = new String(event.getPayload(), StandardCharsets.UTF_8);

                    logger.info("[区块链实时广播] 捕获到账本变更事件! 事件类型: {}, 交易 ID: {}, 业务数据: {}", 
                            eventName, event.getTransactionId(), payload);

                    // 💡 架构拓展提示：
                    // 在正式环境里，你可以写一个 switch(eventName) 分支：
                    // 如果是 BookCreatedEvent -> 调用 MySQL 的 insert 同步数据
                    // 如果是 BookDeletedEvent -> 调用 MySQL 的 delete，并发邮件通知管理员
                }
            } catch (Exception e) {
                logger.error("事件监听器异常: {}", e.getMessage(), e);
            }
        }).start();
    }
}
