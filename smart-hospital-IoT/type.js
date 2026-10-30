const { injectable } = require("inversify")

// 类型或者接口抽象
const TYPES = {
    IDataNormalizer:Symbol.for("IDataNormalizer"),
    IDeviceAdapter:Symbol.for("IDeviceAdapter"),
    IMqttClient:Symbol.for("IMqttClient"),
    ITcpClient:Symbol.for("ITcpClient")
}

// 标准归一之后的院内模型
/***
 * @typedef {Object} StandardVitalData
 * @property {string} deviceId
 * @property {number} heartRate 心率
 * @property {number} spo2 血氧
 * @property {number} temperature
 * @property {number} ts
 * @property {string} vendor
 */

module.exports = {TYPES}