/**
 * The editable board: one panel per seat, each zone a drop target holding real card images.
 *
 * Everything here is presentational — the page owns the {@link BuilderState} and passes down
 * callbacks. Cards can be dragged in from the card browser (payload source `catalog`) or moved
 * between zones (source `zone`, with a `seatId|zone|index` ref so the origin can be emptied).
 */
import { useCallback, useMemo, useState } from 'react'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import {
  HoverFollowPreview,
  setCardDragData,
  useCardDropZone,
  type CardDragPayload,
} from '@/components/deckbuilder/browser'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'
import { getCardImageUrl, landscapeImageRotateDeg } from '@/utils/cardImages'
import type { BuilderSeat, BuilderState } from './builderState'
import { PILE_ZONES, SCENARIO_ZONES, ZONE_LABEL, type ScenarioBattlefieldCard, type ScenarioZone } from './types'
import styles from './ScenarioBuilder.module.css'

export interface BoardTarget {
  seatId: string
  zone: ScenarioZone
}

export interface ScenarioBoardProps {
  state: BuilderState
  /** Card name → catalogue entry, for art and type lines. */
  index: Record<string, CardSummary>
  target: BoardTarget
  onTargetChange: (target: BoardTarget) => void
  onSeatPatch: (seatId: string, patch: Partial<Pick<BuilderSeat, 'name' | 'life'>>) => void
  onDropCard: (payload: CardDragPayload, seatId: string, zone: ScenarioZone) => void
  onRemove: (seatId: string, zone: ScenarioZone, index: number) => void
  onEditCard: (seatId: string, index: number) => void
  onQuickPatch: (seatId: string, index: number, patch: Partial<ScenarioBattlefieldCard>) => void
  onClearZone: (seatId: string, zone: ScenarioZone) => void
  onClearSeat: (seatId: string) => void
  onRemoveSeat: (seatId: string) => void
}

