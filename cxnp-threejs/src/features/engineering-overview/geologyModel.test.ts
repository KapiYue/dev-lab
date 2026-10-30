import { describe, expect, it } from "vitest";
import {
  clampGeologyOpacity,
  getCutawayThreshold,
  isGeologyFacility,
  simulatedGeologyLayers,
  validateGeologyLayers,
} from "./geologyModel";

describe("simulated geology model", () => {
  it("defines four continuous simulated soil layers", () => {
    expect(validateGeologyLayers(simulatedGeologyLayers)).toHaveLength(4);
    expect(simulatedGeologyLayers[0].topDepth).toBe(0);
    expect(simulatedGeologyLayers.at(-1)?.bottomDepth).toBe(19.5);
    expect(simulatedGeologyLayers.every((layer) => layer.dataSource === "simulated")).toBe(true);
  });

  it("restricts the geology preview to the two intake facilities", () => {
    expect(isGeologyFacility("gujiaqiao-intake")).toBe(true);
    expect(isGeologyFacility("liuxia-river-intake")).toBe(true);
    expect(isGeologyFacility("southwest-pump")).toBe(false);
  });

  it("clamps opacity to a readable transparent range", () => {
    expect(clampGeologyOpacity(0.05)).toBe(0.2);
    expect(clampGeologyOpacity(0.7)).toBe(0.7);
    expect(clampGeologyOpacity(2)).toBe(0.92);
    expect(clampGeologyOpacity(Number.NaN)).toBe(0.68);
  });

  it("maps a cutaway percentage across the local north-south footprint", () => {
    expect(getCutawayThreshold(0, 60)).toBe(-30);
    expect(getCutawayThreshold(50, 60)).toBe(0);
    expect(getCutawayThreshold(100, 60)).toBe(30);
  });

  it("rejects gaps and invalid layer counts", () => {
    expect(() => validateGeologyLayers(simulatedGeologyLayers.slice(0, 2))).toThrow(
      "between three and five layers",
    );
    expect(() => validateGeologyLayers([
      simulatedGeologyLayers[0],
      { ...simulatedGeologyLayers[1], topDepth: 3 },
      simulatedGeologyLayers[2],
    ])).toThrow("continuous");
  });
});
