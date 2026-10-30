import { Component, Suspense, useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Canvas, useFrame, useThree } from "@react-three/fiber";
import { Environment, Grid, Html, OrbitControls, useGLTF, useProgress } from "@react-three/drei";
import * as THREE from "three";
import { EffectComposer } from "three/examples/jsm/postprocessing/EffectComposer.js";
import { OutputPass } from "three/examples/jsm/postprocessing/OutputPass.js";
import { RenderPass } from "three/examples/jsm/postprocessing/RenderPass.js";
import { UnrealBloomPass } from "three/examples/jsm/postprocessing/UnrealBloomPass.js";
import { createCurvedPipePath, type CurvedPipePoint } from "./curvedPipe";
import {
  PIPE_FLOW_COLORS,
  PIPE_FLOW_FRAGMENT_SHADER,
  PIPE_FLOW_VERTEX_SHADER,
  createPipeFlowUniforms,
  type PipeFlowStatus,
} from "./pipeFlowShader";
import {
  advanceParticlePhase,
  createParticlePhases,
  createParticleRadialOffsets,
  writeParticlePositions,
} from "./pipeFlowParticles";
import { PIPE_INSTANCE_VISUALS, applyPipeInstanceStates, type PipeInstanceStatus } from "./pipeInstanceState";
import { FLOW_LINE_OFFSETS, createOffsetPipePath } from "./pipeVisualEnhancement";
import { getStraightPipeInstanceTransform, getStraightPipeTransform, type PipePoint } from "./straightPipe";
import {StudyShaderPipe} from './study/StudyShaderPipe'
import { DrawCallMonitor } from "./study/DrawCallMonitor";
import { PipePerformanceTest } from './study/PipePerformanceTest'



type SiteScene = {
  title: string;
  subtitle: string;
  gateLabel: string;
  pumpLabel: string;
  manholes: Array<[number, number]>;
  pipes: Array<[[number, number], [number, number], number, PipeInstanceStatus]>;
  studyPipe: [[number, number], [number, number], number];
  curvedStudyPipe: { points: CurvedPipePoint[]; radius: number };
  channel: { position: [number, number, number]; size: [number, number] };
  devicePositions: { gate: [number, number, number]; pump: [number, number, number]; manhole: [number, number, number] };
};

// Keep learning objects in dedicated foreground zones so they do not pass
// through the GLTF devices when all comparison switches are enabled.
const ordinaryStudyPipe: SiteScene["studyPipe"] = [[1.1, 5.2], [4.5, 5.2], .5];
const curvedStudyPipe: SiteScene["curvedStudyPipe"] = {
  points: [[1.05, 2.65], [1.9, 2.55], [2.65, 3.45], [3.7, 3.2], [4.65, 4.15]],
  radius: .3,
};
const instancedStudyPipes: SiteScene["pipes"] = [
  [[-3.9, 2.75], [-1.3, 2.75], .29, "normal"],
  [[-3.75, 3.55], [-.65, 3.55], .32, "warning"],
  [[-3.6, 4.35], [-.1, 4.18], .35, "critical"],
  [[-3.45, 5.1], [.3, 4.82], .38, "stopped"],
];
const instancedStudyManholes: SiteScene["manholes"] = [
  [-4.05, 2.75],
  [-3.9, 3.55],
  [-3.75, 4.35],
  [-3.6, 5.1],
];

