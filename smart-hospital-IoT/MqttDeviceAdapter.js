class MqttDeviceAdapter {
  constructor(mqttClient, normalizer) { 
    this.mqttClient = mqttClient; 
    this.normalizer = normalizer; 
  }

  async read(deviceType,deviceId, vendor) { 
    const topic = deviceType === "wearable" 
    ? `wearable/${deviceId}/raw`
    :`device/${deviceId}/raw`
    
    return this.normalizer.convert(await this._readMqttTopic(topic), vendor); 
  }

  async control(deviceType, deviceId, cmd) {
    const topic = deviceType === "wearable" ? `wearable/${deviceId}/cmd` : `device/${deviceId}/cmd`
    await new Promise((resolve, reject) => this.mqttClient.publish(topic, 
      JSON.stringify({ deviceId, ...cmd }),
       (error) => error ? reject(error) : resolve()));
  }

  _readMqttTopic(topic) {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => { 
        this.mqttClient.removeListener("message", onMessage); 
        reject(new Error(`MQTT read timeout: ${topic}`)); 
      }, 5000);

      const onMessage = (receivedTopic, payload) => {
        if (receivedTopic !== topic) return;

        clearTimeout(timeout); 
        this.mqttClient.unsubscribe(topic); 
        this.mqttClient.removeListener("message", onMessage);

        try { 
          resolve(JSON.parse(payload.toString()));
         } catch (error) { 
          reject(error); 
        }
      };

      this.mqttClient.on("message", onMessage);

      this.mqttClient.subscribe(topic, (error) => {
         if (error) { 
          clearTimeout(timeout); 
          this.mqttClient.removeListener("message", onMessage); 
          reject(error); 
        } 
      });
    });
  }
}

module.exports = MqttDeviceAdapter;

// 调用时候使用，await adapter.read("device", "D001", "vendorA"); 或者 await adapter.read("wearable", "W001", "vendorA");