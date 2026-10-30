import express from "express";
import cors from "cors";

const app = express();

app.use(cors());
app.use(express.json());

app.get("/api/health", (req, res) => {
  res.json({
    code: 200,
    message: "mock his is healthy"
  });
});

app.get("/api/patients/:id", (req, res) => {
  res.json({
    code: 200,
    message: "success",
    data: {
      id: req.params.id,
      name: "张三",
      gender: "男",
      age: 45,
      department: "心内科"
    }
  });
});

app.get("/api/patients", (req, res) => {
  res.json({
    code: 200,
    message: "success",
    data: [
      {
        id: "P001",
        name: "张三",
        gender: "男",
        age: 45
      },
      {
        id: "P002",
        name: "李四",
        gender: "女",
        age: 32
      }
    ]
  });
});

app.post("/api/login", (req, res) => {
  res.json({
    code: 200,
    message: "登录成功",
    data: {
      token: "mock-token",
      userId: "U001",
      username: "mock-user"
    }
  });
});

app.post("/api/orders", (req, res) => {
    res.json({
      code: 200,
      message: "医嘱创建成功",
      data: {
        orderId: "ORDER001",
        status: "CREATED"
      }
    });
  });
  
  app.get("/api/appointments/:id", (req, res) => {
    res.json({
      code: 200,
      message: "success",
      data: {
        id: req.params.id,
        patientId: "P001",
        doctorName: "王医生",
        appointmentTime: "2026-10-28 14:00:00"
      }
    });
  });
  

app.listen(8080, "0.0.0.0", () => {
  console.log("Mock HIS server listening on port 8080");
});