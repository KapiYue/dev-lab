export const cxnp_web_amap = import.meta.env.VITE_CXNP_WEB_AMAP ?? "";
export const mapbox_token = import.meta.env.VITE_MAPBOX_TOKEN ?? "";
export const amapSecurityJsCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE ?? "";

export const cityMapConfig = {
  center: [120.056338, 30.235655] as const,
  zoom: 12,
};

export const mapboxConfig = {
  style: "mapbox://styles/mapbox/satellite-streets-v12",
  pitch: 60,
  bearing: -24,
  zoom: 13.7,
};
