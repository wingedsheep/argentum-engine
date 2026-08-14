/**
 * Card catalogue + set list loader.
 *
 * `/api/cards` is the full implemented-card catalogue (one `CardSummary` per card name);
 * `/api/sets` is every set the server knows about (code, display name, ISO release date).
 * Both are static for the session, so callers just mount this hook once.
 */
import { useEffect, useState } from 'react'
import type { CardSummary } from '../cardFilter'

export type SetInfo = { code: string; name: string; releaseDate: string | null }

export interface CardCatalog {
  catalog: CardSummary[]
  setInfos: SetInfo[]
  /** Card name → summary. */
  index: Record<string, CardSummary>
  loading: boolean
  /** Non-null when `/api/cards` failed — the browser renders the message instead of an empty grid. */
  error: string | null
}

export function useCardCatalog(): CardCatalog {
  const [catalog, setCatalog] = useState<CardSummary[]>([])
  const [setInfos, setSetInfos] = useState<SetInfo[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    fetch('/api/cards')
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(`HTTP ${r.status}`))))
      .then((list: CardSummary[]) => {
        if (!cancelled) {
          setCatalog(list)
          setLoading(false)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to load cards')
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    fetch('/api/sets')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: SetInfo[]) => {
        if (!cancelled) setSetInfos(list)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const [index, setIndex] = useState<Record<string, CardSummary>>({})
  useEffect(() => {
    const out: Record<string, CardSummary> = {}
    for (const c of catalog) out[c.name] = c
    setIndex(out)
  }, [catalog])

  return { catalog, setInfos, index, loading, error }
}