const siteScenes: Record<string, SiteScene> = {
  "gujiaqiao-intake": {
    title: "顾家桥港进水口施工段", subtitle: "进水渠 · 截流闸 · 地下排水管网",
    gateLabel: "顾家桥截流闸", pumpLabel: "提升泵组 01",
    channel: { position: [-5.8, -.31, 1.4], size: [8.5, 4.2] },
    devicePositions: { gate: [-5.6, 0, .5], pump: [-.4, 0, -1], manhole: [4.6, .1, 1.8] },
    manholes: instancedStudyManholes,
    studyPipe: ordinaryStudyPipe,
    curvedStudyPipe,
    pipes: instancedStudyPipes,
  },
  "liuxia-river-intake": {
    title: "留下河进水口施工段", subtitle: "河道来水 · 格栅井 · 调蓄支管",
    gateLabel: "留下河调节闸", pumpLabel: "低液位提升泵",
    channel: { position: [-4.8, -.31, 1.8], size: [7.2, 4.6] },
    devicePositions: { gate: [-4.8, 0, .7], pump: [.5, 0, -1.1], manhole: [4.2, .1, 1.6] },
    manholes: instancedStudyManholes,
    studyPipe: ordinaryStudyPipe,
    curvedStudyPipe,
    pipes: instancedStudyPipes,
  },
  "southwest-pump": {
    title: "城西南排泵站施工段", subtitle: "泵房 · 出水压力管 · 检修井群",
    gateLabel: "泵站进水闸", pumpLabel: "1# 排水泵",
    channel: { position: [-5.2, -.31, 1.3], size: [7.6, 4.2] },
    devicePositions: { gate: [-5.2, 0, 0], pump: [0, 0, -1], manhole: [5, .1, 2] },
    manholes: instancedStudyManholes,
    studyPipe: ordinaryStudyPipe,
    curvedStudyPipe,
    pipes: instancedStudyPipes,
  },
  "xixi-wetland": {
    title: "西溪湿地监测施工段", subtitle: "生态调蓄池 · 雨污分流支管 · 水位监测井",
    gateLabel: "生态调蓄闸", pumpLabel: "应急排涝泵",
    channel: { position: [-4.7, -.31, 1.4], size: [8.2, 5.2] },
    devicePositions: { gate: [-4.8, 0, .4], pump: [.3, 0, -1.2], manhole: [4.8, .1, 1.8] },
    manholes: instancedStudyManholes,
    studyPipe: ordinaryStudyPipe,
    curvedStudyPipe,
    pipes: instancedStudyPipes,
  },
};

const fallbackScene = siteScenes["southwest-pump"];

function LoadingModel() {
  const { progress } = useProgress();
  return <Html center><div className="model-loading"><i style={{ width: `${progress}%` }}/><span>三维设备加载中 {Math.round(progress)}%</span></div></Html>;
}

function WaterSurface({ channel }: { channel: SiteScene["channel"] }) {
  const ref = useRef<THREE.Mesh>(null);
  useFrame(({ clock }) => {
    if (ref.current) ref.current.position.y = -0.32 + Math.sin(clock.elapsedTime * .9) * .035;
  });
  return <mesh ref={ref} rotation={[-Math.PI / 2, 0, 0]} position={channel.position} receiveShadow>
    <planeGeometry args={[channel.size[0], channel.size[1], 30, 18]}/><meshPhysicalMaterial color="#086c83" transparent opacity={.64} roughness={.2} metalness={.2}/>
  </mesh>;
}

function PipeFlowMaterial({ status, speed, axis, opacity = .82 }: { status: PipeFlowStatus; speed: number; axis: "x" | "y"; opacity?: number }) {
  const uniforms = useMemo(() => createPipeFlowUniforms({ status, speed, axis, opacity }), [axis, opacity]);

  useEffect(() => {
    uniforms.uColor.value.set(PIPE_FLOW_COLORS[status]);
    uniforms.uSpeed.value = speed;
  }, [speed, status, uniforms]);

  useFrame(({ clock }) => {
    uniforms.uTime.value = clock.elapsedTime;
  });

  return <shaderMaterial
    uniforms={uniforms}
    vertexShader={PIPE_FLOW_VERTEX_SHADER}
    fragmentShader={PIPE_FLOW_FRAGMENT_SHADER}
    transparent
    depthWrite={false}
    blending={THREE.NormalBlending}
    toneMapped={false}
  />;
}

function GlassPipeMaterial() {
  return <meshPhysicalMaterial
    color="#c9f3ff"
    metalness={0}
    roughness={.045}
    transmission={.94}
    thickness={.2}
    ior={1.45}
    clearcoat={1}
    clearcoatRoughness={.08}
    envMapIntensity={1.65}
    attenuationColor="#6bdcff"
    attenuationDistance={6}
    transparent
    opacity={.4}
    depthWrite={false}
    side={THREE.DoubleSide}
  />;
}

function FlowLines({ path, radius, status, speed, animated }: { path: THREE.Curve<THREE.Vector3>; radius: number; status: PipeFlowStatus; speed: number; animated: boolean }) {
  const lines = useMemo(
    () => FLOW_LINE_OFFSETS.map(([lateral, vertical]) => createOffsetPipePath(path, radius, lateral, vertical)),
    [path, radius],
  );

  return <group name="pipe-fine-flow-lines" renderOrder={2}>
    {lines.map((line, index) => <mesh key={index} renderOrder={2}>
      <tubeGeometry args={[line, 64, radius * (index === 0 ? .045 : .032), 8, false]}/>
      {animated
        ? <PipeFlowMaterial status={status} speed={speed} axis="x" opacity={index === 0 ? .9 : .68}/>
        : <meshBasicMaterial color={PIPE_FLOW_COLORS[status]} transparent opacity={index === 0 ? .72 : .5} depthWrite={false} toneMapped={false}/>}
    </mesh>)}
  </group>;
}

