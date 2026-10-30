const express = require("express");
const app = express();
app.use(express.json());
app.get("/device/:id", (req, res) => res.json({ dev_id: req.params.id, pulse: 78, oxy: 97, temp: 36.6, time: Date.now() }));
app.post("/device/:id/cmd", (req, res) => res.json({ ok: true, deviceId: req.params.id, command: req.body }));
app.listen(9000, "0.0.0.0", () => console.log("vendor mock listening on 9000"));
