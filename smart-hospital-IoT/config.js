const trimTrailingSlash = (value) => String(value).replace(/\/$/, "");

module.exports = {
  // Node 在宿主机运行时访问 Docker 暴露的 Java Mock。
  javaBaseUrl: trimTrailingSlash(
    process.env.JAVA_HIS_BASE_URL || "http://localhost:8080/api"
  ),
  vendorBaseUrl: trimTrailingSlash(
    process.env.VENDOR_API_BASE_URL || "http://localhost:9000"
  ),
  mqttUrl: process.env.MQTT_URL || "mqtt://127.0.0.1:1883",
  tcpDeviceHost: process.env.TCP_DEVICE_HOST || "127.0.0.1",
  tcpDevicePort: Number(process.env.TCP_DEVICE_PORT || 9001),
  tcpDeviceTimeoutMs: Number(process.env.TCP_DEVICE_TIMEOUT_MS || 5000),
};