function PipeEndCoupling({ position, tangent, radius }: { position: THREE.Vector3; tangent: THREE.Vector3; radius: number }) {
  const quaternion = useMemo(
    () => new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 0, 1), tangent.clone().normalize()),
    [tangent],
  );

  return <group position={position} quaternion={quaternion} name="pipe-metal-coupling">
    <mesh castShadow>
      <torusGeometry args={[radius * 1.05, radius * .12, 12, 36]}/>
      <meshStandardMaterial color="#66899a" metalness={.9} roughness={.16}/>
    </mesh>
    <mesh rotation={[Math.PI / 2, 0, 0]}>
      <cylinderGeometry args={[radius * 1.12, radius * 1.12, radius * .24, 36, 1, true]}/>
      <meshStandardMaterial color="#426272" metalness={.86} roughness={.2} side={THREE.DoubleSide}/>
    </mesh>
    <mesh scale={.88}>
      <torusGeometry args={[radius * 1.05, radius * .035, 8, 36]}/>
      <meshBasicMaterial color="#bdefff" transparent opacity={.7} toneMapped={false}/>
    </mesh>
  </group>;
}

function FlowDirectionMarkers({ path, radius, status, speed, visible }: { path: THREE.Curve<THREE.Vector3>; radius: number; status: PipeFlowStatus; speed: number; visible: boolean }) {
  const groupRef = useRef<THREE.Group>(null);
  const phase = useRef(0);
  const tangent = useMemo(() => new THREE.Vector3(), []);
  const directionQuaternion = useMemo(() => new THREE.Quaternion(), []);
  const cylinderAxis = useMemo(() => new THREE.Vector3(0, 1, 0), []);
  const markerCount = 4;

  useFrame((_, delta) => {
    if (!visible || !groupRef.current || speed <= 0) return;
    phase.current = advanceParticlePhase(phase.current, delta, speed * .78);
    groupRef.current.children.forEach((marker, index) => {
      const progress = (phase.current + index / markerCount) % 1;
      path.getPointAt(progress, marker.position);
      path.getTangentAt(progress, tangent).normalize();
      directionQuaternion.setFromUnitVectors(cylinderAxis, tangent);
      marker.quaternion.copy(directionQuaternion);
    });
  });

  return <group ref={groupRef} visible={visible} name="pipe-direction-arrows" renderOrder={4}>
    {Array.from({ length: markerCount }, (_, index) => <group key={index}>
      <mesh position={[0, radius * .13, 0]}>
        <coneGeometry args={[radius * .16, radius * .42, 16]}/>
        <meshBasicMaterial color={PIPE_FLOW_COLORS[status]} transparent opacity={.92} depthWrite={false} toneMapped={false}/>
      </mesh>
    </group>)}
  </group>;
}

function createParticleSprite() {
  const canvas = document.createElement("canvas");
  canvas.width = 64;
  canvas.height = 64;
  const context = canvas.getContext("2d");
  if (!context) return null;
  const gradient = context.createRadialGradient(32, 32, 0, 32, 32, 30);
  gradient.addColorStop(0, "rgba(255,255,255,1)");
  gradient.addColorStop(.35, "rgba(255,255,255,.95)");
  gradient.addColorStop(1, "rgba(255,255,255,0)");
  context.fillStyle = gradient;
  context.fillRect(0, 0, 64, 64);
  return new THREE.CanvasTexture(canvas);
}

