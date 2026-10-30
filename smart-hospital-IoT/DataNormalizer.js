class DataNormalizer {
  convert(raw, vendor) {
    const mapTable = {
      vendorA: { hr: "pulse", o2: "oxy" },
      vendorB: { hr: "heart", o2: "spo" },
      vendorC: { hr: "hr_val", o2: "spo2_val" }
    };
    const mapping = mapTable[vendor] || mapTable.vendorA;
    return {
      deviceId: raw.dev_id,
      heartRate: Number(raw[mapping.hr]),
      spo2: Number(raw[mapping.o2]),
      temperature: Number(raw.temp),
      ts: raw.time || Date.now(),
      vendor
    };
  }
}
module.exports = DataNormalizer;
