# Book Chain Gateway 多组织网关问题排查报告

**日期：** 2026-04-02

---

## 1. 问题描述

在图书区块链溯源系统中，出现以下问题：

1. 通过 ORG1 上链的图书，修改数据后，只有 ORG1 能查询到溯源历史，ORG2 和 ORG3 查不到。
2. ORG2 或 ORG3 调用更新图书状态接口时返回错误：`ABORTED: failed to endorse transaction`。
3. 修复背书策略后，出现新错误：`FAILED_PRECONDITION: no combination of peers can be derived which satisfy the endorsement policy`。

---

## 2. 区块链网络环境信息

### 2.1 服务器与安装信息

| 项目 | 详情 |
|------|------|
| 操作系统 | Linux (amd64) |
| Fabric 版本 | v2.5.11 |
| Go 版本 | go1.23.5 |
| 安装目录 | `/home/zcj/fabric/fabric-samples/test-network` |
| 二进制工具目录 | `/home/zcj/fabric/fabric-samples/bin` |
| 网络模板 | fabric-samples/test-network（ORG3 为手动添加） |
| 部署方式 | Docker Compose |

### 2.2 节点信息

| 节点类型 | 节点名称 | 容器端口 | 映射端口 | MSP ID |
|---------|---------|---------|---------|--------|
| Orderer | orderer.example.com | 7050 | 0.0.0.0:7050 | OrdererMSP |
| Peer | peer0.org1.example.com | 7051 | 0.0.0.0:7051 | Org1MSP |
| Peer | peer0.org2.example.com | 9051 | 0.0.0.0:9051 | Org2MSP |
| Peer | peer0.org3.example.com | 11051 | 0.0.0.0:11051 | Org3MSP |

**说明：** ORG1 和 ORG2 由 test-network 脚本默认创建，ORG3 通过 `addOrg3` 目录下的脚本手动添加。

### 2.3 通道与链码

| 项目 | 详情 |
|------|------|
| 通道名称 | mychannel |
| 链码名称 | booktrace |
| 链码版本 | 8.0 |
| 链码序列号 | 10（最终修复后的序列号） |
| 链码类型 | Java 链码（fabric-contract-java） |
| 链码包 ID | `booktrace_8.0:ef138b320e7af71f7406e670b5aaece88519249823cb387acb7b4fad726b319c` |
| 背书策略（修复后） | `OR('Org1MSP.member','Org2MSP.member','Org3MSP.member')` |
| 链码源码目录 | `E:\文化产业课题项目\code\book-chaincode` |

### 2.4 证书目录结构

```
test-network/organizations/
├── ordererOrganizations/
│   └── example.com/
│       ├── msp/
│       └── tlsca/
│           └── tlsca.example.com-cert.pem          # Orderer TLS CA 证书
└── peerOrganizations/
    ├── org1.example.com/
    │   ├── msp/
    │   ├── tlsca/
    │   │   └── tlsca.org1.example.com-cert.pem     # ORG1 TLS CA 证书
    │   └── users/
    │       └── Admin@org1.example.com/
    │           └── msp/
    │               ├── signcerts/
    │               │   └── Admin@org1.example.com-cert.pem   # ORG1 Admin 证书
    │               └── keystore/
    │                   └── priv_sk                            # ORG1 Admin 私钥
    ├── org2.example.com/   （结构同上）
    └── org3.example.com/   （结构同上）
```

### 2.5 网关项目证书映射