function PipeFlowParticles({ path, radius, status, speed, count, visible }: { path: THREE.Curve<THREE.Vector3>; radius: number; status: PipeFlowStatus; speed: number; count: number; visible: boolean }) {
  const positionAttribute = useRef<THREE.BufferAttribute>(null);
  const phaseOffset = useRef(0);
  const phases = useMemo(() => createParticlePhases(count), [count]);
  const radialOffsets = useMemo(() => createParticleRadialOffsets(count, radius), [count, radius]);
  const sprite = useMemo(() => createParticleSprite(), []);
  const positions = useMemo(
    () => writeParticlePositions(path, phases, 0, new Float32Array(count * 3), radialOffsets),
    [count, path, phases, radialOffsets],
  );

  useEffect(() => {
    phaseOffset.current = 0;
    writeParticlePositions(path, phases, 0, positions, radialOffsets);
    if (positionAttribute.current) positionAttribute.current.needsUpdate = true;
  }, [path, phases, positions, radialOffsets]);

  useEffect(() => () => sprite?.dispose(), [sprite]);

  useFrame((_, delta) => {
    if (!visible || speed === 0 || !positionAttribute.current) return;
    phaseOffset.current = advanceParticlePhase(phaseOffset.current, delta, speed);
    writeParticlePositions(path, phases, phaseOffset.current, positions, radialOffsets);
    positionAttribute.current.needsUpdate = true;
  });

  return <points name="pipe-flow-particles" visible={visible} renderOrder={2} frustumCulled={false}>
    <bufferGeometry>
      {/* 而是“生成一个点云对象，里面有 260 个点” */}
      <bufferAttribute ref={positionAttribute} attach="attributes-position" args={[positions, 3]}/>
    </bufferGeometry>
    <pointsMaterial
      color={PIPE_FLOW_COLORS[status]}
      size={.042}
      sizeAttenuation
      map={sprite}
      alphaMap={sprite}
      alphaTest={.025}
      transparent
      opacity={.82}
      depthWrite={false}
      blending={THREE.NormalBlending}
      toneMapped={false}
    />
  </points>;
}

function StraightPipe({ from, to, radius, shaderFlow, particleFlow, flowStatus, flowSpeed }: { from: PipePoint; to: PipePoint; radius: number; shaderFlow: boolean; particleFlow: boolean; flowStatus: PipeFlowStatus; flowSpeed: number }) {
  const transform = useMemo(() => getStraightPipeTransform(from, to), [from, to]);
  const particlePath = useMemo(() => new THREE.LineCurve3(
    new THREE.Vector3(0, -transform.length / 2, 0),
    new THREE.Vector3(0, transform.length / 2, 0),
  ), [transform.length]);

  return <group
    name="straight-pipe"
    position={transform.midpoint}
    quaternion={transform.quaternion}
    userData={{ kind: "ordinary-mesh", length: transform.length, radius }}
  >
    <mesh name="pipe-shell" castShadow receiveShadow>
      <cylinderGeometry args={[radius, radius, transform.length, 48, 1, true]}/>
      <GlassPipeMaterial/>
    </mesh>
    <FlowLines path={particlePath} radius={radius * .72} status={flowStatus} speed={flowSpeed} animated={shaderFlow}/>
    <PipeFlowParticles path={particlePath} radius={radius * .62} status={flowStatus} speed={flowSpeed} count={180} visible={particleFlow && flowSpeed > 0}/>
    <FlowDirectionMarkers path={particlePath} radius={radius} status={flowStatus} speed={flowSpeed} visible={particleFlow && flowSpeed > 0}/>
    <PipeEndCoupling position={new THREE.Vector3(0, -transform.length / 2, 0)} tangent={new THREE.Vector3(0, 1, 0)} radius={radius}/>
    <PipeEndCoupling position={new THREE.Vector3(0, transform.length / 2, 0)} tangent={new THREE.Vector3(0, 1, 0)} radius={radius}/>
  </group>;
}

function OrdinaryPipeStudy({ 
  scene, shaderFlow, particleFlow, flowStatus, flowSpeed 
  }: { 
  scene: SiteScene; shaderFlow: boolean; particleFlow: boolean; flowStatus: PipeFlowStatus; flowSpeed: number }) {
    // 从数组取出起点，终点，半径
  const [from, to, radius] = scene.studyPipe;
  return <StraightPipe from={from} to={to} radius={radius} shaderFlow={shaderFlow} particleFlow={particleFlow} flowStatus={flowStatus} flowSpeed={flowSpeed}/>;
}

function OrdinaryPipeStudy1({
  scene,
  shaderFlow,
  flowSpeed
}: {
  scene: SiteScene;
  shaderFlow: boolean;
  particleFlow: boolean;
  flowStatus: PipeFlowStatus;
  flowSpeed: number;
}) {
  const [from, to, radius] = scene.studyPipe;
 
  return (
    <StudyShaderPipe
      from={[4.8, 5.2]}
      to={[8.5, 5.2]}
      radius={radius}
      shaderFlow={shaderFlow}
      flowSpeed={flowSpeed}
    />
  );
}

