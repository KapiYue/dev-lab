// import { useFrame, useThree } from "@react-three/fiber";
// import { useRef } from "react";

// export function DrawCallMonitor() {
//   const gl = useThree((state) => state.gl);
//   const elapsed = useRef(0);

//   useFrame((_, delta) => {
//     elapsed.current += delta;

//     if (elapsed.current >= 1) {
//       elapsed.current = 0;

//       console.log("Draw Calls:", gl.info.render.calls);
//       console.log("Triangles:", gl.info.render.triangles);
//     }
//   });

//   return null;
// }
import { useThree } from "@react-three/fiber";
import { useEffect } from "react";

export function DrawCallMonitor({
  mode,
}: {
  mode: "normal" | "instanced";
}) {
  const { gl, scene, camera } = useThree();

  useEffect(() => {
    // 等待本轮 React/Three 场景更新完成
    const timer = window.setTimeout(() => {
      const previousAutoReset = gl.info.autoReset;

      gl.info.autoReset = false;
      gl.info.reset();

      gl.render(scene, camera);

      console.table({
        mode,
        drawCalls: gl.info.render.calls, // Draw Calls，对比mesh count
        triangles: gl.info.render.triangles,
        geometries: gl.info.memory.geometries,
      });

      gl.info.autoReset = previousAutoReset;
    }, 300);

    return () => window.clearTimeout(timer);
  }, [gl, scene, camera, mode]);

  return null;
}