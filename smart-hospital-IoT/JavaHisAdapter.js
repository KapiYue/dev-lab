const axios = require("axios");
const { javaBaseUrl } = require("./config");

class JavaHisAdapter {
  constructor(normalizer) { 
    this.normalizer = normalizer; 
    this.javaBaseUrl = javaBaseUrl; 
  }

  async getAppointment(id) { 
    const resp = await axios.get(`${this.javaBaseUrl}/appointments/${encodeURIComponent(id)}`); 
    return resp.data;
   }
   
  async read(deviceId, vendor) { 
    const resp = await axios.get(`${this.javaBaseUrl}/vital/get`, { 
      params: { deviceId } 
    }); 
    return this.normalizer.convert(resp.data, vendor); 
  }

  async control(deviceId, cmd) { 
    await axios.post(`${this.javaBaseUrl}/vital/control`, {
      deviceId, ...cmd 
    }); 
  }
}
module.exports = JavaHisAdapter;
