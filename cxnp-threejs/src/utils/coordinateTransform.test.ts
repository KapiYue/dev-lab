import { describe, expect, it } from "vitest";
import { gcj02ToWgs84, wgs84ToGcj02 } from "./coordinateTransform";

describe("coordinate transform", () => {
  it("round trips the authoritative Gujiaqiao coordinate", () => {
    const gcj = [120.043173, 30.257151] as const;
    const wgs = gcj02ToWgs84(gcj);
    const restored = wgs84ToGcj02(wgs);
    expect(restored[0]).toBeCloseTo(gcj[0], 6);
    expect(restored[1]).toBeCloseTo(gcj[1], 6);
  });

  it("does not change coordinates outside China", () => {
    expect(wgs84ToGcj02([0, 0])).toEqual([0, 0]);
  });

});
