const { Container } = require("inversify");
const mqtt = require("mqtt");
const { TYPES } = require("./type");
const DataNormalizer = require("./DataNormalizer");
const MqttDeviceAdapter = require("./MqttDeviceAdapter");
const TcpDeviceAdapter = require("./TcpDeviceAdapter");
const TcpDeviceClient = require("./TcpDeviceClient");
const HttpPollAdapter = require("./HttpPollAdapter");
const JavaHisAdapter = require("./JavaHisAdapter");
const {
  mqttUrl,
  tcpDeviceHost,
  tcpDevicePort,
  tcpDeviceTimeoutMs,
} = require("./config");

const container = new Container();
// 绑定一次后续每次都返回同一个实例
container.bind(TYPES.IDataNormalizer).to(DataNormalizer).inSingletonScope();

// 第三方库创建客户端，再注册客户端，后面需要注册适配器
const mqttClient = mqtt.connect(mqttUrl);
container.bind(TYPES.IMqttClient).toConstantValue(mqttClient);

// 创建真实 TCP 客户端：每条报文使用“JSON + 换行”协议。
const tcpClient = new TcpDeviceClient({
  host: tcpDeviceHost,
  port: tcpDevicePort,
  timeoutMs: tcpDeviceTimeoutMs,
});

// 将 TCP 客户端注册到容器，后续复用这个实例。
container.bind(TYPES.ITcpClient).toConstantValue(tcpClient);

// 比如这里normalizer,前面已经创建实例这里调用即可
const normalizer = () => container.get(TYPES.IDataNormalizer);

// 注册四个适配器。IDeviceAdapter这里绑定，使用了toDynamicValue
container.bind(TYPES.IDeviceAdapter).toDynamicValue(() => new MqttDeviceAdapter(mqttClient, normalizer())).whenNamed("mqtt");
container.bind(TYPES.IDeviceAdapter).toDynamicValue(() => new TcpDeviceAdapter(tcpClient, normalizer())).whenNamed("tcp");
/**
 * 
 /smart-hospital-IoT/server.js不关心底层是http  tcp  mqtt,只需要传入type 动态选择，否则：
if (type === "http") {
  const res = await axios.get(...);
} else if (type === "mqtt") {
  ...
} else if (type === "tcp") {
  ...
}
 */
container.bind(TYPES.IDeviceAdapter).toDynamicValue(() => new HttpPollAdapter(normalizer())).whenNamed("http");
container.bind(TYPES.IDeviceAdapter).toDynamicValue(() => new JavaHisAdapter(normalizer())).whenNamed("java-his");

module.exports = container;
