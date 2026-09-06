import { createContext } from 'react'
import type { Role, User } from '../types'

export type AuthContextValue = {
  user: User | null; loading: boolean; login: (email: string, password: string) => Promise<void>; logout: () => Promise<void>
  can: (...roles: Role[]) => boolean
}
export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
