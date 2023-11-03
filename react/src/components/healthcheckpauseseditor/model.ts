export type PortCode = {
  iata: string
}

export type ScheduledHealthCheckPause = {
  startsAt: number
  endsAt: number
  ports: PortCode[]
  createdAt: number
}