export function ScenarioBoard(props: ScenarioBoardProps) {
  const { state, index } = props
  const [hoverName, setHoverName] = useState<string | null>(null)
  const hoverCard = hoverName ? index[hoverName] : undefined
  const dfc = useDfcHoverFlip(
    hoverCard
      ? {
          name: hoverCard.name,
          imageUri: hoverCard.imageUri ?? null,
          isDoubleFaced: hoverCard.isDoubleFaced ?? false,
          backFaceName: hoverCard.backFaceName ?? null,
          backFaceImageUri: hoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetFlip = dfc.resetFlip
  const handleHover = useCallback(
    (name: string | null) => {
      setHoverName((prev) => {
        if (prev !== name) resetFlip()
        return name
      })
    },
    [resetFlip],
  )

  return (
    <div className={styles.seats}>
      {state.seats.map((seat, i) => (
        <SeatPanel
          key={seat.id}
          seat={seat}
          seatNumber={i + 1}
          isActivePlayer={state.activePlayer === i + 1}
          canRemove={state.seats.length > 2}
          onHover={handleHover}
          {...props}
        />
      ))}
      <HoverFollowPreview
        name={hoverName ? (dfc.displayName ?? hoverName) : null}
        imageUri={hoverName ? (dfc.displayImageUri ?? hoverCard?.imageUri ?? null) : null}
        overlay={dfc.hint}
        imageRotateDeg={landscapeImageRotateDeg(hoverCard)}
      />
    </div>
  )
}

function SeatPanel({
  seat,
  seatNumber,
  isActivePlayer,
  canRemove,
  onHover,
  ...props
}: ScenarioBoardProps & {
  seat: BuilderSeat
  seatNumber: number
  isActivePlayer: boolean
  canRemove: boolean
  onHover: (name: string | null) => void
}) {
  const smallZones = SCENARIO_ZONES.filter((z) => z !== 'battlefield')
  return (
    <section className={isActivePlayer ? styles.seatActive : styles.seat}>
      <header className={styles.seatHead}>
        <span className={styles.seatBadge}>
          Seat {seatNumber}
          {isActivePlayer ? ' · active' : ''}
        </span>
        <input
          className={styles.nameInput}
          value={seat.name}
          aria-label={`Seat ${seatNumber} name`}
          onChange={(e) => props.onSeatPatch(seat.id, { name: e.target.value })}
        />
        <div className={styles.lifeWrap}>
          <span className={styles.lifeHeart} aria-hidden="true">♥</span>
          <button
            type="button"
            className={styles.stepBtn}
            aria-label={`Lower ${seat.name}'s life`}
            onClick={() => props.onSeatPatch(seat.id, { life: seat.life - 1 })}
          >
            −
          </button>
          <input
            type="number"
            className={styles.lifeInput}
            value={seat.life}
            aria-label={`${seat.name} life total`}
            onChange={(e) => props.onSeatPatch(seat.id, { life: Number(e.target.value) })}
          />
          <button
            type="button"
            className={styles.stepBtn}
            aria-label={`Raise ${seat.name}'s life`}
            onClick={() => props.onSeatPatch(seat.id, { life: seat.life + 1 })}
          >
            +
          </button>
        </div>
        <button
          type="button"
          className={styles.ghostBtn}
          onClick={() => props.onClearSeat(seat.id)}
          title="Empty every zone for this seat"
        >
          Clear
        </button>
        {canRemove && (
          <button
            type="button"
            className={styles.ghostBtn}
            onClick={() => props.onRemoveSeat(seat.id)}
            title="Remove this seat from the pod"
          >
            Remove seat
          </button>
        )}
      </header>

      <ZonePanel zone="battlefield" seat={seat} onHover={onHover} {...props} />

      <div className={styles.zoneRow}>
        {smallZones.map((zone) => (
          <ZonePanel key={zone} zone={zone} seat={seat} onHover={onHover} small {...props} />
        ))}
      </div>
    </section>
  )
}

interface ZoneEntry {
  /** Index in the underlying zone array (the last of a stack, so removing pops one copy). */
  index: number
  name: string
  count: number
  card?: ScenarioBattlefieldCard
}

function ZonePanel({
  zone,
  seat,
  small = false,
  onHover,
  ...props
}: ScenarioBoardProps & {
  zone: ScenarioZone
  seat: BuilderSeat
  small?: boolean
  onHover: (name: string | null) => void
}) {
  const { dragActive, dropHandlers } = useCardDropZone((payload) =>
    props.onDropCard(payload, seat.id, zone),
  )

  const entries = useMemo<ZoneEntry[]>(() => {
    if (zone === 'battlefield') {
      return seat.battlefield.map((card, i) => ({ index: i, name: card.name, count: 1, card }))
    }
    const names = seat[zone]
    if (!PILE_ZONES.includes(zone)) {
      return names.map((name, i) => ({ index: i, name, count: 1 }))
    }
    // Pile: collapse duplicates onto one tile carrying the *last* index, so removing a copy
    // pops from the back and the remaining indices stay valid.
    const byName = new Map<string, ZoneEntry>()
    names.forEach((name, i) => {
      const existing = byName.get(name)
      if (existing) {
        existing.count += 1
        existing.index = i
      } else {
        byName.set(name, { index: i, name, count: 1 })
      }
    })
    return [...byName.values()]
  }, [seat, zone])

  const isTarget = props.target.seatId === seat.id && props.target.zone === zone
  const size = zone === 'battlefield' ? 'large' : 'small'
  const classes = [
    small ? styles.zoneSmall : styles.zoneBattlefield,
    dragActive ? styles.zoneDropActive : '',
    isTarget ? styles.zoneTarget : '',
  ]
    .filter(Boolean)
    .join(' ')

  const total = zone === 'battlefield' ? seat.battlefield.length : seat[zone].length

  return (
    <div className={classes} {...dropHandlers}>
      <div
        className={isTarget ? styles.zoneHeadActive : styles.zoneHead}
        onClick={() => props.onTargetChange({ seatId: seat.id, zone })}
        title="Click to make this the target for cards clicked in the browser"
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            props.onTargetChange({ seatId: seat.id, zone })
          }
        }}
      >
        {ZONE_LABEL[zone]} <span className={styles.zoneCount}>{total}</span>
        {total > 0 && (
          <button
            type="button"
            className={styles.zoneClear}
            title={`Empty ${ZONE_LABEL[zone].toLowerCase()}`}
            onClick={(e) => {
              e.stopPropagation()
              props.onClearZone(seat.id, zone)
            }}
          >
            clear
          </button>
        )}
      </div>
      <div className={styles.zoneCards}>
        {entries.length === 0 && (
          <span className={styles.zoneEmpty}>
            {isTarget ? 'Click cards to add them here' : 'Drop cards here'}
          </span>
        )}
        {entries.map((entry) => (
          <ScenarioCard
            key={`${entry.name}-${entry.index}`}
            entry={entry}
            zone={zone}
            seatId={seat.id}
            size={size}
            summary={props.index[entry.name]}
            onHover={onHover}
            onRemove={() => props.onRemove(seat.id, zone, entry.index)}
            onEdit={
              zone === 'battlefield' ? () => props.onEditCard(seat.id, entry.index) : undefined
            }
            onQuickPatch={
              zone === 'battlefield'
                ? (patch) => props.onQuickPatch(seat.id, entry.index, patch)
                : undefined
            }
          />
        ))}
      </div>
    </div>
  )
}

