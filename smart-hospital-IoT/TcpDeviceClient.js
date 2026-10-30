const net = require("node:net");

// TCP 客户端实现：设备协议为每条 JSON 报文以换行结尾。
class TcpDeviceClient {
  constructor({ host, port, timeoutMs = 5000 }) {
    this.host = host;
    this.port = port;
    this.timeoutMs = timeoutMs;
  }

  request(deviceId) {
    return this._exchange({ action: "read", deviceId });
  }

  send(deviceId, cmd) {
    return this._exchange({ action: "control", deviceId, ...cmd });
  }

  _exchange(message) {
    return new Promise((resolve, reject) => {
      const socket = net.createConnection({ host: this.host, port: this.port });
      let buffer = "";
      let settled = false;

      const finish = (error, value) => {
        if (settled) return;
        settled = true;
        socket.destroy();
        error ? reject(error) : resolve(value);
      };

      socket.setTimeout(this.timeoutMs, () => {
        finish(new Error(`TCP device timeout: ${this.host}:${this.port}`));
      });

      socket.once("connect", () => {
        socket.write(`${JSON.stringify(message)}\n`, (error) => {
          if (error) finish(error);
        });
      });

      socket.on("data", (chunk) => {
        buffer += chunk.toString("utf8");
        const newlineIndex = buffer.indexOf("\n");
        if (newlineIndex === -1) return;

        const line = buffer.slice(0, newlineIndex).trim();
        if (!line) {
          finish(new Error("TCP device returned an empty response"));
          return;
        }

        try {
          const response = JSON.parse(line);
          if (response.error) {
            finish(new Error(`TCP device error: ${response.error}`));
            return;
          }
          finish(null, response.data || response);
        } catch (error) {
          finish(new Error(`Invalid TCP device response: ${error.message}`));
        }
      });

      socket.once("error", (error) => {
        finish(
          new Error(
            `TCP device connection failed (${this.host}:${this.port}): ${error.message}`
          )
        );
      });

      socket.once("close", () => {
        if (!settled) finish(new Error("TCP device closed without a response"));
      });
    });
  }
}

module.exports = TcpDeviceClient;
