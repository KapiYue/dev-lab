const axios = require("axios");
const { vendorBaseUrl } = require("./config");

class HttpPollAdapter {
  constructor(normalizer) { 
    this.normalizer = normalizer; 
  }

  async read(deviceId, vendor) { 
    const res = await axios.get(`${vendorBaseUrl}/device/${encodeURIComponent(deviceId)}`); 
    return this.normalizer.convert(res.data, vendor); 
  }

  async control(deviceId, cmd) { 
    await axios.post(`${vendorBaseUrl}/device/${encodeURIComponent(deviceId)}/cmd`, cmd); 
  }
}

module.exports = HttpPollAdapter;
