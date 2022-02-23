export type PendingConfig = {
  kind: "PendingConfig"
}

export type LoadedConfig = {
  kind: "LoadedConfig"
  values: ConfigValues
}

export type PortRegion = {
  name: string
  ports: string[]
}

export class PortRegionHelper {
  public static portsInRegions(regions: PortRegion[]): string[] {
     return regions.map(r => r.ports).reduce((a, b) => a.concat(b))
  }
}

export type ConfigValues = {
  portsByRegion: PortRegion[];
  domain: string;
  teamEmail: string;
}

export type Config = PendingConfig | LoadedConfig
