import { describe, expect, it } from "vitest";
import {
  PIPE_FLOW_COLORS,
  PIPE_FLOW_FRAGMENT_SHADER,
  createPipeFlowUniforms,
  getPipeFlowPhase,
} from "./pipeFlowShader";

describe("pipe flow shader", () => {
  it("creates the required uniforms with the selected status color", () => {
    const uniforms = createPipeFlowUniforms({ status: "warning", speed: 0.65, opacity: 0.8 });

    expect(uniforms.uTime.value).toBe(0);
    expect(uniforms.uSpeed.value).toBe(0.65);
    expect(`#${uniforms.uColor.value.getHexString()}`).toBe(PIPE_FLOW_COLORS.warning);
    expect(uniforms.uOpacity.value).toBe(0.8);
  });

  it("keeps the animation phase fixed when speed is zero", () => {
    expect(getPipeFlowPhase(1, 0)).toBe(0);
    expect(getPipeFlowPhase(25, 0)).toBe(0);
    expect(getPipeFlowPhase(1.5, 0.5)).toBeCloseTo(0.75);
  });

  it("maps monitoring states to distinct shader colors", () => {
    expect(PIPE_FLOW_COLORS).toEqual({
      normal: "#00b7dc",
      warning: "#ff9d20",
      critical: "#ff2645",
      offline: "#657681",
    });
  });

  it("uses time and speed in the axial flow calculation", () => {
    expect(PIPE_FLOW_FRAGMENT_SHADER).toContain("uTime * uSpeed");
    expect(PIPE_FLOW_FRAGMENT_SHADER).toContain("flowCoordinate * 2.4");
  });

  it("rejects invalid speed and opacity values", () => {
    expect(() => createPipeFlowUniforms({ status: "normal", speed: -0.1 })).toThrow(RangeError);
    expect(() => createPipeFlowUniforms({ status: "normal", speed: 1, opacity: 1.1 })).toThrow(RangeError);
  });
});