function ScenarioCard({
  entry,
  zone,
  seatId,
  size,
  summary,
  onHover,
  onRemove,
  onEdit,
  onQuickPatch,
}: {
  entry: ZoneEntry
  zone: ScenarioZone
  seatId: string
  size: 'small' | 'large'
  summary: CardSummary | undefined
  onHover: (name: string | null) => void
  onRemove: () => void
  /** Battlefield only — the other zones have nothing to edit beyond "which card is it". */
  onEdit?: (() => void) | undefined
  onQuickPatch?: ((patch: Partial<ScenarioBattlefieldCard>) => void) | undefined
}) {
  const [imgFailed, setImgFailed] = useState(false)
  const card = entry.card
  const counters = card?.counters ?? {}
  const counterTotal = Object.values(counters).reduce((a, b) => a + b, 0)
  const plusOne = counters.PLUS_ONE_PLUS_ONE ?? 0
  const imageUrl = getCardImageUrl(entry.name, summary?.imageUri ?? null, 'small')

  return (
    <div
      className={[size === 'large' ? styles.cardLarge : styles.card, card?.tapped ? styles.cardTapped : '']
        .filter(Boolean)
        .join(' ')}
      draggable
      onDragStart={(e) => setCardDragData(e, entry.name, 'zone', `${seatId}|${zone}|${entry.index}`)}
      onMouseEnter={() => onHover(entry.name)}
      onMouseLeave={() => onHover(null)}
      onClick={() => onEdit?.()}
      onContextMenu={(e) => {
        e.preventDefault()
        onRemove()
      }}
      title={onEdit ? `${entry.name} — click to edit, right-click to remove` : `${entry.name} — right-click to remove`}
    >
      {imgFailed ? (
        // No art for this printing (or the CDN 404'd) — the name still has to be readable,
        // otherwise the tile is an anonymous grey rectangle.
        <div className={styles.cardFallback}>{entry.name}</div>
      ) : (
        <img
          className={card?.tapped ? styles.cardImgTapped : styles.cardImg}
          src={imageUrl}
          alt={entry.name}
          loading="lazy"
          onError={() => setImgFailed(true)}
        />
      )}
      {entry.count > 1 && <span className={styles.stackCount}>×{entry.count}</span>}
      <div className={styles.cardBadges}>
        {card?.tapped && <span className={styles.badgeTapped}>T</span>}
        {card?.summoningSickness && <span className={styles.badge}>SS</span>}
        {plusOne > 0 && <span className={styles.badgeCounter}>+{plusOne}/+{plusOne}</span>}
        {counterTotal > plusOne && <span className={styles.badgeCounter}>◈</span>}
        {card?.attachedTo && <span className={styles.badgeAttached}>→</span>}
      </div>
      <div className={styles.cardTools}>
        {onQuickPatch && (
          <>
            <button
              type="button"
              className={styles.toolBtn}
              title="Toggle tapped"
              onClick={(e) => {
                e.stopPropagation()
                onQuickPatch({ tapped: !card?.tapped })
              }}
            >
              T
            </button>
            <button
              type="button"
              className={styles.toolBtn}
              title="Add a +1/+1 counter"
              onClick={(e) => {
                e.stopPropagation()
                onQuickPatch({
                  counters: { ...counters, PLUS_ONE_PLUS_ONE: plusOne + 1 },
                })
              }}
            >
              +1
            </button>
          </>
        )}
        <button
          type="button"
          className={`${styles.toolBtn} ${styles.toolBtnDanger}`}
          title="Remove"
          onClick={(e) => {
            e.stopPropagation()
            onRemove()
          }}
        >
          ✕
        </button>
      </div>
    </div>
  )
}
