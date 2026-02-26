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
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class FabricGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(FabricGatewayService.class);

    private static final String CHANNEL_NAME = "mychannel";
    private static final String CHAINCODE_NAME = "booktrace";

    // 🌟 核心：多租户智能合约路由表
    private final Map<String, Contract> contractMap = new HashMap<>();

    @PostConstruct
    public void init() throws Exception {
        logger.info("==================================================");
        logger.info("🔄 正在初始化【省级联盟链多租户网关】...");

        // 1. 初始化 Org1 (出版集团)
        Contract org1Contract = initOrgContract("Org1MSP", "localhost:7051", "peer0.org1.example.com", "org1");
        contractMap.put("ORG1", org1Contract);

        // 2. 初始化 Org2 (物流企业)
        Contract org2Contract = initOrgContract("Org2MSP", "localhost:9051", "peer0.org2.example.com", "org2");
        contractMap.put("ORG2", org2Contract);

        // 3. 初始化 Org3 (书店/终端)
        Contract org3Contract = initOrgContract("Org3MSP", "localhost:11051", "peer0.org3.example.com", "org3");
        contractMap.put("ORG3", org3Contract);

        logger.info("✅ 多组织身份路由配置完毕！金库大门已全面敞开！");
        logger.info("==================================================");
    }

    /**
     * 通用的机构初始化方法，动态读取对应文件夹下的证书
     */
    private Contract initOrgContract(String mspId, String peerEndpoint, String overrideAuth, String orgDir) throws Exception {
        logger.info(">> 正在加载机构身份: {} (Endpoint: {})", mspId, peerEndpoint);

        InputStream tlsCertStream = new ClassPathResource("network/" + orgDir + "/tls-ca.crt").getInputStream();
        InputStream userCertStream = new ClassPathResource("network/" + orgDir + "/user-cert.pem").getInputStream();
        InputStream userKeyStream = new ClassPathResource("network/" + orgDir + "/user-key.pem").getInputStream();

        Reader certReader = new InputStreamReader(userCertStream, StandardCharsets.UTF_8);
        Reader keyReader = new InputStreamReader(userKeyStream, StandardCharsets.UTF_8);

        ManagedChannel channel = Grpc.newChannelBuilder(peerEndpoint,
                        TlsChannelCredentials.newBuilder().trustManager(tlsCertStream).build())
                .overrideAuthority(overrideAuth)
                .build();

        X509Certificate certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity(mspId, certificate);

        PrivateKey privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        Gateway gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(channel)
                // 超时时间统一设为 30 秒，防止首次调用时容器冷启动导致报错
                .evaluateOptions(options -> options.withDeadlineAfter(30, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(30, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(30, TimeUnit.SECONDS))
                .connect();

        Network network = gateway.getNetwork(CHANNEL_NAME);

        // 只需要用 Org1 的网络去监听全局事件即可，防止多机构重复打印
        if ("org1".equals(orgDir)) {
            startEventListener(network);
        }

        return network.getContract(CHAINCODE_NAME);
    }

    /**
     * 智能路由选择器：根据机构ID获取对应的 Contract
     */
    private Contract getContract(String orgId) {
        if (orgId == null || orgId.trim().isEmpty()) {
            orgId = "ORG1"; // 默认兜底
        }
        Contract contract = contractMap.get(orgId.toUpperCase());
        if (contract == null) {
            throw new RuntimeException("非法的机构路由ID: " + orgId);
        }
        return contract;
    }

    // ================= 业务方法 (第一个参数全变成了 orgId) =================

    public String createBook(String orgId, String bookId, String bookName, String publisher, String currentLocation,
                             String operator, String operatorRole) throws Exception {
        logger.debug("[{}] 发起【图书上链】交易: bookId={}, bookName={}, operator={}", orgId, bookId, bookName, operator);
        byte[] result = getContract(orgId).submitTransaction("createBook", bookId, bookName, publisher, currentLocation, operator, operatorRole);
        String resultStr = new String(result, StandardCharsets.UTF_8);
        logger.info("[{}] 图书上链交易成功: bookId={}", orgId, bookId);
        return resultStr;
    }

    public String queryBook(String orgId, String bookId) throws Exception {
        logger.debug("[{}] 查询图书信息: bookId={}", orgId, bookId);
        byte[] result = getContract(orgId).evaluateTransaction("queryBook", bookId);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String updateBookLocation(String orgId, String bookId, String newLocation, String newStatus,
                                     String operator, String operatorRole) throws Exception {
        logger.debug("[{}] 发起【图书流转】交易: bookId={}, newLocation={}, operator={}", orgId, bookId, newLocation, operator);
        byte[] result = getContract(orgId).submitTransaction("updateBookLocation", bookId, newLocation, newStatus, operator, operatorRole);
        String resultStr = new String(result, StandardCharsets.UTF_8);
        logger.info("[{}] 图书流转交易成功: bookId={}", orgId, bookId);
        return resultStr;
    }

    public String getBookHistory(String orgId, String bookId) throws Exception {
        logger.debug("[{}] 查询图书历史溯源数据: bookId={}", orgId, bookId);
        byte[] result = getContract(orgId).evaluateTransaction("getBookHistory", bookId);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String deleteBook(String orgId, String bookId) throws Exception {
        logger.debug("[{}] 发起【图书删除】交易: bookId={}", orgId, bookId);
        getContract(orgId).submitTransaction("deleteBook", bookId);
        String result = "图书 [" + bookId + "] 已成功从当前账本状态中删除！";
        logger.info("[{}] 图书删除交易成功: bookId={}", orgId, bookId);
        return result;
    }

    private void startEventListener(Network network) {
        logger.info("正在启动区块链全局事件监听器...");
        new Thread(() -> {
            try {
                CloseableIterator<ChaincodeEvent> eventIter = network.getChaincodeEvents(CHAINCODE_NAME);
                logger.info("📡 监听器已就绪，正在等待区块链网络广播...");
                while (eventIter.hasNext()) {
                    ChaincodeEvent event = eventIter.next();
                    String payload = new String(event.getPayload(), StandardCharsets.UTF_8);
                    logger.info("🔔 [区块链实时广播] 事件类型: {}, 交易 ID: {}, 数据: {}",
                            event.getEventName(), event.getTransactionId(), payload);
                }
            } catch (Exception e) {
                logger.error("事件监听器异常: {}", e.getMessage(), e);
            }
        }).start();
    }
}