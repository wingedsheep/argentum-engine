/**
 * Drag payload shared by every surface that moves a card around: the catalog grid, the deck
 * list, the sideboard, and the scenario builder's board zones. We carry both a custom MIME (so
 * a drop zone can tell where the drag started) and `text/plain` (so the drag still has a sane
 * fallback outside the app).
 *
 * `source` lets a drop zone treat a card dragged out of another zone as a *move* (remove one
 * there, add one here) versus a catalog drag as a plain add — in the deckbuilder the combined
 * 4-of cap (CR 100.2a) spans deck + sideboard, so a plain add could otherwise be a silent
 * no-op; in the scenario builder a move between zones must not duplicate the card.
 */
import { useState } from 'react'

const CARD_DRAG_MIME = 'application/x-argentum-card'

/**
 * Where a drag started. `catalog` is the search grid (an add). `deck` / `sideboard` are the
 * deckbuilder's own lists. `zone` is a scenario-builder board zone — the payload's `zoneRef`
 * says which one.
 */
export type CardDragSource = 'catalog' | 'deck' | 'sideboard' | 'zone'

export interface CardDragPayload {
  name: string
  source: CardDragSource
  /**
   * Opaque origin tag for `zone` drags — the scenario builder encodes `seat:zone:index` so a
   * drop can remove the card from where it came. Ignored by the deckbuilder.
   */
  zoneRef?: string
}

export function setCardDragData(
  e: React.DragEvent,
  name: string,
  source: CardDragSource,
  zoneRef?: string,
) {
  const payload: CardDragPayload = zoneRef ? { name, source, zoneRef } : { name, source }
  e.dataTransfer.setData(CARD_DRAG_MIME, JSON.stringify(payload))
  e.dataTransfer.setData('text/plain', name)
  e.dataTransfer.effectAllowed = 'copyMove'
}

export function readCardDragData(e: React.DragEvent): CardDragPayload | null {
  const raw = e.dataTransfer.getData(CARD_DRAG_MIME)
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as CardDragPayload
      if (parsed.name) return parsed
    } catch {
      // fall through to the text/plain fallback
    }
  }
  const name = e.dataTransfer.getData('text/plain')
  return name ? { name, source: 'catalog' } : null
}

/**
 * Shared drop-zone wiring. Tracks a drag-over highlight and invokes `onDrop` with the decoded
 * payload. The `onDragLeave` guard only clears the highlight when the pointer truly leaves the
 * zone, not when it crosses between child elements.
 */
export function useCardDropZone(onDrop: (payload: CardDragPayload) => void) {
  const [dragActive, setDragActive] = useState(false)
  const dropHandlers = {
    onDragOver: (e: React.DragEvent) => {
      e.preventDefault()
      e.dataTransfer.dropEffect = 'copy'
      if (!dragActive) setDragActive(true)
    },
    onDragLeave: (e: React.DragEvent) => {
      if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setDragActive(false)
    },
    onDrop: (e: React.DragEvent) => {
      e.preventDefault()
      setDragActive(false)
      const payload = readCardDragData(e)
      if (payload) onDrop(payload)
    },
  }
  return { dragActive, dropHandlers }
}
