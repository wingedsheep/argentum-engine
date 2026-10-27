import { useState, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseReplacementDecision, OptionMetadata } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { optionPip } from '@/assets/icons/options'
import { DecisionCardPreview } from './DecisionComponents'
import styles from './DecisionUI.module.css'

type Row = { label: string; index: number }

function filterRows(options: readonly string[], filter: string): Row[] {
  const rows = options.map((label, index) => ({ label, index }))
  if (!filter) return rows
  const lower = filter.toLowerCase()
  return rows.filter((r) => r.label.toLowerCase().includes(lower))
}

function PreviewChip({ word, meta, placeholder }: { word: string | null; meta: OptionMetadata | undefined; placeholder: string }) {
  const pipUrl = optionPip(meta?.iconKey)
  if (!word) return <span className={styles.replacementChipEmpty}>{placeholder}</span>
  return (
    <span className={styles.replacementChip}>
      {pipUrl && <img src={pipUrl} alt="" className={styles.optionItemPip} />}
      {word}
    </span>
  )
}

function ReplacementColumn({
  title,
  searchable,
  filter,
  setFilter,
  rows,
  metadata,
  selectedIndex,
  onSelect,
}: {
  title: string
  searchable: boolean
  filter: string
  setFilter: (v: string) => void
  rows: Row[]
  metadata: readonly OptionMetadata[] | undefined
  selectedIndex: number | null
  onSelect: (index: number) => void
}) {
  return (
    <div className={styles.replacementColumn}>
      <h3 className={styles.replacementColumnTitle}>{title}</h3>
      {searchable && (
        <input
          type="text"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Search..."
          className={styles.optionSearchInput}
        />
      )}
      <div className={styles.optionList}>
        {rows.map((row) => {
          const meta = metadata?.[row.index]
          const pipUrl = optionPip(meta?.iconKey)
          return (
            <button
              key={row.index}
              onClick={() => onSelect(row.index)}
              className={`${styles.optionItem} ${selectedIndex === row.index ? styles.optionItemSelected : ''}`}
            >
              {pipUrl && <img src={pipUrl} alt="" className={styles.optionItemPip} />}
              <span>{row.label}</span>
              {meta?.description && <span className={styles.optionTileDescription}> — {meta.description}</span>}
            </button>
          )
        })}
        {rows.length === 0 && <p className={styles.noCardsMessage}>No matching options</p>}
      </div>
    </div>
  )
}

/**
 * Text-change replacement decision (Crystal Spray, Artificial Evolution): pick a FROM word and a
 * TO word in one screen, shown together as "from → to". The TO column is constrained by
 * `allowedToByFrom` once a FROM is chosen (Crystal Spray keeps colors↔colors, lands↔lands).
 */
export function ChooseReplacementDecisionUI({ decision }: { decision: ChooseReplacementDecision }) {
  const submitReplacementDecision = useGameStore((s) => s.submitReplacementDecision)
  const gameState = useGameStore((s) => s.gameState)
  const responsive = useResponsive()

  const [fromIndex, setFromIndex] = useState<number | null>(decision.defaultFromIndex ?? null)
  // A fixed replacement ("... with Vampire") arrives as the only TO option: pre-select it.
  const [toIndex, setToIndex] = useState<number | null>(decision.toOptions.length === 1 ? 0 : null)
  const [fromFilter, setFromFilter] = useState('')
  const [toFilter, setToFilter] = useState('')
  const [minimized, setMinimized] = useState(false)
  const [isHoveringSource, setIsHoveringSource] = useState(false)

  const sourceCard = decision.context.sourceId ? gameState?.cards[decision.context.sourceId] : undefined
  const sourceCardName = decision.context.sourceName ?? sourceCard?.name
  const sourceCardImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  // TO indices allowed for the current FROM. Null => no constraint (all allowed).
  const allowedToIndices = useMemo<readonly number[] | null>(() => {
    if (fromIndex === null) return null
    const constraints = decision.allowedToByFrom
    if (!constraints || constraints.length === 0) return null
    return constraints[fromIndex] ?? []
  }, [fromIndex, decision.allowedToByFrom])

  const fromRows = useMemo(() => filterRows(decision.fromOptions, fromFilter), [decision.fromOptions, fromFilter])
  const toRows = useMemo(() => {
    const rows = filterRows(decision.toOptions, toFilter)
    if (allowedToIndices === null) return rows
    const allow = new Set(allowedToIndices)
    return rows.filter((r) => allow.has(r.index))
  }, [decision.toOptions, toFilter, allowedToIndices])

  const handleSelectFrom = (idx: number) => {
    setFromIndex(idx)
    const constraints = decision.allowedToByFrom
    if (constraints && constraints.length > 0 && toIndex !== null) {
      const allow = constraints[idx] ?? []
      if (!allow.includes(toIndex)) setToIndex(null)
    }
  }

  if (minimized) {
    return (
      <button className={styles.floatingReturnButton} onClick={() => setMinimized(false)}>
        Return to {decision.prompt}
      </button>
    )
  }

  const fromWord = fromIndex !== null ? decision.fromOptions[fromIndex] ?? null : null
  const toWord = toIndex !== null ? decision.toOptions[toIndex] ?? null : null
  const fromMeta = fromIndex !== null ? decision.fromMetadata?.[fromIndex] : undefined
  const toMeta = toIndex !== null ? decision.toMetadata?.[toIndex] : undefined

  return (
    <div className={styles.overlay}>
      {sourceCardImageUrl && (
        <img
          src={sourceCardImageUrl}
          alt={`Source: ${sourceCardName ?? 'card'}`}
          className={styles.bannerCardImage}
          onMouseEnter={() => setIsHoveringSource(true)}
          onMouseLeave={() => setIsHoveringSource(false)}
        />
      )}

      <h2 className={styles.title}>{decision.prompt}</h2>

      <div className={styles.replacementPreview}>
        <PreviewChip word={fromWord} meta={fromMeta} placeholder="Choose a word" />
        <span className={styles.replacementArrow}>→</span>
        <PreviewChip word={toWord} meta={toMeta} placeholder="Choose a replacement" />
      </div>

      <div className={styles.replacementColumns}>
        <ReplacementColumn
          title="Replace"
          searchable={decision.fromOptions.length > 20}
          filter={fromFilter}
          setFilter={setFromFilter}
          rows={fromRows}
          metadata={decision.fromMetadata}
          selectedIndex={fromIndex}
          onSelect={handleSelectFrom}
        />
        <ReplacementColumn
          title="With"
          searchable={decision.toOptions.length > 20}
          filter={toFilter}
          setFilter={setToFilter}
          rows={toRows}
          metadata={decision.toMetadata}
          selectedIndex={toIndex}
          onSelect={setToIndex}
        />
      </div>

      {isHoveringSource && sourceCardName && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCardName} imageUri={sourceCard?.imageUri} />
      )}

      <div className={styles.optionButtonRow}>
        <button onClick={() => setMinimized(true)} className={styles.viewBattlefieldButton}>
          View Battlefield
        </button>
        <button
          onClick={() => {
            if (fromIndex !== null && toIndex !== null) submitReplacementDecision(decision.id, fromIndex, toIndex)
          }}
          disabled={fromIndex === null || toIndex === null}
          className={styles.confirmButton}
        >
          Confirm
        </button>
      </div>
    </div>
  )
}