function CurvedPipeStudy({ scene, shaderFlow, particleFlow, flowStatus, flowSpeed }: { scene: SiteScene; shaderFlow: boolean; particleFlow: boolean; flowStatus: PipeFlowStatus; flowSpeed: number }) {
  const { points, radius } = scene.curvedStudyPipe;
  const path = useMemo(() => createCurvedPipePath(points), [points]);
  const labelPosition = useMemo(() => path.getPoint(.54).add(new THREE.Vector3(0, .78, 0)), [path]);
  const startPosition = useMemo(() => path.getPointAt(0), [path]);
  const endPosition = useMemo(() => path.getPointAt(1), [path]);
  const startTangent = useMemo(() => path.getTangentAt(0), [path]);
  const endTangent = useMemo(() => path.getTangentAt(1), [path]);

  // return 'todo 学习粒子水流'
  return <group name="curved-pipe-study" userData={{ kind: "tube-geometry", controlPointCount: points.length }}>
    <mesh name="curved-pipe-shell" castShadow receiveShadow>
      <tubeGeometry args={[path, 96, radius, 32, false]}/>
      <GlassPipeMaterial/>
    </mesh>
    {/* <FlowLines path={path} radius={radius * .72} status={flowStatus} speed={flowSpeed} animated={shaderFlow}/> */}
    <PipeFlowParticles path={path} radius={radius * .62} status={flowStatus} speed={flowSpeed} count={260} visible={particleFlow && flowSpeed > 0}/>
    <FlowDirectionMarkers path={path} radius={radius} status={flowStatus} speed={flowSpeed} visible={particleFlow && flowSpeed > 0}/>
    <PipeEndCoupling position={startPosition} tangent={startTangent} radius={radius}/>
    <PipeEndCoupling position={endPosition} tangent={endTangent} radius={radius}/>
    {/* <Html center position={labelPosition} distanceFactor={13}>
      <div className="curved-pipe-label"><i/>TubeGeometry · 弯曲路径</div>
    </Html> */}
  </group>;
}

// todo
function InstancedPipeStudy({ scene }: { scene: SiteScene }) {
  const shellRef = useRef<THREE.InstancedMesh>(null); // 管道壳子
  const flowRef = useRef<THREE.InstancedMesh>(null);// 管道内部
  // 生成实例矩阵，内部调用Martrix4函数。
  const transforms = useMemo(
    () => scene.pipes.map(([from, to, radius]) => getStraightPipeInstanceTransform(from, to, radius)),// 先计算中点、长度以及旋转四元数
    [scene],
  );
  const statuses = useMemo(() => scene.pipes.map(([, , , status]) => status), [scene]);

  useLayoutEffect(() => {
    transforms.forEach((transform, index) => {
      // 写入外壳实例
      shellRef.current?.setMatrixAt(index, transform.matrix);
      // 复制外壳矩阵
      const flowMatrix = transform.matrix.clone();

      const position = new THREE.Vector3();
      const quaternion = new THREE.Quaternion();
      const scale = new THREE.Vector3();
      flowMatrix.decompose(position, quaternion, scale);

      // 让内部水流比外壳略细、略短
      scale.set(scale.x * .72, scale.y * .985, scale.z * .72);

      flowMatrix.compose(position, quaternion, scale);

      // 写入水流实例
      flowRef.current?.setMatrixAt(index, flowMatrix);
    });

    if (shellRef.current) {
      shellRef.current.instanceMatrix.needsUpdate = true;
      shellRef.current.computeBoundingSphere();
    }
    if (flowRef.current) {
      flowRef.current.instanceMatrix.needsUpdate = true;
      flowRef.current.computeBoundingSphere();
    }
    if (shellRef.current && flowRef.current) {
      applyPipeInstanceStates(shellRef.current, flowRef.current, statuses);
    }
  }, [statuses, transforms]);

  return <group name="instanced-pipe-study" userData={{ kind: "instanced-mesh", count: transforms.length }}>
    {/* 管道壳子 */}
  {/* args={[geometry，material，实例数量]} */}
    <instancedMesh ref={shellRef} args={[undefined, undefined, transforms.length]} castShadow receiveShadow>
      <cylinderGeometry args={[1, 1, 1, 24]}/>
      <meshPhysicalMaterial color="#ffffff" metalness={.42} roughness={.24} transparent opacity={.58} depthWrite={false} side={THREE.DoubleSide}/>
    </instancedMesh>
    {/* 管道内部 */}
    <instancedMesh ref={flowRef} args={[undefined, undefined, transforms.length]} renderOrder={1}>
      <cylinderGeometry args={[1, 1, 1, 20]}/>
      <meshStandardMaterial color="#ffffff" emissive="#07121d" transparent opacity={.72} depthWrite={false}/>
    </instancedMesh>
    {scene.pipes.map(([from, to, , status], index) => <Html
      key={`${status}-${index}`}
      center
      position={[(from[0] + to[0]) / 2, .58, (from[1] + to[1]) / 2]}
      distanceFactor={13}
    >
      <div className={`pipe-instance-label pipe-instance-${status}`}>
        <i/>{index + 1} · {PIPE_INSTANCE_VISUALS[status].label}
      </div>
    </Html>)}
  </group>;
}

