import { useLayoutEffect, useRef } from "react";
import * as THREE from "three";

const COUNT = 1000;

function NormalPipes() {
  return (
    <group>
      {Array.from({ length: COUNT }, (_, i) => (
        <mesh key={i} position={[(i % 40) * 1.5, 0, Math.floor(i / 40) * 1.5]}>
          <cylinderGeometry args={[0.2, 0.2, 1, 12]} />
          <meshStandardMaterial color="#54a9bd" />
        </mesh>
      ))}
    </group>
  );
}

function InstancedPipes() {
  const ref = useRef<THREE.InstancedMesh>(null);

  useLayoutEffect(() => {
    const mesh = ref.current;
    if (!mesh) return;
// 在这里打印
console.log("管道实例数量mesh count:", mesh.count);
    const dummy = new THREE.Object3D();

    for (let i = 0; i < COUNT; i++) {
      dummy.position.set(
        (i % 40) * 1.5,
        0,
        Math.floor(i / 40) * 1.5
      );

      dummy.scale.set(0.2, 1, 0.2);
      dummy.updateMatrix();

      mesh.setMatrixAt(i, dummy.matrix);
    }

    mesh.instanceMatrix.needsUpdate = true;
    mesh.computeBoundingSphere();
  }, []);

  return (
    <instancedMesh ref={ref} args={[undefined, undefined, COUNT]}>
      <cylinderGeometry args={[1, 1, 1, 12]} />
      <meshStandardMaterial color="#54a9bd" />
    </instancedMesh>
  );
}

export function PipePerformanceTest({
  mode,
}: {
  mode: "normal" | "instanced";
}) {
  return mode === "normal" ? <NormalPipes /> : <InstancedPipes />;
}