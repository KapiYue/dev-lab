# Smart Hospital IoT

院内物联网数据接入示例项目。项目通过适配器接入 MQTT、TCP、厂商 HTTP 以及 Java HIS 服务，并将不同厂商的生命体征字段转换为统一的数据结构。

## 1. 运行环境

- Node.js 22+（当前代码也可在 Node.js 24 下运行）
- npm
- Docker Desktop / Docker Compose（运行本地模拟服务时需要）

## 2. 项目结构

```text
.
├── server.js                 # Node 主服务，监听 3000 端口
├── config.js                 # 外部服务地址配置
├── container.js              # IOC 容器及适配器注册
├── DataNormalizer.js         # 厂商数据转院内统一模型
├── MqttDeviceAdapter.js      # MQTT 设备适配器
├── TcpDeviceAdapter.js       # TCP 设备适配器（当前为模拟 TCP 客户端）
├── HttpPollAdapter.js        # 厂商 HTTP 轮询适配器
├── JavaHisAdapter.js         # Java HIS 适配器
├── docker-compose.yml        # 本地依赖模拟服务
├── mock-his/                 # Java HIS 模拟服务，8080 端口
├── mock-vendor/              # 厂商 HTTP 模拟服务，9000 端口
└── mock-mqtt/                # MQTT 模拟发布器及 Mosquitto 配置
```

## 3. 安装依赖

在项目根目录执行：

```bash
npm install
```

## 4. 启动方式

### 4.1 启动本地模拟依赖

```bash
docker compose up -d --build
```

启动后会提供：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| Java HIS Mock | `http://localhost:8080` | 模拟患者、登录、医嘱和预约接口 |
| Vendor API Mock | `http://localhost:9000` | 模拟厂商设备查询和控制接口 |
| MQTT Broker | `mqtt://localhost:1883` | Mosquitto，无认证 |
| MQTT Mock Publisher | Docker 内部服务 | 每 5 秒发布 `device/D001/raw` |

查看模拟服务日志：

```bash
docker compose logs -f
```

### 4.2 启动 Node 主服务

另开一个终端，在项目根目录执行：

```bash
node server.js
```

看到以下日志表示主服务已启动：

```text
Node IOC data layer listening on 3000
```

停止本地模拟依赖：

```bash
docker compose down
```

## 5. 配置

配置通过环境变量传入，未设置时使用以下默认值：

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `JAVA_HIS_BASE_URL` | `http://localhost:8080/api` | Java HIS 服务基础地址 |
| `VENDOR_API_BASE_URL` | `http://localhost:9000` | 厂商 HTTP 服务基础地址 |
| `MQTT_URL` | `mqtt://127.0.0.1:1883` | MQTT Broker 地址 |

macOS / Linux 示例：

```bash
JAVA_HIS_BASE_URL=http://localhost:8080/api \
VENDOR_API_BASE_URL=http://localhost:9000 \
MQTT_URL=mqtt://127.0.0.1:1883 \
node server.js
```

如果以后把 Node 主服务也放进 Docker 容器，不能继续使用 `localhost` 访问其他容器，应改用 Compose 服务名，例如：

```bash
JAVA_HIS_BASE_URL=http://java-his-server:8080/api
VENDOR_API_BASE_URL=http://vendor-api:9000
MQTT_URL=mqtt://mqtt:1883
```

## 6. API 快速验证

### 6.1 读取厂商 HTTP 设备

```bash
curl 'http://localhost:3000/api/device/read?type=http&deviceId=D001&vendor=vendorA'
```

预期返回类似：

```json
{
  "code": 0,
  "data": {
    "deviceId": "D001",
    "heartRate": 78,
    "spo2": 97,
    "temperature": 36.6,
    "vendor": "vendorA"
  }
}
```

### 6.2 读取 MQTT 设备

先确保 `docker compose up -d --build` 已启动，然后执行：

```bash
curl 'http://localhost:3000/api/device/read?type=mqtt&deviceId=D001&vendor=vendorA'
```

设备 MQTT 原始主题格式为：

```text
device/{deviceId}/raw
```

控制主题格式为：

```text
device/{deviceId}/cmd
```

### 6.3 控制设备

```bash
curl -X POST 'http://localhost:3000/api/device/control' \
  -H 'Content-Type: application/json' \
  -d '{"type":"http","deviceId":"D001","cmd":{"action":"start"}}'
```

`type` 可使用：

- `mqtt`
- `tcp`
- `http`
- `java-his`

### 6.4 查询 HIS 预约

```bash
curl 'http://localhost:3000/api/his/appointments/A001'
```

## 7. 统一数据模型

不同厂商的数据会转换为以下格式：

```json
{
  "deviceId": "D001",
  "heartRate": 78,
  "spo2": 97,
  "temperature": 36.6,
  "ts": 1760000000000,
  "vendor": "vendorA"
}
```

当前字段映射：

| vendor | 心率字段 | 血氧字段 |
| --- | --- | --- |
| `vendorA` | `pulse` | `oxy` |
| `vendorB` | `heart` | `spo` |
| `vendorC` | `hr_val` | `spo2_val` |

如果接入新的厂商，请在 `DataNormalizer.js` 的 `mapTable` 中增加字段映射。

## 8. 当前实现注意事项

1. 根目录 `package.json` 当前没有 `start` 脚本，因此主服务使用 `node server.js` 启动；如需使用 `npm start`，可以在 `package.json` 增加：

   ```json
   "scripts": {
     "start": "node server.js"
   }
   ```

2. `TcpDeviceAdapter` 当前使用 `container.js` 中的模拟 TCP 客户端，读取结果固定为 `D002` 的示例数据；接入真实 TCP 设备时需要替换 `ITcpClient` 实现。

3. `mock-his` 当前提供预约、患者、登录和医嘱接口，但没有实现 `/api/vital/get` 与 `/api/vital/control`。因此 Java HIS 的预约查询可以直接验证，生命体征读取和控制需要补充 Mock 接口或连接真实 HIS。

4. MQTT Mock Broker 当前允许匿名访问，仅适合本地开发测试，不能直接用于生产环境。

5. 主服务暂未提供健康检查接口、鉴权、日志框架和统一错误码规范；生产部署前需要按实际环境补充。

## 9. 开发建议

建议后续补充：

- 在 `package.json` 中增加 `start`、`dev` 和 `test` 脚本；
- 为 `server.js` 增加 `/health` 健康检查接口；
- 为 MQTT、HTTP、TCP 和 HIS 适配器增加单元测试；
- 将 MQTT 用户名、密码、超时和端口等配置全部改为环境变量；
- 为生产环境增加认证、访问日志、超时、重试和数据脱敏。