function NetworkManholes({ scene }: { scene: SiteScene }) {
  return <group>{scene.manholes.map(([x, z], index) => <group key={`manhole-${index}`} position={[x, .08, z]}>
      <mesh rotation={[-Math.PI / 2, 0, 0]} receiveShadow><ringGeometry args={[.64, .88, 28]}/><meshStandardMaterial color="#506674" metalness={.82} roughness={.35}/></mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, .015, 0]} receiveShadow><circleGeometry args={[.59, 28]}/><meshStandardMaterial color="#263944" roughness={.75}/></mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, .03, 0]}><ringGeometry args={[.1, .12, 16]}/><meshBasicMaterial color="#1ee6ab" transparent opacity={.8}/></mesh>
  </group>)}</group>;
}

function LoadedDevices({ scene, running, gateOpening, manholeOpen, onSelect }: { scene: SiteScene; running: boolean; gateOpening: number; manholeOpen: boolean; onSelect: (name: string) => void }) {
  const gate = useGLTF("/models/gate.gltf").scene.clone();
  const pump = useGLTF("/models/pump.gltf").scene.clone();
  const manhole = useGLTF("/models/manhole.gltf").scene.clone();
  const rotor = useRef<THREE.Object3D | null>(null);
  const gateBoard = useRef<THREE.Object3D | null>(null);
  const lid = useRef<THREE.Object3D | null>(null);
  const velocity = useRef(0);

  useEffect(() => {
    rotor.current = pump.getObjectByName("Rotor") ?? null;
    gateBoard.current = gate.getObjectByName("GateBoard") ?? null;
    lid.current = manhole.getObjectByName("ManholeLid") ?? null;
  }, [gate, pump, manhole]);

  useFrame((_, delta) => {
    velocity.current = THREE.MathUtils.damp(velocity.current, running ? 4.8 : 0, 3.5, delta);
    if (rotor.current) rotor.current.rotation.z += velocity.current * delta;
    if (gateBoard.current) gateBoard.current.position.y = THREE.MathUtils.damp(gateBoard.current.position.y, .55 + gateOpening / 33, 3.8, delta);
    if (lid.current) {
      lid.current.rotation.z = THREE.MathUtils.damp(lid.current.rotation.z, manholeOpen ? -1.15 : 0, 4, delta);
      lid.current.position.y = THREE.MathUtils.damp(lid.current.position.y, manholeOpen ? 1.05 : .42, 4, delta);
    }
  });

  const prep = (object: THREE.Object3D) => object.traverse((child) => { if (child instanceof THREE.Mesh) { child.castShadow = true; child.receiveShadow = true; } });
  prep(gate); prep(pump); prep(manhole);

  return <group>
    <primitive object={gate} position={scene.devicePositions.gate} scale={.78} onClick={(event: any) => { event.stopPropagation(); onSelect(scene.gateLabel); }}/>
    <primitive object={pump} position={scene.devicePositions.pump} scale={.92} onClick={(event: any) => { event.stopPropagation(); onSelect(scene.pumpLabel); }}/>
    <primitive object={manhole} position={scene.devicePositions.manhole} scale={1.05} onClick={(event: any) => { event.stopPropagation(); onSelect("检修井盖 A-03"); }}/>
  </group>;
}

