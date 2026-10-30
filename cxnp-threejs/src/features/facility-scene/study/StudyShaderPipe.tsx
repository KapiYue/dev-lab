
import { useMemo, useRef } from "react";
import { useFrame } from "@react-three/fiber";
import * as THREE from "three";

// 1. 顶点着色器：传递 UV 坐标
const vertexShader = `
  varying vec2 vUv;

  void main() {
    vUv = uv;

    gl_Position =
      projectionMatrix *
      modelViewMatrix *
      vec4(position, 1.0);
  }
`;

// 2. 片元着色器：实现水流动画
const fragmentShader = `
  uniform float uTime;
  uniform float uSpeed;
  uniform float uFlowEnabled;

  varying vec2 vUv;

  void main() {
    // 沿管道长度方向运动
    float movement =
      vUv.y * 40.0 - uTime * uSpeed * 3.0;

    // 创建周期性的亮色水流条纹
    float stripe = pow(
      max(sin(movement), 0.0),
      6.0
    );

    stripe *= uFlowEnabled;

    // 管道基础颜色
    vec3 baseColor = vec3(1.0, 0.0, 0.0);// 红色

    // 水流高亮颜色
    vec3 flowColor = vec3(0.0, 1.0, 1.0);// 青色

    float intensity = stripe * 0.3;

    // 混合颜色，整体以红色为主，只在水流位置叠加少量青色。
    vec3 color = mix(baseColor, flowColor, stripe);


    gl_FragColor = vec4(color, 1.0);
  }
`;

type StudyShaderPipeProps = {
  from: readonly [number, number];
  to: readonly [number, number];
  radius: number;
  shaderFlow: boolean;
  flowSpeed: number;
};

export function StudyShaderPipe({
  from,
  to,
  radius,
  shaderFlow,
  flowSpeed,
}: StudyShaderPipeProps) {

  // 3. 管道的起点和终点
  // 本例假设二维坐标对应 Three.js 的 X、Z 轴
  const start = useMemo(
    () => new THREE.Vector3(from[0], 0, from[1]),
    [from[0], from[1]]
  );

  const end = useMemo(
    () => new THREE.Vector3(to[0], 0, to[1]),
    [to[0], to[1]]
  );

  // 4. 计算管道长度
  const length = start.distanceTo(end);

  // 5. 计算中心位置
  const center = useMemo(
    () => start.clone().add(end).multiplyScalar(0.5),
    [start, end]
  );

  // 6. 计算旋转方向
  // CylinderGeometry 默认沿 Y 轴
  const quaternion = useMemo(() => {
    const direction = end.clone().sub(start).normalize();

    return new THREE.Quaternion().setFromUnitVectors(
      new THREE.Vector3(0, 1, 0),
      direction
    );
  }, [start, end]);

  // 7. 定义 Shader 的 uniforms
  const uniforms = useMemo(() => ({
    uTime: { value: 0 },
    uSpeed: { value: flowSpeed },
    uFlowEnabled: { value: shaderFlow ? 1 : 0 }
  }), []);

  // 8. 每帧更新 Shader 参数
  useFrame(({ clock }) => {
    uniforms.uTime.value = clock.getElapsedTime();
    uniforms.uSpeed.value = flowSpeed;
    uniforms.uFlowEnabled.value = shaderFlow ? 1 : 0;
  });

  // 9. 只创建一个 Mesh
  return (
    <mesh
      name="study-shader-pipe"
      position={center}
      quaternion={quaternion}
    >
      <cylinderGeometry
        args={[radius, radius, length, 32]}
      />

      <shaderMaterial
        vertexShader={vertexShader}
        fragmentShader={fragmentShader}
        uniforms={uniforms}
        side={THREE.DoubleSide}
      />
    </mesh>
  );
}