| 组织 | 网关项目路径 | 对应 Fabric 网络路径 |
|------|-------------|-------------------|
| ORG1 TLS CA | `network/org1/tls-ca.crt` | `organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem` |
| ORG1 用户证书 | `network/org1/user-cert.pem` | `organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem` |
| ORG1 用户私钥 | `network/org1/user-key.pem` | `organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/priv_sk` |
| ORG2 TLS CA | `network/org2/tls-ca.crt` | `organizations/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem` |
| ORG2 用户证书 | `network/org2/user-cert.pem` | `organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp/signcerts/Admin@org2.example.com-cert.pem` |
| ORG2 用户私钥 | `network/org2/user-key.pem` | `organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp/keystore/priv_sk` |
| ORG3 TLS CA | `network/org3/tls-ca.crt` | `organizations/peerOrganizations/org3.example.com/tlsca/tlsca.org3.example.com-cert.pem` |
| ORG3 用户证书 | `network/org3/user-cert.pem` | `organizations/peerOrganizations/org3.example.com/users/Admin@org3.example.com/msp/signcerts/Admin@org3.example.com-cert.pem` |
| ORG3 用户私钥 | `network/org3/user-key.pem` | `organizations/peerOrganizations/org3.example.com/users/Admin@org3.example.com/msp/keystore/priv_sk` |

### 2.6 Docker 镜像版本

| 镜像 | 版本 | 用途 |
|------|------|------|
| hyperledger/fabric-peer | 2.5.11 | Peer 节点 |
| hyperledger/fabric-orderer | 2.5.11 | 排序节点 |
| hyperledger/fabric-tools | 2.5.11 | CLI 工具 |
| hyperledger/fabric-ccenv | 2.5.11 | 链码构建环境 |
| hyperledger/fabric-javaenv | 2.5 | Java 链码构建环境 |

### 2.7 应用配置（application.yml）

| 配置项 | 值 |
|-------|---|
| 服务端口 | 8080 |
| 通道名称 | mychannel |
| 链码名称 | booktrace |
| 超时时间 | 30 秒 |
| ORG1 MSP ID | Org1MSP |
| ORG1 Peer 地址 | localhost:7051 |
| ORG1 Override Auth | peer0.org1.example.com |
| ORG2 MSP ID | Org2MSP |
| ORG2 Peer 地址 | localhost:9051 |
| ORG2 Override Auth | peer0.org2.example.com |
| ORG3 MSP ID | Org3MSP |
| ORG3 Peer 地址 | localhost:11051 |
| ORG3 Override Auth | peer0.org3.example.com |

### 2.8 常用运维命令速查

```bash
# 设置环境变量（每次打开新终端需重新设置）
cd ~/fabric/fabric-samples/test-network
export PATH=${PWD}/../bin:$PATH
export CORE_PEER_TLS_ENABLED=true

# 切换组织环境变量（以下以 ORG1 为例）
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS=localhost:7051
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp

# 查看区块高度
peer channel getinfo -c mychannel

# 查看已安装的链码
peer lifecycle chaincode queryinstalled

# 查看已提交的链码定义
peer lifecycle chaincode querycommitted -c mychannel --name booktrace

# 查看链码容器状态
docker ps | grep booktrace

# 查看 Peer 日志
docker logs peer0.org2.example.com --tail 50

# 解码证书信息
openssl x509 -in user-cert.pem -noout -subject

# 拉取通道最新配置
peer channel fetch config config_block.pb -c mychannel -o localhost:7050 --tls --cafile ${PWD}/organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem
configtxlator proto_decode --input config_block.pb --type common.Block > config.json
```

---

## 3. 根因分析

### 3.1 问题一：ORG2/ORG3 更新失败 — 背书策略不满足

**错误信息：**

```
io.grpc.StatusRuntimeException: ABORTED: failed to endorse transaction
```

**排查过程：**

1. 查询已提交的链码定义，确认三个组织均已批准（sequence: 8, version: 8.0）
2. 解码 `validation_parameter`，发现使用的是通道默认策略 `/Channel/Application/Endorsement`
3. 拉取通道配置并解码，发现默认背书策略为 **MAJORITY（多数原则）**
4. 3 个组织中至少需要 **2 个组织同时签名背书**才能通过

**根本原因：**

通道默认的 Endorsement 策略是 MAJORITY，3 个组织需要至少 2 个签名。但网关代码每次只连接一个组织的 Peer 去提交交易（例如 ORG2 只拿到 ORG2 的背书），不满足 MAJORITY 要求，交易被拒绝。