function ProgrammaticFallback() {
  return <group>
    <mesh position={[-4, 1, 0]}><boxGeometry args={[3, 3, .5]}/><meshStandardMaterial color="#12b8aa" metalness={.6}/></mesh>
    <mesh position={[0, 1, 0]} rotation={[Math.PI / 2, 0, 0]}><cylinderGeometry args={[1.4,1.4,2.8,24]}/><meshStandardMaterial color="#277c99" metalness={.7}/></mesh>
    <mesh position={[4, .25, 1]}><cylinderGeometry args={[1.4,1.4,.35,32]}/><meshStandardMaterial color="#384955" metalness={.8}/></mesh>
    <Html center position={[0, 4, 0]}><div className="fallback-note">模型资源异常，已启用程序化设备回退</div></Html>
  </group>;
}

class ModelBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  render() { return this.state.failed ? <ProgrammaticFallback/> : this.props.children; }
}

export type FacilitySceneView = "normal" | "study";

function SceneCamera({ resetSignal, viewMode }: { resetSignal: number; viewMode: FacilitySceneView }) {
  const { camera } = useThree();
  const controls = useRef<any>(null);
  useEffect(() => {
    const isStudyView = viewMode === "study";
    if (isStudyView) {
      camera.position.set(0, 11.5, 19);
      controls.current?.target.set(0, .2, 0);
    } else {
      camera.position.set(13, 9.5, 16.5);
      controls.current?.target.set(.8, .4, -.5);
    }
    if (camera instanceof THREE.PerspectiveCamera) {
      camera.fov = isStudyView ? 60 : 42;
      camera.updateProjectionMatrix();
    }
    controls.current?.update();
  }, [camera, resetSignal, viewMode]);
  return <OrbitControls ref={controls} makeDefault enableDamping dampingFactor={.06} minDistance={6} maxDistance={34} maxPolarAngle={Math.PI / 2.05} autoRotate autoRotateSpeed={.25}/>;
}

function Hotspot({ position, label, status, visible }: { position: [number, number, number]; label: string; status: string; visible: boolean }) {
  if (!visible) return null;
  return <Html position={position} distanceFactor={12}><button className="scene-hotspot"><i/><span><strong>{label}</strong><small>{status}</small></span></button></Html>;
}

function SceneBloom() {
  const { gl, scene, camera, size } = useThree();
  const composer = useMemo(() => {
    const nextComposer = new EffectComposer(gl);
    nextComposer.addPass(new RenderPass(scene, camera));
    nextComposer.addPass(new UnrealBloomPass(new THREE.Vector2(size.width, size.height), .38, .24, .98));
    nextComposer.addPass(new OutputPass());
    return nextComposer;
  }, [camera, gl, scene]);

  useEffect(() => {
    composer.setSize(size.width, size.height);
  }, [composer, size.height, size.width]);

  useEffect(() => () => composer.dispose(), [composer]);
  useFrame(() => composer.render(), 1);
  return null;
}

function SceneRenderingSettings({ viewMode }: { viewMode: FacilitySceneView }) {
  const { gl } = useThree();
  useEffect(() => {
    gl.toneMappingExposure = viewMode === "study" ? .8 : 1;
    return () => { gl.toneMappingExposure = 1; };
  }, [gl, viewMode]);
  return null;
}

