import { create } from 'zustand'
import type { RegistroCampo } from '@/types'
import * as fs from '@/lib/firestore'

interface ServicioState {
  records: RegistroCampo[]
  selectedDayMillis: number
  setRecords: (r: RegistroCampo[]) => void
  setSelectedDay: (ms: number) => void
  getMonthRecords: (year: number, month: number) => RegistroCampo[]
  getMonthTotals: (year: number, month: number) => { hours: number; revisits: number; studies: number; publications: number; videos: number; days: number }
}

function startOfDay(ms: number): number {
  const d = new Date(ms)
  d.setHours(0, 0, 0, 0)
  return d.getTime()
}

export const useServicioStore = create<ServicioState>((set, get) => ({
  records: [],
  selectedDayMillis: startOfDay(Date.now()),
  setRecords: (r: RegistroCampo[]) => set({ records: r }),
  setSelectedDay: (ms: number) => set({ selectedDayMillis: startOfDay(ms) }),
  getMonthRecords: (year: number, month: number) => {
    const start = new Date(year, month, 1).getTime()
    const end = new Date(year, month + 1, 0, 23, 59, 59, 999).getTime()
    return get().records.filter((r: RegistroCampo) => r.dateMillis >= start && r.dateMillis <= end)
  },
  getMonthTotals: (year: number, month: number) => {
    const recs = get().getMonthRecords(year, month)
    return {
      hours: recs.reduce((s: number, r: RegistroCampo) => s + r.hours, 0),
      revisits: recs.reduce((s: number, r: RegistroCampo) => s + r.revisits, 0),
      studies: recs.reduce((s: number, r: RegistroCampo) => s + r.studies, 0),
      publications: recs.reduce((s: number, r: RegistroCampo) => s + r.publications, 0),
      videos: recs.reduce((s: number, r: RegistroCampo) => s + r.videos, 0),
      days: recs.length,
    }
  },
}))

export async function upsertRecord(uid: string, data: Omit<RegistroCampo, 'id'>) {
  return fs.upsertRecord(uid, data)
}

export async function deleteRecord(uid: string, id: string) {
  return fs.deleteRecord(uid, id)
}
