const express = require("express");
const container = require("./container");
const { TYPES } = require("./type");
const app = express();
app.use(express.json());

// 最开始，前端请求/api/device/read?type=mqtt&deviceId=D001
app.get("/api/device/read", async (req, res) => {
  try {
    const { type, deviceType = "device", deviceId, vendor = "vendorA" } = req.query;
    // 先找对应的适配器，这里是mqtt
    const adapter = container.get(TYPES.IDeviceAdapter, { 
      name: type 
    });


    res.json({ 
      code: 0, 
      data: await adapter.read(deviceType, deviceId, vendor) 
    });

  } catch (error) { 
    res.status(500).json({ 
      code: -1, msg: error.message 
    }); 
  }
});

app.post("/api/device/control", async (req, res) => {
  try {
    const { type, deviceId, cmd } = req.body;

    await container.get(TYPES.IDeviceAdapter, { 
      name: type 
    }).control(deviceId, cmd);

    res.json({ code: 0, msg: "ok" });
  } catch (error) { 
    res.status(500).json({ 
      code: -1, 
      msg: error.message }); 
  }
});

app.get("/api/his/appointments/:id", async (req, res) => {
  try {
    const adapter = container.get(TYPES.IDeviceAdapter, { name: "java-his" });
    res.json({ 
      code: 0, 
      data: await adapter.getAppointment(req.params.id) 
    });
  } catch (error) { 
    res.status(502).json({ 
      code: -1, 
      msg: error.message 
    }); 
  }
});

app.listen(3000, () => console.log("Node IOC data layer listening on 3000"));
/**完整链路
 * 
前端
  ↓ GET /api/device/read?type=mqtt&deviceId=D001
server.js
  ↓ 根据 type="mqtt" 选择适配器
MqttDeviceAdapter
  ↓ 根据 deviceId 订阅 device/D001/raw
MQTT 返回原始数据
  ↓
DataNormalizer 统一字段格式
  ↓
adapter.read() 返回统一对象
  ↓
res.json({ code: 0, data: ... })
前端获取 response.data

 * 
 */