export function FacilityTwinScene({ facilityId, running, gateOpening, manholeOpen, showHotspots, showOrdinaryPipe, showInstancedPipes, showCurvedPipe, showShaderFlow, showParticleFlow, shaderFlowStatus, shaderFlowSpeed, viewMode, resetSignal, onSelect }: { facilityId: string; running: boolean; gateOpening: number; manholeOpen: boolean; showHotspots: boolean; showOrdinaryPipe: boolean; showInstancedPipes: boolean; showCurvedPipe: boolean; showShaderFlow: boolean; showParticleFlow: boolean; shaderFlowStatus: PipeFlowStatus; shaderFlowSpeed: number; viewMode: FacilitySceneView; resetSignal: number; onSelect: (name: string) => void }) {
  const [mode, setMode] = useState<"normal" | "instanced">("normal");
  const [webglError, setWebglError] = useState(false);
  const scene = useMemo(() => siteScenes[facilityId] ?? fallbackScene, [facilityId]);
  useEffect(() => {
    try {
      const canvas = document.createElement("canvas");
      setWebglError(!canvas.getContext("webgl2") && !canvas.getContext("webgl"));
    } catch { setWebglError(true); }
  }, []);
  if (webglError) return <div className="webgl-fallback"><strong>当前环境不可用 WebGL</strong><span>请在已启用硬件加速的 Chrome 或 Edge 中打开。</span></div>;
  return <Canvas shadows dpr={[1, 1.75]} camera={{ position: [0, 11.5, 19], fov: 60 }} gl={{ antialias: true, alpha: false }} onCreated={({ gl }) => {
    // 仅开发环境调试
    // if (import.meta.env.DEV) {
    //   (window as Window & {
    //     __threeRenderer?: THREE.WebGLRenderer;
    //   }).__threeRenderer = gl;
    // }

    gl.setClearColor("#07121d");
    gl.toneMapping = THREE.ACESFilmicToneMapping;
  }}
  
  >
    <fog attach="fog" args={["#07121d", 19, 42]}/>
    <ambientLight intensity={.55}/><hemisphereLight args={["#9be9ff", "#06111a", 1.2]}/>
    <directionalLight castShadow position={[8, 14, 6]} intensity={2.2} color="#c6f4ff" shadow-mapSize={[2048, 2048]}/>
    <pointLight position={[-6, 4, 1]} intensity={14} color="#15d9b0" distance={12}/>
    <WaterSurface channel={scene.channel}/>
    <mesh position={[0, -.48, 0]} receiveShadow><boxGeometry args={[22, .35, 11]}/><meshStandardMaterial color="#172b35" roughness={.82}/></mesh>
    <Grid position={[0, -.29, -1.8]} args={[22, 11]} cellSize={1} cellThickness={.55} cellColor="#18424f" sectionSize={5} sectionColor="#1d8ba0" fadeDistance={28}/>
    {/* 普通Mesh单根 */}
    {viewMode === "study" && showOrdinaryPipe && <OrdinaryPipeStudy 
    scene={scene} shaderFlow={showShaderFlow} 
    particleFlow={showParticleFlow} flowStatus={shaderFlowStatus} flowSpeed={shaderFlowSpeed}/>}
    {/* todo 单个管道学习 */}
    {viewMode == 'study' && <OrdinaryPipeStudy1 scene={scene} shaderFlow={showShaderFlow} 
    particleFlow={showParticleFlow} flowStatus={shaderFlowStatus} flowSpeed={shaderFlowSpeed} />}
    {viewMode === "study" && showInstancedPipes && <group>
      <InstancedPipeStudy scene={scene}/>
      <NetworkManholes scene={scene}/>
      {/* todo 性能学习 */}
      {/* <ambientLight intensity={1} />
<PipePerformanceTest mode={mode} />
<DrawCallMonitor mode={mode} /> */}
      </group>}
    {viewMode === "study" && showCurvedPipe && <CurvedPipeStudy 
    scene={scene} shaderFlow={showShaderFlow} particleFlow={showParticleFlow} flowStatus={shaderFlowStatus} flowSpeed={shaderFlowSpeed}/>}
    <ModelBoundary><Suspense fallback={<LoadingModel/>}><LoadedDevices scene={scene} running={running} gateOpening={gateOpening} manholeOpen={manholeOpen} onSelect={onSelect}/></Suspense></ModelBoundary>
    <Html position={[0, .15, -5.05]} center distanceFactor={14}><div className="site-scene-title"><strong>{scene.title}</strong><span>{scene.subtitle}</span></div></Html>
    <Hotspot visible={showHotspots} position={[scene.devicePositions.gate[0], 4.9, scene.devicePositions.gate[2]]} label={scene.gateLabel} status={`开度 ${gateOpening}%`}/>
    <Hotspot visible={showHotspots} position={[scene.devicePositions.pump[0], 4.1, scene.devicePositions.pump[2]]} label={scene.pumpLabel} status={running ? "运行 · 状态正常" : "停止 · 待机"}/>
    <Hotspot visible={showHotspots} position={[scene.devicePositions.manhole[0], 2.3, scene.devicePositions.manhole[2]]} label="检修井盖 A-03" status={manholeOpen ? "检验中" : "已闭合"}/>
    <Environment preset="city" environmentIntensity={.35}/><SceneCamera resetSignal={resetSignal} viewMode={viewMode}/><SceneRenderingSettings viewMode={viewMode}/>
    {viewMode === "study" && <SceneBloom/>}
  </Canvas>;
}

useGLTF.preload("/models/gate.gltf");
useGLTF.preload("/models/pump.gltf");
useGLTF.preload("/models/manhole.gltf");
