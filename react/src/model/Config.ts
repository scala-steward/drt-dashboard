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

export type ConfigValues = {
  regions: PortRegion[];
  ports: string[];
  domain: string;
  teamEmail: string;
}

export type Config = PendingConfig | LoadedConfig