### 3.2 问题二：链码容器未启动 — 缺少包引用

**错误信息：**

```
FAILED_PRECONDITION: no combination of peers can be derived which satisfy the endorsement policy: required chaincodes are not installed on sufficient peers
```

**排查过程：**

1. `docker ps | grep booktrace` 发现链码容器全部消失
2. 查询已提交的链码定义，发现 `querycommitted` 的 JSON 输出中 **没有 package_id 字段**
3. 链码已安装在所有 Peer 上（`queryinstalled` 可查到）

**根本原因：**

重新提交链码定义（sequence 9）时，`approve` 命令没有携带 `--package-id` 参数。导致 Peer 不知道应该用哪个已安装的链码包来启动容器。后续重启 Peer 容器后，链码容器也不会自动重建。

### 3.3 问题三：Writers 策略校验失败 — 证书身份错误

**Peer 日志关键信息：**

```
subject=CN=peer0.org2.example.com,OU=peer,L=San Francisco,ST=California,C=US
Failed evaluating policy on signed data during check policy on channel [mychannel] with policy [/Channel/Application/Writers]
```

**排查过程：**

1. 用 `openssl x509 -in user-cert.pem -noout -subject` 解码网关项目中的证书
2. 发现证书主题为 `CN=peer0.org2.example.com,OU=peer`（Peer 节点证书）
3. 正确的管理员证书主题应为 `CN=Admin@org2.example.com,OU=admin`

**根本原因：**

网关项目 `network/org{N}/user-cert.pem` 放置的是 **Peer 节点的 TLS 证书**，而非 **Admin 用户的身份证书**。Writers 策略要求身份的 OU 为 admin 或 client，但 Peer 证书的 OU 为 peer，不满足条件。三个组织均存在此问题。

| 字段 | 错误（Peer 证书） | 正确（Admin 证书） |
|------|-------------------|-------------------|
| CN | peer0.org2.example.com | Admin@org2.example.com |
| OU | peer | admin |
| 来源 | Peer 节点 TLS 证书 | Admin 用户身份证书 |

---

## 4. 解决方案

### 4.1 修改背书策略为 OR（任一组织背书即可）

**第一步：三个组织分别 approve 新的链码定义**

```bash
cd ~/fabric/fabric-samples/test-network
export PATH=${PWD}/../bin:$PATH
export CORE_PEER_TLS_ENABLED=true
export PKG_ID="booktrace_8.0:ef138b320e7af71f7406e670b5aaece88519249823cb387acb7b4fad726b319c"
export ORDERER_CA=${PWD}/organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem
export POLICY="OR('Org1MSP.member','Org2MSP.member','Org3MSP.member')"

# ORG1
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS=localhost:7051
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
peer lifecycle chaincode approveformyorg -o localhost:7050 --tls --cafile $ORDERER_CA \
  --channelID mychannel --name booktrace --version 8.0 --sequence 10 \
  --package-id $PKG_ID --signature-policy "$POLICY"

# ORG2
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_ADDRESS=localhost:9051
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp
peer lifecycle chaincode approveformyorg -o localhost:7050 --tls --cafile $ORDERER_CA \
  --channelID mychannel --name booktrace --version 8.0 --sequence 10 \
  --package-id $PKG_ID --signature-policy "$POLICY"

# ORG3
export CORE_PEER_LOCALMSPID="Org3MSP"
export CORE_PEER_ADDRESS=localhost:11051
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org3.example.com/tlsca/org3.example.com-cert.pem
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org3.example.com/users/Admin@org3.example.com/msp
peer lifecycle chaincode approveformyorg -o localhost:7050 --tls --cafile $ORDERER_CA \
  --channelID mychannel --name booktrace --version 8.0 --sequence 10 \
  --package-id $PKG_ID --signature-policy "$POLICY"
```

**第二步：commit（必须包含所有三个 Peer 地址）**

