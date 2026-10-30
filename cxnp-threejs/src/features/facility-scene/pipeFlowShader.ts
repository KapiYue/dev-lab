import * as THREE from "three";

export type PipeFlowStatus = "normal" | "warning" | "critical" | "offline";

export const PIPE_FLOW_COLORS: Record<PipeFlowStatus, string> = {
  normal: "#00b7dc",
  warning: "#ff9d20",
  critical: "#ff2645",
  offline: "#657681",
};

export interface PipeFlowUniformOptions {
  status: PipeFlowStatus;
  speed: number;
  opacity?: number;
  axis?: "x" | "y";
}

export function createPipeFlowUniforms({
  status,
  speed,
  opacity = 0.88,
  axis = "y",
}: PipeFlowUniformOptions) {
  if (!Number.isFinite(speed) || speed < 0) {
    throw new RangeError("Pipe flow speed must be a finite non-negative number.");
  }
  if (!Number.isFinite(opacity) || opacity < 0 || opacity > 1) {
    throw new RangeError("Pipe flow opacity must be between 0 and 1.");
  }

  return {
    uTime: { value: 0 },
    uSpeed: { value: speed },
    uColor: { value: new THREE.Color(PIPE_FLOW_COLORS[status]) },
    uOpacity: { value: opacity },
    uAxisX: { value: axis === "x" ? 1 : 0 },
  };
}

export function getPipeFlowPhase(time: number, speed: number) {
  const phase = (time * speed) % 1;
  return phase < 0 ? phase + 1 : phase;
}

export const PIPE_FLOW_VERTEX_SHADER = /* glsl */ `
  varying vec2 vFlowUv;

  void main() {
    vFlowUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`;

export const PIPE_FLOW_FRAGMENT_SHADER = /* glsl */ `
  uniform float uTime;
  uniform float uSpeed;
  uniform vec3 uColor;
  uniform float uOpacity;
  uniform float uAxisX;
  varying vec2 vFlowUv;

  void main() {
    float flowCoordinate = mix(vFlowUv.y, vFlowUv.x, uAxisX);
    float phase = fract(flowCoordinate * 2.4 - uTime * uSpeed * 0.65);
    float streak = smoothstep(0.08, 0.24, phase) * (1.0 - smoothstep(0.48, 0.72, phase));
    float glow = 0.72 + streak * 0.38;
    vec3 luminousColor = uColor * glow;
    float alpha = uOpacity * (0.58 + streak * 0.32);

    gl_FragColor = vec4(luminousColor, alpha);
  }
`;
