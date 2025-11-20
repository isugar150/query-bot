export type AuthResponse = {
  username: string
  accessToken: string
  refreshToken: string
}

export type InitStatusResponse = {
  initialized: boolean
}

export type InitSetupResponse = {
  auth: AuthResponse
  database?: DbSummary | null
}

export type DatabaseType = 'MYSQL' | 'MARIADB' | 'POSTGRESQL'

export type DbConnectionRequest = {
  name: string
  dbType: DatabaseType
  host: string
  port?: number
  databaseName: string
  username: string
  password: string
}

export type DbSummary = {
  id: number
  name: string
  dbType: DatabaseType
  host: string
  port: number
  databaseName: string
  schemaReady: boolean
}

export type ColumnOverview = {
  name: string
  type: string
  nullable: boolean
}

export type TableOverview = {
  name: string
  columns: ColumnOverview[]
}

export type SchemaOverview = {
  database: string
  tables: TableOverview[]
}

export type DbTestResponse = {
  success: boolean
  message: string
  schema?: SchemaOverview
}

export type ChatMessage = {
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
}

export type ChatResponse = {
  sessionId: number
  reply: string
  history: ChatMessage[]
}
