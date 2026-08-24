import { useMemo, useState, useEffect, useCallback } from 'react'
import { createPortal } from 'react-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState, selectViewingPlayerId, useCardLegalActions } from '@/store/selectors.ts'
import { AbilityFlagDisplayNames, ZoneType, zoneIdEquals } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { useResponsiveContext, handleImageError, getCounterStatModifier, hasStatCounters, listCardCounters, getTokenFrameGradient, getTokenFrameTextColor, getPTColor } from '../board/shared'
import { styles } from '../board/styles'
import { counterManaClass } from '@/assets/icons/keywords'
import { HoverCardPreview } from '../../ui/HoverCardPreview'
import { useHasHover } from '@/hooks/useHasHover.ts'
import { ManaCost, AbilityText } from '../../ui/ManaSymbols'
import { buildActionOptions, playCostRange, playLadderOptions } from '@/utils/actionOptions.ts'
import { parseManaCost, totalManaNeeded } from '@/utils/manaCost.ts'

/**
 * Game board card preview — wraps the shared HoverCardPreview with
 * game-specific extras: token frames, stat breakdowns, keywords, revealed labels.
 */
export function CardPreview() {
  const hoveredCardId = useGameStore((state) => state.hoveredCardId)
  const hoverPosition = useGameStore((state) => state.hoverPosition)
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)
  const responsive = useResponsiveContext()
  const hasHover = useHasHover()

  // All hooks must be called before any early return
  const cardActions = useCardLegalActions(hoveredCardId)
  const card = hoveredCardId && gameState ? gameState.cards[hoveredCardId] ?? null : null

  // Check if hovered card is in the player's hand
  const isInHand = useMemo(() => {
    if (!hoveredCardId || !gameState || !playerId) return false
    const handZoneId = { zoneType: ZoneType.HAND, ownerId: playerId }
    const handZone = gameState.zones.find((z) => zoneIdEquals(z.zoneId, handZoneId))
    return handZone?.cardIds.includes(hoveredCardId) ?? false
  }, [hoveredCardId, gameState, playerId])

  // DFC flip state — press F while hovering to see the other face
  const isDfc = card?.isDoubleFaced === true
  const [dfcFlipped, setDfcFlipped] = useState(false)

  // Reset flip when hovering a different card
  useEffect(() => {
    setDfcFlipped(false)
  }, [hoveredCardId])

  const handleFlipKey = useCallback((e: KeyboardEvent) => {
    if (e.key === 'f' || e.key === 'F') {
      setDfcFlipped((prev) => !prev)
    }
  }, [])

  useEffect(() => {
    if (!isDfc) return
    window.addEventListener('keydown', handleFlipKey)
    return () => window.removeEventListener('keydown', handleFlipKey)
  }, [isDfc, handleFlipKey])

  // Every way the server offers to use this card, in the same order and with the same labels as the
  // click-to-play action menu — the ladder below the image renders them one per row. Sharing
  // `buildActionOptions` is the point: the menu and the preview can't disagree about what a card
  // costs, which they did while each kept its own hand-maintained list of cast variants to ignore.
  const actionOptions = useMemo(
    () => (card ? buildActionOptions(card, cardActions) : []),
    [card, cardActions]
  )

  // The badge on the image: the span of prices for playing the card, anchored on the printed cost so
  // the reduced/increased tint means "cheaper or dearer than the card says" rather than "these two
  // options differ". Only for cards in hand, where a price is something the player can act on.
  const manaCostInfo = useMemo(() => {
    if (!isInHand || !card?.manaCost) return null
    const range = playCostRange(actionOptions)
    if (!range) return { cost: card.manaCost, floor: null, isReduced: false, isIncreased: false }
    // Tint judged on the cheapest way to play it — see the matching note in GameCard.
    const printedMana = totalManaNeeded(parseManaCost(card.manaCost))
    const lowMana = totalManaNeeded(parseManaCost(range.low))
    return {
      cost: range.high,
      floor: range.isRange ? range.low : null,
      isReduced: lowMana < printedMana,
      isIncreased: lowMana > printedMana,
    }
  }, [isInHand, card?.manaCost, actionOptions])

  // The ladder lists the ways to play the card wherever it is being played from — hand, a graveyard
  // flashback, a command zone — not the activated abilities of a permanent already on the battlefield.
  const costRows = useMemo(() => playLadderOptions(actionOptions), [actionOptions])

  // Worth a panel when there is more than one row, or when a lone row's cost can be reduced — a
  // convoke or delve spell has one way to cast it and still needs the hint saying what the floor costs
  // you, which is otherwise invisible until the card is clicked.
  const showCostLadder = costRows.length > 1 || costRows.some((o) => o.manaCostReducedTo)

  if (!card) return null

  // On mobile, show the fullscreen overlay (game-specific behaviour). Any device that can't hover
  // gets it too, whatever its width: the cursor-following variant has nowhere to anchor without a
  // cursor, and a landscape tablet would otherwise get a 280px card pinned to the top-left corner.
  //
  // `dismissible` is exactly "can't hover", not "is a phone": with a mouse the preview is dismissed
  // by moving off the card, and a tap-catching backdrop over a hovered card would both swallow the
  // board's clicks and prevent the mouseleave that clears it.
  if (responsive.isMobile || !hasHover) {
    return <MobileCardPreview card={card} dismissible={!hasHover} />
  }

  const isRevealedFaceDown = card.isFaceDown && !!card.revealedName
  // When DFC is flipped via F key, show the other face
  const showingBackFace = isDfc && dfcFlipped
  const displayName = showingBackFace && card.backFaceName
    ? card.backFaceName
    : isRevealedFaceDown ? card.revealedName! : card.name
  const displayImageUri = showingBackFace && card.backFaceImageUri
    ? card.backFaceImageUri
    : isRevealedFaceDown ? (card.revealedImageUri ?? undefined) : card.imageUri

  // Determine if stats are modified
  const isPowerBuffed = card.power !== null && card.basePower !== null && card.power > card.basePower
  const isPowerDebuffed = card.power !== null && card.basePower !== null && card.power < card.basePower
  const isToughnessBuffed = card.toughness !== null && card.baseToughness !== null && card.toughness > card.baseToughness
  const isToughnessDebuffed = card.toughness !== null && card.baseToughness !== null && card.toughness < card.baseToughness
  const hasStatModifications = isPowerBuffed || isPowerDebuffed || isToughnessBuffed || isToughnessDebuffed

  const counterModifier = getCounterStatModifier(card)
  const hasCounters = hasStatCounters(card)
  // Every counter type on the card, not just the ones that move P/T. The stats box below is gated
  // on the card having power/toughness at all, so a land's counters (City of Shadows' storage)
  // could never appear there — this panel is independent of it.
  const allCounters = listCardCounters(card)
  const effectPowerMod = card.power !== null && card.basePower !== null
    ? (card.power - card.basePower) - counterModifier : 0
  const effectToughnessMod = card.toughness !== null && card.baseToughness !== null
    ? (card.toughness - card.baseToughness) - counterModifier : 0
  const hasEffects = effectPowerMod !== 0 || effectToughnessMod !== 0

  // Estimate extra height for positioning
  let extraHeight = 0
  const GAP = 8
  // manaCostInfo overlay is on the image itself, no extra height needed
  // The cost ladder is a real panel though: header + padding, then a row (plus its optional hint line).
  if (showCostLadder) extraHeight += 40 + costRows.length * 26 + GAP
  if (hasStatModifications) extraHeight += 80 + GAP
  if (card.keywords.length > 0 || (card.abilityFlags && card.abilityFlags.length > 0)) extraHeight += 40 + GAP

  // Split-layout cards (CR 709) — Rooms and classic Invasion split spells like
  // Pain // Suffering — are printed portrait with the two halves stacked, so display
  // the image rotated +90° (CW) to read landscape with the halves side by side. Source
  // stores face[1] on top and face[0] on bottom; after rotation face[1] → right, face[0]
  // → left. Rooms additionally dim the locked half with an upright lock chip (below).
  // `isRoom` still drives the per-half lock chips below; orientation is isLandscapeFace's job.
  const isRoom = card.isRoom === true
  // Rotation follows the image actually on screen, which the DFC flip toggle can swap: hovering a
  // Siege and flipping shows the portrait Deluge of the Dead face (don't rotate), and flipping a
  // permanent that is *already* the back face shows the landscape Siege front (do rotate). One
  // server-side flag per face covers every sideways-printed family — splits, Rooms, battles — so
  // this never re-derives orientation from `cardFaces` or a type line.
  const shownFaceIsLandscape = showingBackFace
    ? card.backFaceIsLandscape === true
    : card.isLandscapeFace === true
  const isLandscapePrint = shownFaceIsLandscape
  const landscapeImageRotateDeg: 0 | 90 = isLandscapePrint ? 90 : 0
  // Flip-layout tokens (WOE "Cursed" / "Sorcerer" Roles) carry imageRotation = 180 so the bottom
  // face reads upright. Split-card landscape rotation takes precedence when both somehow apply.
  const previewImageRotateDeg: 0 | 90 | 180 | 270 = landscapeImageRotateDeg !== 0
    ? landscapeImageRotateDeg
    : ((card.imageRotation ?? 0) as 0 | 90 | 180 | 270)

  // Mana cost overlay badge for the card image (only for hand cards)
  const previewOverlay = (
    <>
      {manaCostInfo && (
        <div style={{
          position: 'absolute',
          top: 8,
          right: 8,
          maxWidth: 'calc(100% - 16px)',
          backgroundColor: manaCostInfo.isReduced || manaCostInfo.isIncreased
            ? 'rgba(0, 0, 0, 0.85)'
            : 'rgba(0, 0, 0, 0.7)',
          padding: '3px 6px',
          borderRadius: 6,
          border: `1px solid ${
            manaCostInfo.isReduced ? 'rgba(0, 200, 80, 0.5)'
            : manaCostInfo.isIncreased ? 'rgba(255, 68, 68, 0.5)'
            : 'rgba(255, 255, 255, 0.3)'
          }`,
          boxShadow: manaCostInfo.isReduced ? '0 0 8px rgba(0, 200, 80, 0.3)'
            : manaCostInfo.isIncreased ? '0 0 8px rgba(255, 68, 68, 0.3)'
            : 'none',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-end',
          flexWrap: 'wrap',
          gap: 3,
          zIndex: 5,
        }}>
          {/* Cheapest reachable price first, then the asking price — range convention, and the
              cheap end is the one the player is deciding against. */}
          {manaCostInfo.floor && (
            <>
              <ManaCost cost={manaCostInfo.floor} size={18} gap={2} />
              <span aria-hidden style={{ color: '#9aa4b8', fontSize: 13 }}>–</span>
            </>
          )}
          <ManaCost cost={manaCostInfo.cost} size={18} gap={2} />
        </div>
      )}
      {isDfc && (
        <div style={{
          position: 'absolute',
          bottom: 10,
          left: '50%',
          transform: 'translateX(-50%)',
          backgroundColor: 'rgba(0, 0, 0, 0.88)',
          color: '#d0d4e0',
          fontSize: 13,
          fontWeight: 600,
          padding: '5px 12px',
          borderRadius: 6,
          border: '1px solid rgba(180, 190, 220, 0.5)',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
          whiteSpace: 'nowrap',
          zIndex: 5,
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}>
          <i className={`ms ms-dfc-${showingBackFace ? 'night' : 'day'}`} style={{ fontSize: 14 }} />
          <span style={{
            backgroundColor: 'rgba(255, 255, 255, 0.15)',
            padding: '1px 6px',
            borderRadius: 3,
            fontSize: 12,
            fontWeight: 700,
            letterSpacing: 0.5,
          }}>F</span>
          <span>to flip</span>
        </div>
      )}
      {isRoom && card.cardFaces && card.cardFaces.length === 2 && card.cardFaces.map((face, idx) => {
        if (face.isUnlocked) return null
        // After +90° image rotation: face[1] (source top half) → right of visible,
        // face[0] (source bottom half) → left of visible.
        const onRight = idx === 1
        return (
          <div key={face.faceId} style={{
            position: 'absolute',
            top: 0,
            bottom: 0,
            ...(onRight ? { right: 0 } : { left: 0 }),
            width: '50%',
            backgroundColor: 'rgba(0, 0, 0, 0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            pointerEvents: 'none',
            zIndex: 6,
          }}>
            <div style={{
              backgroundColor: 'rgba(40, 20, 20, 0.92)',
              color: '#e89b9b',
              fontSize: 18,
              padding: '4px 8px',
              borderRadius: 4,
              border: '1px solid rgba(200, 100, 100, 0.6)',
              lineHeight: 1,
              boxShadow: '0 2px 6px rgba(0, 0, 0, 0.5)',
            }}>
              🔒
            </div>
          </div>
        )
      })}
    </>
  )

  return (
    <HoverCardPreview
      name={displayName}
      imageUri={displayImageUri ?? null}
      pos={hoverPosition}
      rulings={card.rulings}
      extraHeight={extraHeight}
      imageRotateDeg={previewImageRotateDeg}
      overlay={previewOverlay}
    >
      {/* Ways to play, with what each one costs. The badge on the image can only fit the two ends of
          the range; this is where an adventure face, a kicker, a morph, an alternative cost or a
          cycling option becomes visible without clicking the card first. Rows the player can't pay
          for stay listed and dimmed — "not yet" is an answer. */}
      {showCostLadder && (
        <div style={styles.cardPreviewCostOptions}>
          <div style={styles.cardPreviewCostHeader}>Ways to play</div>
          {costRows.map((option) => (
            <div key={option.key}>
              <div style={{
                ...styles.cardPreviewCostRow,
                ...(option.isAvailable ? {} : styles.cardPreviewCostRowUnavailable),
              }}>
                <span style={styles.cardPreviewCostLabel}>
                  <AbilityText text={option.label} size={12} />
                </span>
                <span style={styles.cardPreviewCostValue}>
                  {option.manaCostReducedTo ? (
                    <>
                      <span style={styles.cardPreviewCostStruck}>
                        <ManaCost cost={option.manaCost} size={14} gap={1} />
                      </span>
                      <span aria-hidden style={{ color: '#9aa4b8', fontSize: 11 }}>→</span>
                      <ManaCost cost={option.manaCostReducedTo} size={14} gap={1} />
                    </>
                  ) : (
                    <ManaCost cost={option.manaCost} size={14} gap={1} />
                  )}
                </span>
              </div>
              {option.hint && (
                <div style={styles.cardPreviewCostHint}>{option.hint}</div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Stats box (for creatures with modifications) */}
      {card.power !== null && card.toughness !== null && hasStatModifications && (
        <div style={styles.cardPreviewStatsBox}>
          <div style={styles.cardPreviewStatsMain}>
            <span style={{
              color: isPowerBuffed ? '#00ff00' : isPowerDebuffed ? '#ff4444' : '#ffffff',
              fontWeight: 700, fontSize: 26,
            }}>
              {card.power}
            </span>
            <span style={{ color: '#ffffff', fontSize: 26 }}>/</span>
            <span style={{
              color: isToughnessBuffed ? '#00ff00' : isToughnessDebuffed ? '#ff4444' : '#ffffff',
              fontWeight: 700, fontSize: 26,
            }}>
              {card.toughness}
            </span>
          </div>
          <div style={styles.cardPreviewStatsBreakdown}>
            {card.basePower !== null && card.baseToughness !== null && (
              <div style={styles.cardPreviewStatsRow}>
                <span style={styles.cardPreviewStatsLabel}>Base</span>
                <span style={styles.cardPreviewStatsValue}>
                  {card.basePower}/{card.baseToughness}
                </span>
              </div>
            )}
            {hasCounters && (
              <div style={styles.cardPreviewStatsRow}>
                <span style={{...styles.cardPreviewStatsLabel, color: '#66ccff'}}>
                  <i className={`ms ms-${counterModifier >= 0 ? counterManaClass.PLUS_ONE_PLUS_ONE : counterManaClass.MINUS_ONE_MINUS_ONE}`} style={{marginRight: 4, fontSize: 10}} />Counters
                </span>
                <span style={{...styles.cardPreviewStatsValue, color: counterModifier >= 0 ? '#66ccff' : '#ff6666'}}>
                  {counterModifier >= 0 ? '+' : ''}{counterModifier}/{counterModifier >= 0 ? '+' : ''}{counterModifier}
                </span>
              </div>
            )}
            {hasEffects && (
              <div style={styles.cardPreviewStatsRow}>
                <span style={{...styles.cardPreviewStatsLabel, color: '#ffcc66'}}>Effects</span>
                <span style={{...styles.cardPreviewStatsValue, color: '#ffcc66'}}>
                  {effectPowerMod >= 0 ? '+' : ''}{effectPowerMod}/{effectToughnessMod >= 0 ? '+' : ''}{effectToughnessMod}
                </span>
              </div>
            )}
            {card.damage != null && card.damage > 0 && (
              <div style={styles.cardPreviewDamageRow}>
                <span>Damage</span>
                <span style={{ fontWeight: 600, fontFamily: 'monospace' }}>
                  {card.damage}
                </span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Counters panel — the card's full counter inventory, whatever its card type. */}
      {allCounters.length > 0 && (
        <div style={styles.cardPreviewCounters}>
          <div style={styles.cardPreviewCountersHeading}>Counters</div>
          {allCounters.map(({ type, label, count }) => (
            <div key={type} style={styles.cardPreviewCounterRow}>
              <span style={styles.cardPreviewCounterLabel}>
                {counterManaClass[type] && (
                  <i className={`ms ms-${counterManaClass[type]}`} style={{ fontSize: 11 }} />
                )}
                {label}
              </span>
              <span style={styles.cardPreviewCounterValue}>{count}</span>
            </div>
          ))}
        </div>
      )}

      {/* Keywords/abilities info panel */}
      {(card.keywords.length > 0 || (card.abilityFlags && card.abilityFlags.length > 0)) && (
        <div style={styles.cardPreviewKeywords}>
          {card.keywords.map((keyword) => (
            <div key={keyword} style={styles.cardPreviewKeyword}>
              <span style={styles.cardPreviewKeywordName}>{keyword}</span>
            </div>
          ))}
          {/* Ability flags are engine enum names, not printed keywords — a raw "DOESNT_UNTAP" chip
              is the one place the preview shouts an identifier at the player. Prefer the written
              rules text, falling back to the enum name for a flag the client hasn't named yet. */}
          {card.abilityFlags?.map((flag) => (
            <div key={flag} style={styles.cardPreviewKeyword}>
              <span style={styles.cardPreviewKeywordName}>{AbilityFlagDisplayNames[flag] ?? flag}</span>
            </div>
          ))}
        </div>
      )}

      {/* Granted types — the printed image can't show a type an effect added (Super-Soldier Serum
          making its target a Soldier), and the type line above is only printed for tokens, so the
          grant would otherwise be invisible even though the rules apply it. */}
      {(() => {
        // Card types arrive uppercase (matching `cardTypes`); subtypes are already title-case.
        const grantedTypes = [
          ...(card.grantedCardTypes ?? []).map((t) => t.charAt(0) + t.slice(1).toLowerCase()),
          ...(card.grantedSubtypes ?? []),
        ]
        if (grantedTypes.length === 0) return null
        return (
          <div style={styles.cardPreviewEffects}>
            <div style={styles.cardPreviewEffect}>
              <span style={styles.cardPreviewEffectName}>Granted types</span>
              <span style={styles.cardPreviewEffectText}>{grantedTypes.join(', ')}</span>
            </div>
          </div>
        )
      })()}

      {/* Granted / active-effect abilities — temporary text not in the printed oracle text
          (e.g. an ability granted by Dreadmaw's Ire). The DTO carries these in activeEffects;
          the on-card badge only surfaces them on a nested hover, so show them here too. */}
      {card.activeEffects && card.activeEffects.some((e) => e.description) && (
        <div style={styles.cardPreviewEffects}>
          {card.activeEffects
            .filter((e) => e.description)
            .map((effect) => (
              <div key={effect.effectId} style={styles.cardPreviewEffect}>
                <span style={styles.cardPreviewEffectName}>{effect.name}</span>
                <span style={styles.cardPreviewEffectText}>
                  <AbilityText text={effect.description ?? ''} size={13} />
                </span>
              </div>
            ))}
        </div>
      )}
    </HoverCardPreview>
  )
}

/**
 * Mobile fullscreen card preview overlay (game-specific).
 */
function MobileCardPreview({ card, dismissible = false }: { card: import('@/types').ClientCard; dismissible?: boolean }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const isRevealedFaceDown = card.isFaceDown && !!card.revealedName
  const cardImageUrl = isRevealedFaceDown
    ? getCardImageUrl(card.revealedName!, card.revealedImageUri ?? undefined, 'large')
    : getCardImageUrl(card.name, card.imageUri, 'large')

  // As wide as the desktop preview wherever the viewport allows, shrinking to fit narrow or short
  // screens — the point of opening it is to read the rules text, which 200px can't carry.
  const previewWidth = 'min(280px, 78vw, calc((100vh - 140px) / 1.4))'

  // Portalled to <body> for the same reason HoverCardPreview is: the spectator/replay shells
  // wrap the board in their own stacking context, and the zone browsers (graveyard/exile/deck)
  // portal to <body> — an in-tree preview lands underneath them.
  return createPortal(
    <div
      style={{
        ...styles.cardPreviewOverlay,
        top: 0, left: 0, right: 0, bottom: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        // A long-press preview clears itself on touchend; one opened from the action menu's
        // "View card" has no such gesture behind it, so the backdrop takes the next tap.
        ...(dismissible ? { pointerEvents: 'auto' as const, cursor: 'pointer' } : null),
      }}
      onClick={dismissible ? () => hoverCard(null) : undefined}
    >
      <div style={{ ...styles.cardPreviewContainer, width: previewWidth, alignItems: 'center' }}>
        <div style={{
          ...styles.cardPreviewCard,
          position: 'relative',
          width: previewWidth,
          aspectRatio: '63 / 88',
        }}>
          {card.isToken && card.imageUri?.includes('/art_crop/') ? (
            <div style={{
              ...styles.tokenFrame,
              background: getTokenFrameGradient(card.colors),
              borderRadius: 12,
            }}>
              <div style={{
                ...styles.tokenNameBar,
                color: getTokenFrameTextColor(card.colors),
                fontSize: 14, padding: '5px 10px',
                borderRadius: '8px 8px 0 0',
              }}>
                {card.name}
              </div>
              <div style={styles.tokenArtBox}>
                <img src={cardImageUrl} alt={card.name} style={styles.tokenArtImage} />
              </div>
              <div style={{
                ...styles.tokenTypeBar,
                color: getTokenFrameTextColor(card.colors),
                fontSize: 11, padding: '4px 10px',
                borderRadius: '0 0 8px 8px',
              }}>
                {card.typeLine}
              </div>
              {card.power !== null && card.toughness !== null && (
                <div style={{
                  ...styles.tokenPreviewPT,
                  color: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness),
                }}>
                  {card.power}/{card.toughness}
                </div>
              )}
            </div>
          ) : (
            <img
              src={cardImageUrl}
              alt={isRevealedFaceDown ? card.revealedName! : card.name}
              style={styles.cardPreviewImage}
              onError={(e) => handleImageError(e, isRevealedFaceDown ? card.revealedName! : card.name, 'large')}
            />
          )}
          {/* Same marker as on the battlefield card: a token that copies a real card shows that
              card's image, so only this says it is a token. */}
          {card.isToken && !card.imageUri?.includes('/art_crop/') && (
            <div style={{
              position: 'absolute', top: 8, left: 8,
              backgroundColor: 'rgba(0, 0, 0, 0.78)', color: '#f0f0f0',
              fontSize: 11, fontWeight: 700, letterSpacing: 0.5, padding: '2px 8px', borderRadius: 4,
              border: '1px solid rgba(255, 255, 255, 0.55)', pointerEvents: 'none', whiteSpace: 'nowrap',
            }}>
              TOKEN
            </div>
          )}
          {isRevealedFaceDown && (
            <div style={{
              position: 'absolute', top: 8, left: '50%', transform: 'translateX(-50%)',
              backgroundColor: 'rgba(0, 0, 0, 0.75)', color: '#66ccff',
              fontSize: 12, fontWeight: 600, padding: '2px 10px', borderRadius: 4,
              border: '1px solid rgba(102, 204, 255, 0.5)', pointerEvents: 'none', whiteSpace: 'nowrap',
            }}>
              Revealed
            </div>
          )}
        </div>
        {dismissible && (
          <div style={{
            color: '#aaa',
            fontSize: 12,
            textAlign: 'center',
            textShadow: '0 1px 3px rgba(0, 0, 0, 0.9)',
          }}>
            Tap anywhere to close
          </div>
        )}
      </div>
    </div>,
    document.body,
  )
}
