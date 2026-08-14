/**
 * Undo/redo for the scenario builder. Building a board is fiddly — a mis-dropped card or an
 * accidental "clear seat" shouldn't cost the tester their setup — and every builder mutation is
 * already a pure `BuilderState -> BuilderState`, so a plain past/present/future stack is enough.
 *
 * `reset` replaces the state *and* clears the history: loading a share link, a file, or pasted
 * JSON starts a new editing session rather than something you can undo back out of.
 */
import { useCallback, useMemo, useState } from 'react'

const HISTORY_LIMIT = 50

interface History<T> {
  past: T[]
  present: T
  future: T[]
}

export function useUndoable<T>(initial: T | (() => T)) {
  const [history, setHistory] = useState<History<T>>(() => ({
    past: [],
    present: typeof initial === 'function' ? (initial as () => T)() : initial,
    future: [],
  }))

  const commit = useCallback((updater: T | ((prev: T) => T)) => {
    setHistory((h) => {
      const next =
        typeof updater === 'function' ? (updater as (prev: T) => T)(h.present) : updater
      if (next === h.present) return h
      return {
        past: [...h.past, h.present].slice(-HISTORY_LIMIT),
        present: next,
        future: [],
      }
    })
  }, [])

  const reset = useCallback((next: T) => {
    setHistory({ past: [], present: next, future: [] })
  }, [])

  const undo = useCallback(() => {
    setHistory((h) => {
      const previous = h.past[h.past.length - 1]
      if (previous === undefined) return h
      return { past: h.past.slice(0, -1), present: previous, future: [h.present, ...h.future] }
    })
  }, [])

  const redo = useCallback(() => {
    setHistory((h) => {
      const [next, ...rest] = h.future
      if (next === undefined) return h
      return { past: [...h.past, h.present], present: next, future: rest }
    })
  }, [])

  return useMemo(
    () => ({
      state: history.present,
      commit,
      reset,
      undo,
      redo,
      canUndo: history.past.length > 0,
      canRedo: history.future.length > 0,
    }),
    [history, commit, reset, undo, redo],
  )
}
