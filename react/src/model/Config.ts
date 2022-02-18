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
  portsByRegion: PortRegion[];
  domain: string;
  teamEmail: string;
}

export type Config = PendingConfig | LoadedConfig
