package com.arsc.bookchaingateway.trace.service;

import com.arsc.bookchaingateway.trace.config.FabricProperties;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import jakarta.annotation.PostConstruct;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

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

    private final FabricProperties fabricProperties;
    private final Map<String, Contract> contractMap = new HashMap<>();

    public FabricGatewayService(FabricProperties fabricProperties) {
        this.fabricProperties = fabricProperties;
    }

    @PostConstruct
    public void init() throws Exception {
        logger.info("==================================================");
        logger.info("正在初始化【省级联盟链多租户网关】...");
        logger.info("通道名称: {}", fabricProperties.getChannelName());
        logger.info("智能合约: {}", fabricProperties.getChaincodeName());

        for (Map.Entry<String, FabricProperties.OrgConfig> entry : fabricProperties.getOrganizations().entrySet()) {
            String orgKey = entry.getKey();
            FabricProperties.OrgConfig orgConfig = entry.getValue();
            Contract contract = initOrgContract(orgKey, orgConfig);
            contractMap.put(orgKey.toUpperCase(), contract);
        }

        logger.info("多组织身份路由配置完毕！共加载 {} 个组织", contractMap.size());
        logger.info("==================================================");
    }

    private Contract initOrgContract(String orgKey, FabricProperties.OrgConfig orgConfig) throws Exception {
        logger.info(">> 正在加载机构身份: {} (Endpoint: {})", orgConfig.getMspId(), orgConfig.getPeerEndpoint());

        InputStream tlsCertStream = new ClassPathResource(orgConfig.getTlsCert()).getInputStream();
        InputStream userCertStream = new ClassPathResource(orgConfig.getUserCert()).getInputStream();
        InputStream userKeyStream = new ClassPathResource(orgConfig.getUserKey()).getInputStream();

        Reader certReader = new InputStreamReader(userCertStream, StandardCharsets.UTF_8);
        Reader keyReader = new InputStreamReader(userKeyStream, StandardCharsets.UTF_8);

        ManagedChannel channel = Grpc.newChannelBuilder(orgConfig.getPeerEndpoint(),
                        TlsChannelCredentials.newBuilder().trustManager(tlsCertStream).build())
                .overrideAuthority(orgConfig.getOverrideAuth())
                .build();

        X509Certificate certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity(orgConfig.getMspId(), certificate);

        PrivateKey privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        int timeout = fabricProperties.getTimeoutSeconds();
        Gateway gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(timeout, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(timeout, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(timeout, TimeUnit.SECONDS))
                .connect();

        Network network = gateway.getNetwork(fabricProperties.getChannelName());

        if ("org1".equalsIgnoreCase(orgKey)) {
            startEventListener(network);
        }

        return network.getContract(fabricProperties.getChaincodeName());
    }

    private Contract getContract(String orgId) {
        if (orgId == null || orgId.trim().isEmpty()) {
            orgId = "ORG1";
        }
        Contract contract = contractMap.get(orgId.toUpperCase());
        if (contract == null) {
            throw new RuntimeException("非法的机构路由ID: " + orgId);
        }
        return contract;
    }

    public String createBook(String orgId, String bookId, String bookName, String publisher,
                             String currentLocation, String operator, String operatorRole) throws Exception {
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
                CloseableIterator<ChaincodeEvent> eventIter = network.getChaincodeEvents(fabricProperties.getChaincodeName());
                logger.info("监听器已就绪，正在等待区块链网络广播...");
                while (eventIter.hasNext()) {
                    ChaincodeEvent event = eventIter.next();
                    String payload = new String(event.getPayload(), StandardCharsets.UTF_8);
                    logger.info("[区块链实时广播] 事件类型: {}, 交易 ID: {}, 数据: {}",
                            event.getEventName(), event.getTransactionId(), payload);
                }
            } catch (Exception e) {
                logger.error("事件监听器异常: {}", e.getMessage(), e);
            }
        }).start();
    }
}
