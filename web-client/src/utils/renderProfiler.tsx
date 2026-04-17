import { Profiler, type ProfilerOnRenderCallback, type ReactNode } from 'react'

type Stats = {
  commits: number
  totalActual: number
  totalBase: number
  maxActual: number
  lastActual: number
}

const stats = new Map<string, Stats>()

function isEnabled(): boolean {
  if (typeof window === 'undefined') return false
  const w = window as unknown as { __profile?: boolean }
  if (w.__profile) return true
  try {
    return new URLSearchParams(window.location.search).get('profile') === '1'
  } catch {
    return false
  }
}

const onRender: ProfilerOnRenderCallback = (id, _phase, actualDuration, baseDuration) => {
  const s = stats.get(id) ?? { commits: 0, totalActual: 0, totalBase: 0, maxActual: 0, lastActual: 0 }
  s.commits += 1
  s.totalActual += actualDuration
  s.totalBase += baseDuration
  s.lastActual = actualDuration
  if (actualDuration > s.maxActual) s.maxActual = actualDuration
  stats.set(id, s)
}

export function RenderProfiler({ id, children }: { id: string; children: ReactNode }) {
  if (!isEnabled()) return <>{children}</>
  return (
    <Profiler id={id} onRender={onRender}>
      {children}
    </Profiler>
  )
}

function report() {
  const rows: Array<{ id: string; commits: number; totalMs: number; avgMs: number; maxMs: number; lastMs: number }> = []
  for (const [id, s] of stats) {
    rows.push({
      id,
      commits: s.commits,
      totalMs: +s.totalActual.toFixed(2),
      avgMs: +(s.totalActual / s.commits).toFixed(2),
      maxMs: +s.maxActual.toFixed(2),
      lastMs: +s.lastActual.toFixed(2),
    })
  }
  rows.sort((a, b) => b.totalMs - a.totalMs)
  // eslint-disable-next-line no-console
  console.table(rows)
  return rows
}

function reset() {
  stats.clear()
  // eslint-disable-next-line no-console
  console.log('[profiler] reset')
}

if (typeof window !== 'undefined') {
  const w = window as unknown as {
    __profileReport?: () => unknown
    __profileReset?: () => void
    __profileEnable?: () => void
    __profileDisable?: () => void
  }
  w.__profileReport = report
  w.__profileReset = reset
  w.__profileEnable = () => {
    ;(window as unknown as { __profile?: boolean }).__profile = true
    // eslint-disable-next-line no-console
    console.log('[profiler] enabled — reload the page for wrappers to activate')
  }
  w.__profileDisable = () => {
    ;(window as unknown as { __profile?: boolean }).__profile = false
    // eslint-disable-next-line no-console
    console.log('[profiler] disabled — reload the page to deactivate wrappers')
  }
}
