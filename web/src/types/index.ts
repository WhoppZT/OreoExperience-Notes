export interface Discurso {
  id: string
  title: string
  scriptures: string
  tags: string
  notes: string
  createdAt: number
  updatedAt: number
  targetDurationSec: number
  pinned: boolean
  deletedAt: number | null
  category: NoteCategory
  userId: string
}

export interface RegistroCampo {
  id: string
  dateMillis: number
  hours: number
  revisits: number
  publications: number
  videos: number
  studies: number
  notes: string
  createdAt: number
  updatedAt: number
  userId: string
}

export type NoteCategory = 'discurso' | 'consideracion' | 'servicio_campo' | 'general'

export const CATEGORIES: Record<NoteCategory, { label: string; short: string }> = {
  discurso: { label: 'Discursos', short: 'Discurso' },
  consideracion: { label: 'Consideraciones', short: 'Consideracion' },
  servicio_campo: { label: 'Servicio del Campo', short: 'Servicio' },
  general: { label: 'General', short: 'Nota' },
}

export type SortBy = 'updated' | 'created' | 'title'

export type ThemeMode = 'system' | 'light' | 'dark'

export interface UserPreferences {
  autoSave: boolean
  themeMode: ThemeMode
  sortBy: SortBy
  pdfDensity: 'compact' | 'normal' | 'loose'
  pdfMargin: 'tight' | 'normal' | 'wide'
  pdfIncludeCover: boolean
  pdfWatermark: boolean
  aiEnabled: boolean
  aiApiKey: string
}
