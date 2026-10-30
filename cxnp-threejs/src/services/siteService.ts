import { drainageSites } from "../data/sites";
import type { DrainageFacility } from "../types/domain";

const delay = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

export const siteService = {
  async list(): Promise<DrainageFacility[]> {
    await delay(140);
    return structuredClone(drainageSites);
  },
  async get(id: string): Promise<DrainageFacility | undefined> {
    await delay(90);
    const site = drainageSites.find((item) => item.id === id);
    return site ? structuredClone(site) : undefined;
  },
};
