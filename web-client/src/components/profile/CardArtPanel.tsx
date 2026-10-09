/**
 * The picker's "Card art" tab: search the catalog by name, pick a card (and, when it has several, the
 * printing whose art you like), then frame the part of the illustration you want with the crop editor.
 * Reports each change as a ready-to-save avatar value.
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { pageStyles as p } from '@/components/ui/PageShell'
import { type CardCrop, cardArtPath, cardArtUrl, formatCardAvatar } from './avatars'
import { CropEditor } from './CropEditor'
import s from './CardArtPanel.module.css'

interface CardHit {
  readonly name: string
  readonly imageUri?: string | null
  readonly setCode?: string | null
}

interface PrintingHit {
  readonly setCode: string
  readonly setName: string | null
  readonly imageUri: string | null
  readonly artist: string | null
}

/** A printing with drawable art: its image path, and what to call it on the chip. */
interface ArtChoice {
  readonly path: string
  readonly label: string
  readonly title: string
}

/** The card being framed. `name` is unknown when re-opening an avatar saved in an earlier visit. */
export interface CardArtSelection {
  readonly name: string | null
  readonly path: string
  readonly crop: CardCrop | null
}

const RESULT_LIMIT = 24
/** Shown before anything is typed: art-forward cards with a clear subject. */
const SUGGESTION_QUERY = 't:legendary t:creature r:mythic'

const searchCache = new Map<string, Promise<CardHit[]>>()
function searchCards(q: string): Promise<CardHit[]> {
  let hit = searchCache.get(q)
  if (!hit) {
    hit = fetch(`/api/cards/search?q=${encodeURIComponent(q)}`)
      .then((r) => (r.ok ? r.json() : { cards: [] }))
      .then((body: { cards?: CardHit[] }) => (body.cards ?? []).filter((c) => cardArtPath(c.imageUri)))
      .catch(() => [])
    searchCache.set(q, hit)
  }
  return hit
}

function fetchArtChoices(name: string): Promise<ArtChoice[]> {
  return fetch(`/api/cards/${encodeURIComponent(name)}/printings`)
    .then((r) => (r.ok ? r.json() : []))
    .then((printings: PrintingHit[]) => {
      const seen = new Set<string>()
      const choices: ArtChoice[] = []
      for (const pr of printings) {
        const path = cardArtPath(pr.imageUri)
        if (!path || seen.has(path)) continue
        seen.add(path)
        choices.push({
          path,
          label: pr.setCode.toUpperCase(),
          title: [pr.setName, pr.artist && `art by ${pr.artist}`].filter(Boolean).join(' — '),
        })
      }
      return choices
    })
    .catch(() => [])
}

export function CardArtPanel({
  selection,
  onSelect,
}: {
  selection: CardArtSelection | null
  onSelect: (selection: CardArtSelection | null) => void
}) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<CardHit[] | null>(null)
  const [choices, setChoices] = useState<ArtChoice[]>([])
  const inputRef = useRef<HTMLInputElement>(null)

  // Debounced search; an empty box shows the suggestions.
  useEffect(() => {
    if (selection) return
    const q = query.trim()
    let cancelled = false
    const timer = setTimeout(
      () => {
        void searchCards(q ? q : SUGGESTION_QUERY).then((cards) => {
          // Suggestions come back alphabetical; a shuffle shows a spread of the catalog instead.
          if (!cancelled) setResults((q ? cards : shuffled(cards)).slice(0, RESULT_LIMIT))
        })
      },
      q ? 220 : 0,
    )
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query, selection])

  const selectedName = selection?.name ?? null
  useEffect(() => {
    setChoices([])
    if (!selectedName) return
    let cancelled = false
    void fetchArtChoices(selectedName).then((c) => {
      if (!cancelled) setChoices(c)
    })
    return () => {
      cancelled = true
    }
  }, [selectedName])

  const pick = (card: CardHit) => {
    const path = cardArtPath(card.imageUri)
    if (path) onSelect({ name: card.name, path, crop: null })
  }

  const backToSearch = () => {
    onSelect(null)
    requestAnimationFrame(() => inputRef.current?.focus())
  }

  const alt = useMemo(() => (selection?.name ? `${selection.name} art` : 'Card art'), [selection?.name])

  if (selection) {
    return (
      <div className={s.editor}>
        <div className={s.editorHead}>
          <button type="button" className={s.back} onClick={backToSearch}>
            <span aria-hidden>←</span> {selection.name ? 'Choose another card' : 'Choose a card'}
          </button>
          {selection.name && <span className={s.cardName}>{selection.name}</span>}
        </div>
        {choices.length > 1 && (
          <div className={s.printings} role="radiogroup" aria-label="Printing">
            {choices.map((c) => (
              <button
                key={c.path}
                type="button"
                role="radio"
                aria-checked={c.path === selection.path}
                className={s.printing}
                title={c.title}
                onClick={() => onSelect({ ...selection, path: c.path })}
              >
                <img src={cardArtUrl(c.path)} alt="" className={s.printingThumb} loading="lazy" />
                {c.label}
              </button>
            ))}
          </div>
        )}
        <CropEditor
          key={selection.path}
          src={cardArtUrl(selection.path)}
          alt={alt}
          crop={selection.crop}
          onChange={(crop) => onSelect({ ...selection, crop })}
        />
      </div>
    )
  }

  return (
    <div className={s.search}>
      <input
        ref={inputRef}
        className={`${p.input} ${s.input}`}
        placeholder="Search for a card by name…"
        value={query}
        autoFocus
        aria-label="Search cards"
        onChange={(e) => setQuery(e.target.value)}
        data-testid="avatar-card-search"
      />
      <p className={s.hint}>{query.trim() ? 'Pick a card to frame its art.' : 'A few to start with — or search for any card.'}</p>
      {results === null ? (
        <div className={s.empty}>Searching…</div>
      ) : results.length === 0 ? (
        <div className={s.empty}>No cards match “{query.trim()}”.</div>
      ) : (
        <div className={s.results}>
          {results.map((card) => (
            <button
              key={card.name}
              type="button"
              className={s.result}
              onClick={() => pick(card)}
              title={card.name}
              data-testid={`avatar-card-${card.name}`}
            >
              <img src={cardArtUrl(cardArtPath(card.imageUri)!)} alt="" className={s.resultArt} loading="lazy" />
              <span className={s.resultName}>{card.name}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

const suggestionOrder = new Map<string, number>()
function shuffled<T extends { name: string }>(cards: readonly T[]): T[] {
  // Stable for the page's lifetime, so reopening the picker doesn't reshuffle under the cursor.
  for (const c of cards) if (!suggestionOrder.has(c.name)) suggestionOrder.set(c.name, Math.random())
  return [...cards].sort((a, b) => suggestionOrder.get(a.name)! - suggestionOrder.get(b.name)!)
}

export function cardSelectionValue(selection: CardArtSelection | null): string | null {
  return selection?.crop ? formatCardAvatar(selection.path, selection.crop) : null
}
