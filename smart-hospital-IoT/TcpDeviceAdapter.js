class TcpDeviceAdapter {
  constructor(tcpClient, normalizer) {
    this.tcpClient = tcpClient;
    this.normalizer = normalizer;
  }

  async read(deviceId, vendor) {
    if (!deviceId) {
      throw new Error("deviceId is required");
    }

    const raw = await this.tcpClient.request(deviceId);
    return this.normalizer.convert(raw, vendor);
  }

  async control(deviceId, cmd) {
    if (!deviceId) {
      throw new Error("deviceId is required");
    }

    await this.tcpClient.send(deviceId, cmd);
  }
}

module.exports = TcpDeviceAdapter;
