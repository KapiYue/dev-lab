# 城西南排水数字孪生监控平台

纯前端演示项目，分别使用高德地图、Three.js 与 Mapbox GL JS 表达城市定位、设备级三维场景和工程空间分析。三种能力职责独立，不共用或伪装底图。

## 启动

```bash
npm install
cp .env.example .env.local
npm run dev
```

在 `.env.local` 中填写自己的浏览器端密钥：

```env
VITE_CXNP_WEB_AMAP=高德 Web JS API Key
VITE_AMAP_SECURITY_JS_CODE=可选的高德安全密钥
VITE_MAPBOX_TOKEN=Mapbox public token
```

不要提交 `.env.local`。若密钥缺失、地图资源加载失败或 WebGL 不可用，页面会展示可恢复的空状态，不会白屏。

## 页面职责

- `/`：高德城市监控。使用 GCJ-02 点位，提供设施筛选、告警、实时指标和模拟控制。
- `/facility/:id`：独立全屏 Three.js 设备场景。加载闸门、水泵、井盖 GLTF，提供动画、热点、视角控制与模型失败回退。
- `/overview`：Mapbox 卫星工程概览。使用转换后的 WGS84 GeoJSON，并通过 Mapbox custom layer 在共享 WebGL 上下文中叠加 GLTF 重点模型。

## 坐标流程

权威输入坐标保存在 `src/data/sites.ts`，统一为 `[lng, lat]` GCJ-02。高德直接使用 GCJ-02；`src/utils/coordinateTransform.ts` 将点位和覆盖区逐点转换为 WGS84 后再传入 Mapbox/GeoJSON。转换单测位于 `src/utils/coordinateTransform.test.ts`。

```bash
npm test
```

## 替换 mock 数据

- 点位与坐标：`src/data/sites.ts`
- 监测指标、告警与趋势：`src/data/monitoring.mock.ts`
- 工程覆盖区与关联线：`src/data/overview-zones.mock.ts`
- 异步边界：`src/services/monitoringService.ts`、`src/services/siteService.ts`

未来接入 REST 或 WebSocket 时，保留 service 方法签名并替换其数据源即可。演示数据使用固定基准和正弦序列，可复现、可调试。

## 替换三维模型

当前三份可加载的 GLTF 位于 `public/models/`，生成源为 `scripts/generate-models.mjs`：

```bash
npm run generate:models
```

真实模型可替换同名文件，或同步修改 `src/features/facility-scene/FacilityTwinScene.tsx` 与 `src/features/engineering-overview/MapboxOverview.tsx` 的资源路径。若要保留动画，请沿用 `GateBoard`、`Rotor`、`ManholeLid` 节点名，或更新节点查询逻辑。

## 替换工程边界

`src/data/overview-zones.mock.ts` 中的边界以 GCJ-02 定义，并在导出 GeoJSON 时逐点转换为 WGS84。替换时应继续保留这一流程；当拿到正式边界数据后，同时删除界面中的“演示范围，待工程边界数据校准”提示。

## 交付检查

```bash
npm run typecheck
npm test
npm run build
```
