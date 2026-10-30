const mqtt = require("mqtt");
const client = mqtt.connect(process.env.MQTT_URL || "mqtt://mqtt:1883");
client.on("connect", () => {
  const publish = () => {
    
    client.publish("device/D001/raw", 
      JSON.stringify({ 
        dev_id: "D001", 
        pulse: 78, 
        oxy: 97, 
        temp: 36.6, 
        time: Date.now() 
      }), { 
        retain: true 
      })
  };

  publish();
  setInterval(publish, 5000);
});
