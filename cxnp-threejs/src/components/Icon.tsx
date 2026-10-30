import type { SVGProps } from "react";

type IconName = "map" | "cube" | "layers" | "alert" | "activity" | "pump" | "gate" | "back" | "search" | "reset" | "eye";

const paths: Record<IconName, React.ReactNode> = {
  map: <><path d="m3 6 5-3 8 3 5-3v15l-5 3-8-3-5 3z"/><path d="M8 3v15M16 6v15"/></>,
  cube: <><path d="m12 2 9 5-9 5-9-5z"/><path d="m3 7 9 5 9-5v10l-9 5-9-5zM12 12v10"/></>,
  layers: <><path d="m12 2 9 5-9 5-9-5z"/><path d="m3 12 9 5 9-5M3 17l9 5 9-5"/></>,
  alert: <><path d="M10.3 3.6 2.2 18a2 2 0 0 0 1.7 3h16.2a2 2 0 0 0 1.7-3L13.7 3.6a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4M12 17h.01"/></>,
  activity: <path d="M3 12h4l2-6 4 12 2-6h6"/>,
  pump: <><circle cx="10" cy="12" r="6"/><path d="M16 12h5v6h-5M4 12H2M10 6V3h4"/></>,
  gate: <><path d="M5 21V5h14v16M3 5h18M8 9h8v10H8z"/><path d="M12 2v7"/></>,
  back: <><path d="m15 18-6-6 6-6"/><path d="M9 12h12"/></>,
  search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
  reset: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/></>,
  eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/></>,
};

export function Icon({ name, ...props }: { name: IconName } & SVGProps<SVGSVGElement>) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>{paths[name]}</svg>;
}
