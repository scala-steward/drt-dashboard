
export type PendingConfig = {
    kind: "PendingConfig"
}

export type LoadedConfig = {
    kind: "LoadedConfig"
    values: ConfigValues
}

export type ConfigValues = {
    ports: string[];
    domain: string;
    teamEmail: string;
}

export type Config = PendingConfig | LoadedConfig
