import * as THREE from "three";
import { GLTFExporter } from "three/examples/jsm/exporters/GLTFExporter.js";
import { mkdir, writeFile } from "node:fs/promises";

class NodeFileReader {
  result = null;
  onloadend = null;
  async readAsArrayBuffer(blob) { this.result = await blob.arrayBuffer(); this.onloadend?.(); }
  async readAsDataURL(blob) {
    const bytes = Buffer.from(await blob.arrayBuffer());
    this.result = `data:${blob.type || "application/octet-stream"};base64,${bytes.toString("base64")}`;
    this.onloadend?.();
  }
}
globalThis.FileReader = NodeFileReader;

const exporter = new GLTFExporter();
const output = new URL("../public/models/", import.meta.url);
await mkdir(output, { recursive: true });

const materials = {
  steel: new THREE.MeshStandardMaterial({ color: 0x294658, metalness: 0.75, roughness: 0.32 }),
  dark: new THREE.MeshStandardMaterial({ color: 0x101d26, metalness: 0.82, roughness: 0.4 }),
  cyan: new THREE.MeshStandardMaterial({ color: 0x0ec9b2, metalness: 0.45, roughness: 0.25, emissive: 0x07554c, emissiveIntensity: 0.65 }),
  orange: new THREE.MeshStandardMaterial({ color: 0xe8872c, metalness: 0.35, roughness: 0.35 }),
  concrete: new THREE.MeshStandardMaterial({ color: 0x71818a, metalness: 0.05, roughness: 0.85 }),
};

function mesh(geometry, material, name, position = [0, 0, 0], rotation = [0, 0, 0]) {
  const value = new THREE.Mesh(geometry, material);
  value.name = name;
  value.position.set(...position);
  value.rotation.set(...rotation);
  value.castShadow = true;
  value.receiveShadow = true;
  return value;
}

function gate() {
  const root = new THREE.Group(); root.name = "GateAssembly";
  root.add(mesh(new THREE.BoxGeometry(4.8, .45, 3.8), materials.concrete, "ChannelBase", [0, -.25, 0]));
  root.add(mesh(new THREE.BoxGeometry(.38, 5, .5), materials.steel, "GuideLeft", [-1.8, 2.3, 0]));
  root.add(mesh(new THREE.BoxGeometry(.38, 5, .5), materials.steel, "GuideRight", [1.8, 2.3, 0]));
  root.add(mesh(new THREE.BoxGeometry(4.2, .4, .6), materials.orange, "TopBeam", [0, 4.65, 0]));
  root.add(mesh(new THREE.BoxGeometry(3.25, 2.8, .28), materials.cyan, "GateBoard", [0, 1.45, 0]));
  root.add(mesh(new THREE.CylinderGeometry(.11, .11, 2.7, 12), materials.steel, "LiftRod", [0, 4.05, 0]));
  return root;
}

function pump() {
  const root = new THREE.Group(); root.name = "PumpAssembly";
  root.add(mesh(new THREE.BoxGeometry(4.8, .45, 3.4), materials.concrete, "PumpBase", [0, -.2, 0]));
  root.add(mesh(new THREE.CylinderGeometry(1.25, 1.25, 2.1, 32), materials.cyan, "PumpHousing", [0, 1.2, 0], [Math.PI / 2, 0, 0]));
  root.add(mesh(new THREE.CylinderGeometry(.5, .5, 3.8, 24), materials.steel, "OutletPipe", [1.55, 1.2, 0], [0, 0, Math.PI / 2]));
  root.add(mesh(new THREE.CylinderGeometry(.48, .48, 2.3, 24), materials.steel, "InletPipe", [-1.2, .5, 0], [0, 0, Math.PI / 2]));
  const rotor = new THREE.Group(); rotor.name = "Rotor"; rotor.position.set(0, 1.2, 1.15);
  for (let index = 0; index < 6; index += 1) rotor.add(mesh(new THREE.BoxGeometry(.24, .85, .12), materials.orange, `Blade${index}`, [0, .52, 0], [0, 0, index * Math.PI / 3]));
  rotor.add(mesh(new THREE.CylinderGeometry(.28, .28, .3, 20), materials.dark, "Hub", [0, 0, 0], [Math.PI / 2, 0, 0]));
  root.add(rotor);
  return root;
}

function manhole() {
  const root = new THREE.Group(); root.name = "ManholeAssembly";
  root.add(mesh(new THREE.CylinderGeometry(1.72, 1.72, .42, 40), materials.concrete, "ManholeRing", [0, .12, 0]));
  const lid = new THREE.Group(); lid.name = "ManholeLid"; lid.position.set(0, .42, 0);
  lid.add(mesh(new THREE.CylinderGeometry(1.48, 1.48, .22, 48), materials.dark, "LidPlate"));
  for (let i = 0; i < 12; i += 1) lid.add(mesh(new THREE.BoxGeometry(.1, .07, 2.35), materials.steel, `LidRib${i}`, [0, .15, 0], [0, i * Math.PI / 6, 0]));
  lid.add(mesh(new THREE.TorusGeometry(.72, .05, 8, 32), materials.orange, "InspectRing", [0, .17, 0], [Math.PI / 2, 0, 0]));
  root.add(lid);
  return root;
}

async function save(name, scene) {
  const result = await exporter.parseAsync(scene, { binary: false, onlyVisible: true });
  await writeFile(new URL(`${name}.gltf`, output), JSON.stringify(result));
}

await Promise.all([save("gate", gate()), save("pump", pump()), save("manhole", manhole())]);
console.log("Generated public/models/gate.gltf, pump.gltf, manhole.gltf");
