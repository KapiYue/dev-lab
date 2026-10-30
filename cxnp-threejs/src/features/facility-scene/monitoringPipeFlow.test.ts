import { describe, expect, it } from "vitest";
import {
  MAX_SHADER_FLOW_SPEED,
  mapMonitoringToPipeFlow,
} from "./monitoringPipeFlow";

describe("monitoring pipe flow mapping", () => {
  it("maps instant flow to shader speed", () => {
    expect(mapMonitoringToPipeFlow({
      instantFlow: 3.26,
      pumpRunning: true,
      facilityStatus: "normal",
      alarmLevel: "normal",
    })).toEqual({ status: "normal", speed: 0.652, flowing: true });
  });

  it("caps unusually high flow at the shader speed limit", () => {
    expect(mapMonitoringToPipeFlow({
      instantFlow: 100,
      pumpRunning: true,
      facilityStatus: "normal",
      alarmLevel: "normal",
    }).speed).toBe(MAX_SHADER_FLOW_SPEED);
  });

  it("stops the flow when the pump is stopped or flow is zero", () => {
    expect(mapMonitoringToPipeFlow({
      instantFlow: 4.2,
      pumpRunning: false,
      facilityStatus: "warning",
      alarmLevel: "warning",
    })).toEqual({ status: "warning", speed: 0, flowing: false });
    expect(mapMonitoringToPipeFlow({
      instantFlow: 0,
      pumpRunning: true,
      facilityStatus: "normal",
      alarmLevel: "normal",
    }).speed).toBe(0);
  });

  it("maps warning and critical alarms to orange and red states", () => {
    expect(mapMonitoringToPipeFlow({
      instantFlow: 1.17,
      pumpRunning: true,
      facilityStatus: "warning",
      alarmLevel: "warning",
    }).status).toBe("warning");
    expect(mapMonitoringToPipeFlow({
      instantFlow: 1.17,
      pumpRunning: true,
      facilityStatus: "normal",
      alarmLevel: "critical",
    }).status).toBe("critical");
  });

  it("forces offline monitoring data to a stopped offline visual", () => {
    expect(mapMonitoringToPipeFlow({
      instantFlow: 3.6,
      pumpRunning: true,
      facilityStatus: "offline",
      alarmLevel: "offline",
    })).toEqual({ status: "offline", speed: 0, flowing: false });
  });

  it("treats invalid and negative flow as stopped", () => {
    for (const instantFlow of [Number.NaN, Number.POSITIVE_INFINITY, -1]) {
      expect(mapMonitoringToPipeFlow({
        instantFlow,
        pumpRunning: true,
        facilityStatus: "normal",
        alarmLevel: "normal",
      }).flowing).toBe(false);
    }
  });
});