```bash
export O1_CA=${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
export O2_CA=${PWD}/organizations/peerOrganizations/org2.example.com/tlsca/org2.example.com-cert.pem
export O3_CA=${PWD}/organizations/peerOrganizations/org3.example.com/tlsca/org3.example.com-cert.pem

export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS=localhost:7051
export CORE_PEER_TLS_ROOTCERT_FILE=$O1_CA
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp

peer lifecycle chaincode commit \
  -o localhost:7050 --tls --cafile $ORDERER_CA \
  --channelID mychannel --name booktrace \
  --version 8.0 --sequence 10 \
  --signature-policy "$POLICY" \
  --peerAddresses localhost:7051 --tlsRootCertFiles $O1_CA \
  --peerAddresses localhost:9051 --tlsRootCertFiles $O2_CA \
  --peerAddresses localhost:11051 --tlsRootCertFiles $O3_CA
```

**注意事项：**

- sequence 必须递增（8 → 9 → 10）
- `--package-id` 必须携带，否则 Peer 无法关联链码包
- `--signature-policy` 在 approve 和 commit 中都要携带
- commit 时必须指定所有三个 Peer 的地址和 TLS 证书
- ORG3 是手动添加的第三个组织，命令中需注意证书路径与其他组织不同

### 4.2 替换用户证书和私钥

将网关项目中的 Peer 节点证书替换为 Admin 用户证书。

**Fabric 网络中正确的 Admin 证书路径：**

| 文件 | Fabric 网络路径 |
|------|----------------|
| ORG1 用户证书 | `organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem` |
| ORG2 用户证书 | `organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp/signcerts/Admin@org2.example.com-cert.pem` |
| ORG3 用户证书 | `organizations/peerOrganizations/org3.example.com/users/Admin@org3.example.com/msp/signcerts/Admin@org3.example.com-cert.pem` |
| ORG1 用户私钥 | `organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/priv_sk` |
| ORG2 用户私钥 | `organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp/keystore/priv_sk` |
| ORG3 用户私钥 | `organizations/peerOrganizations/org3.example.com/users/Admin@org3.example.com/msp/keystore/priv_sk` |

**网关项目中的目标路径：**

- `src/main/resources/network/org{N}/user-cert.pem`
- `src/main/resources/network/org{N}/user-key.pem`

**验证证书是否正确：**

```bash
openssl x509 -in user-cert.pem -noout -subject
# 预期输出：subject=CN = Admin@org2.example.com, OU = admin, ...
```

**重要：** 替换文件后必须重新编译项目（`mvn compile` 或 IDEA 中 Build → Rebuild Project），确保 `target` 目录中的文件同步更新，然后重启应用。

---

## 5. 验证步骤

修复完成后，依次验证：

1. 查询背书策略：`peer lifecycle chaincode querycommitted -C mychannel --name booktrace` 确认已改为 OR 策略
2. 检查链码容器：`docker ps | grep booktrace` 确认三个组织容器均运行
3. ORG1 查询图书：`GET /api/books/{id}?orgId=ORG1`
4. ORG2 更新图书：`PUT /api/books/{id}` 请求体中 `orgId=ORG2`
5. ORG3 更新图书：`PUT /api/books/{id}` 请求体中 `orgId=ORG3`
6. ORG2 查询历史：`GET /api/books/{id}/history?orgId=ORG2`
7. ORG3 查询历史：`GET /api/books/{id}/history?orgId=ORG3`

---

## 6. 总结

| 问题 | 根本原因 | 解决方案 |
|------|---------|---------|
| 多组织交易失败 | 通道默认 MAJORITY 背书策略要求 2+ 组织签名 | 改为 OR 策略，任一组织即可背书 |
| 链码容器未启动 | approve 时缺少 `--package-id` 参数 | approve 和 commit 时携带 `--package-id` |
| Writers 策略校验失败 | 使用了 Peer 证书（OU=peer）而非 Admin 证书（OU=admin） | 替换为正确的 Admin 用户证书和私钥 |
