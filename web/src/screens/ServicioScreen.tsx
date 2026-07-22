import { useState, useMemo } from 'react'
import { ChevronLeft, ChevronRight, Users, BookOpen, FileText } from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { useServicioStore, upsertRecord } from '@/stores/servicioStore'
import clsx from 'clsx'

const MONTH_NAMES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']
const DAY_LABELS = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa', 'Do']

function startOfDay(ms: number) {
  const d = new Date(ms)
  d.setHours(0, 0, 0, 0)
  return d.getTime()
}

function buildMonthCells(year: number, month: number) {
  const first = new Date(year, month, 1)
  const days = new Date(year, month + 1, 0).getDate()
  const leading = (first.getDay() + 6) % 7
  const cells: Array<{ millis: number; day: number } | null> = []
  for (let i = 0; i < leading; i++) cells.push(null)
  for (let d = 1; d <= days; d++) {
    cells.push({ millis: startOfDay(new Date(year, month, d).getTime()), day: d })
  }
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
}

export function ServicioScreen() {
  const user = useAuthStore(s => s.user)
  const records = useServicioStore(s => s.records)
  const selectedDayMillis = useServicioStore(s => s.selectedDayMillis)
  const setSelectedDay = useServicioStore(s => s.setSelectedDay)
  const getMonthRecords = useServicioStore(s => s.getMonthRecords)
  const getMonthTotals = useServicioStore(s => s.getMonthTotals)
  const [showForm, setShowForm] = useState(false)

  const now = new Date()
  const [viewYear, setViewYear] = useState(now.getFullYear())
  const [viewMonth, setViewMonth] = useState(now.getMonth())

  const cells = useMemo(() => buildMonthCells(viewYear, viewMonth), [viewYear, viewMonth])
  const monthRecords = useMemo(() => getMonthRecords(viewYear, viewMonth), [records, viewYear, viewMonth])
  const totals = useMemo(() => getMonthTotals(viewYear, viewMonth), [records, viewYear, viewMonth])
  const recordedDays = useMemo(() => new Set(monthRecords.map(r => r.dateMillis)), [monthRecords])

  const selectedRecord = useMemo(
    () => records.find(r => r.dateMillis === selectedDayMillis),
    [records, selectedDayMillis],
  )

  const prevMonth = () => {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1) }
    else setViewMonth(m => m - 1)
  }
  const nextMonth = () => {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1) }
    else setViewMonth(m => m + 1)
  }

  const handleSaveRecord = async (data: { hours: number; revisits: number; studies: number; notes: string }) => {
    if (!user) return
    await upsertRecord(user.uid, {
      dateMillis: selectedDayMillis,
      hours: data.hours,
      revisits: data.revisits,
      studies: data.studies,
      publications: 0,
      videos: 0,
      notes: data.notes,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      userId: user.uid,
    })
    setShowForm(false)
  }

  return (
    <div className="max-w-lg mx-auto px-5 pt-14 pb-4">
      <h1 className="text-3xl font-bold text-on-surface tracking-tight mb-1">
        Servicio del Campo
      </h1>
      <p className="text-sm text-on-surface-muted mb-6 flex items-center gap-2">
        <span className="w-6 h-0.5 bg-accent rounded-full inline-block" />
        Predicación · revisitas · cursos
      </p>

      {/* Calendar Card */}
      <div className="bg-bg-card rounded-2xl p-4 mb-6">
        <div className="flex items-center justify-between mb-4">
          <button onClick={prevMonth} className="w-8 h-8 rounded-full bg-surface-hi flex items-center justify-center text-on-surface-muted hover:text-on-surface transition-colors">
            <ChevronLeft size={16} />
          </button>
          <div className="flex items-center gap-1.5">
            <span className="bg-surface-hi px-3 py-1 rounded-lg text-sm font-semibold text-on-surface">{MONTH_NAMES[viewMonth]}</span>
            <span className="bg-surface-hi px-3 py-1 rounded-lg text-sm font-semibold text-on-surface">{viewYear}</span>
          </div>
          <button onClick={nextMonth} className="w-8 h-8 rounded-full bg-surface-hi flex items-center justify-center text-on-surface-muted hover:text-on-surface transition-colors">
            <ChevronRight size={16} />
          </button>
        </div>

        <div className="grid grid-cols-7 gap-1 mb-1">
          {DAY_LABELS.map((d, i) => (
            <div key={d} className={clsx('text-center text-[11px] font-medium py-1', i >= 5 ? 'text-accent-sub/40' : 'text-on-surface-faint')}>
              {d}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-7 gap-1">
          {cells.map((cell, i) => (
            <div key={i} className="aspect-square flex items-center justify-center">
              {cell && (
                <button
                  onClick={() => { setSelectedDay(cell.millis); setShowForm(true) }}
                  className={clsx(
                    'w-full h-full rounded-xl flex flex-col items-center justify-center text-sm transition-all',
                    cell.millis === selectedDayMillis
                      ? 'bg-accent text-white font-bold'
                      : cell.millis === startOfDay(Date.now())
                      ? 'bg-accent/15 text-accent font-bold'
                      : 'bg-surface-hi text-on-surface/70 hover:bg-surface-hi/80',
                  )}
                >
                  {cell.day}
                  {recordedDays.has(cell.millis) && cell.millis !== selectedDayMillis && (
                    <div className="w-1 h-0.5 bg-accent-sub rounded-full mt-0.5" />
                  )}
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Summary Card */}
      <div className="bg-gradient-to-b from-white/5 to-transparent rounded-3xl p-5 border border-outline-faint mb-6 relative overflow-hidden">
        <div className="absolute -top-16 -right-16 w-32 h-32 bg-accent/10 rounded-full blur-2xl" />
        <p className="text-xs text-on-surface-muted mb-1 relative">Resumen del mes</p>
        <div className="flex items-baseline gap-2 mb-5 relative">
          <span className="text-4xl font-bold text-on-surface">{totals.hours.toFixed(1)}</span>
          <span className="text-sm text-on-surface-faint">horas</span>
        </div>
        <div className="space-y-2 relative">
          <div className="flex items-center justify-between p-3 rounded-xl bg-surface/50 border border-outline-faint">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-accent/10 flex items-center justify-center">
                <Users size={16} className="text-accent-sub" />
              </div>
              <span className="text-sm text-on-surface">Revisitas</span>
            </div>
            <span className="text-lg font-bold text-accent-sub">{totals.revisits}</span>
          </div>
          <div className="flex items-center justify-between p-3 rounded-xl bg-surface/50 border border-outline-faint">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-accent/10 flex items-center justify-center">
                <BookOpen size={16} className="text-accent-sub" />
              </div>
              <span className="text-sm text-on-surface">Cursos bíblicos</span>
            </div>
            <span className="text-lg font-bold text-accent-sub">{totals.studies}</span>
          </div>
        </div>
        <button className="w-full mt-4 py-3 rounded-2xl bg-gradient-to-r from-accent/80 to-accent-deep text-white font-medium text-sm flex items-center justify-center gap-2 hover:opacity-90 active:scale-[0.98] transition-all relative">
          <FileText size={16} />
          Exportar mes a PDF
        </button>
      </div>

      {/* Record Form Modal */}
      {showForm && (
        <RecordForm
          dateMillis={selectedDayMillis}
          existing={selectedRecord}
          onSave={handleSaveRecord}
          onClose={() => setShowForm(false)}
        />
      )}
    </div>
  )
}

function RecordForm({
  dateMillis,
  existing,
  onSave,
  onClose,
}: {
  dateMillis: number
  existing?: { hours: number; revisits: number; studies: number; notes: string }
  onSave: (data: { hours: number; revisits: number; studies: number; notes: string }) => void
  onClose: () => void
}) {
  const [hours, setHours] = useState(existing?.hours?.toString() || '')
  const [revisits, setRevisits] = useState(existing?.revisits?.toString() || '0')
  const [studies, setStudies] = useState(existing?.studies?.toString() || '0')
  const [notes, setNotes] = useState(existing?.notes || '')

  const dateStr = new Date(dateMillis).toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' })

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div className="w-full max-w-lg bg-bg-card rounded-t-3xl sm:rounded-3xl p-6 space-y-4" onClick={e => e.stopPropagation()}>
        <h3 className="text-lg font-bold text-on-surface capitalize">{dateStr}</h3>

        <div>
          <label className="text-xs text-on-surface-muted mb-1 block">Horas</label>
          <input type="number" step="0.5" value={hours} onChange={e => setHours(e.target.value)}
            className="w-full px-3 py-2.5 rounded-xl bg-surface border border-outline-faint text-sm text-on-surface focus:outline-none focus:border-accent/40" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-on-surface-muted mb-1 block">Revisitas</label>
            <input type="number" value={revisits} onChange={e => setRevisits(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-surface border border-outline-faint text-sm text-on-surface focus:outline-none focus:border-accent/40" />
          </div>
          <div>
            <label className="text-xs text-on-surface-muted mb-1 block">Cursos</label>
            <input type="number" value={studies} onChange={e => setStudies(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-surface border border-outline-faint text-sm text-on-surface focus:outline-none focus:border-accent/40" />
          </div>
        </div>
        <div>
          <label className="text-xs text-on-surface-muted mb-1 block">Notas</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={3}
            className="w-full px-3 py-2.5 rounded-xl bg-surface border border-outline-faint text-sm text-on-surface focus:outline-none focus:border-accent/40 resize-none" />
        </div>

        <div className="flex gap-3 pt-2">
          <button onClick={onClose} className="flex-1 py-3 rounded-xl bg-surface-hi text-on-surface-muted text-sm font-medium hover:bg-surface transition-colors">
            Cancelar
          </button>
          <button
            onClick={() => onSave({ hours: parseFloat(hours) || 0, revisits: parseInt(revisits) || 0, studies: parseInt(studies) || 0, notes })}
            className="flex-1 py-3 rounded-xl bg-accent text-white text-sm font-medium hover:opacity-90 transition-opacity"
          >
            Guardar
          </button>
        </div>
      </div>
    </div>
  )
}
