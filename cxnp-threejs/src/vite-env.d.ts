/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_CXNP_WEB_AMAP?: string;
  readonly VITE_AMAP_SECURITY_JS_CODE?: string;
  readonly VITE_MAPBOX_TOKEN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
