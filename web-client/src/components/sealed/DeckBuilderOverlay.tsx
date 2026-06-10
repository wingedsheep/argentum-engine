import { useState, useMemo, useCallback, useEffect } from 'react'
import { useGameStore, type DeckBuildingState } from '@/store/gameStore.ts'
import type { SealedCardInfo } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { playableWithinColors } from '@/utils/manaCost.ts'
import { ManaSymbol, ManaCost } from '../ui/ManaSymbols'
import { HoverCardPreview } from '../ui/HoverCardPreview'
import { useDfcHoverFlip } from '../ui/useDfcHoverFlip'
import { SetSynergiesButton, type Archetype } from '../draft/SetSynergiesOverlay'
import { DeckbuilderChatPanel } from './DeckbuilderChatPanel'
import { fetchAdvisors, type AdvisorInfo } from '@/api/aiAssist'
import type { AutoBuildResult } from '@/store/slices/types'
import {
  detectProducedColors,
  suggestBasicLands,
  type BasicLand,
  type DeckEntry,
  type LandColor,
} from '@/utils/landSuggestion'

/**
 * Deck Builder overlay for sealed draft mode.
 */
export function DeckBuilderOverlay() {
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)

  if (!deckBuildingState) return null

  if (deckBuildingState.phase === 'waiting') {
    return <WaitingForOpponent setName={deckBuildingState.setNames.join(' + ')} />
  }

  return <DeckBuilder state={deckBuildingState} />
}

/**
 * Waiting screen shown to the first player while waiting for opponent.
 */
function WaitingForOpponent({ setName }: { setName: string }) {
  const sessionId = useGameStore((state) => state.sessionId)
  const cancelGame = useGameStore((state) => state.cancelGame)
  const responsive = useResponsive()

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 24,
        padding: responsive.containerPadding,
        zIndex: 1000,
      }}
    >
      <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 20 : 28 }}>
        Sealed Draft - {setName}
      </h2>
      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.large }}>
        Waiting for opponent to join...
      </p>
      <div
        style={{
          backgroundColor: '#222',
          padding: '16px 24px',
          borderRadius: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          Session ID:
        </p>
        <code
          style={{
            color: '#4fc3f7',
            fontSize: responsive.isMobile ? 14 : 18,
            backgroundColor: '#333',
            padding: '8px 16px',
            borderRadius: 4,
            fontFamily: 'monospace',
            userSelect: 'all',
          }}
        >
          {sessionId}
        </code>
        <p style={{ color: '#666', margin: 0, fontSize: responsive.fontSize.small }}>
          Share this ID with your opponent
        </p>
      </div>
      <button
        onClick={cancelGame}
        style={{
          marginTop: 8,
          padding: '10px 20px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#c0392b',
          color: 'white',
          border: 'none',
          borderRadius: 4,
          cursor: 'pointer',
        }}
      >
        Cancel Game
      </button>
    </div>
  )
}

/**
 * Main deck builder interface.
 */
function DeckBuilder({ state }: { state: DeckBuildingState }) {
  const responsive = useResponsive()
  const addCardToDeck = useGameStore((s) => s.addCardToDeck)
  const storedDeckCardScores = useGameStore((s) => s.deckCardScores)
  const removeCardFromDeck = useGameStore((s) => s.removeCardFromDeck)
  const clearDeck = useGameStore((s) => s.clearDeck)
  const setLandCount = useGameStore((s) => s.setLandCount)
  const setLlmHighlights = useGameStore((s) => s.setLlmHighlights)
  const setCommander = useGameStore((s) => s.setCommander)
  const submitSealedDeck = useGameStore((s) => s.submitSealedDeck)
  const unsubmitDeck = useGameStore((s) => s.unsubmitDeck)
  const leaveLobby = useGameStore((s) => s.leaveLobby)
  const stopLobby = useGameStore((s) => s.stopLobby)
  const cancelGame = useGameStore((s) => s.cancelGame)
  const lobbyState = useGameStore((s) => s.lobbyState)
  const isInLobby = lobbyState !== null
  const isHost = lobbyState?.isHost ?? false
  // Commander Draft / Sealed lobbies require a commander to be chosen from the pool before the
  // deck can be submitted. The lobby's preset drives the minimum deck size (defaults to 60).
  const lobbyFormat = lobbyState?.settings.format
  const isCommanderShape = lobbyFormat === 'COMMANDER_DRAFT' || lobbyFormat === 'COMMANDER_SEALED'
  const commanderMinDeckSize = lobbyState?.settings.deckSizeMin ?? 60
  // AI assistance is host-gated in lobbies; practice mode (no lobby) always allows it. When off,
  // hide the controls AND any card score badges that were fetched before the host switched it off.
  const aiAssistEnabled = lobbyState ? lobbyState.settings.aiAssistEnabled : true
  const deckCardScores = aiAssistEnabled ? storedDeckCardScores : null

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [sortBy, setSortBy] = useState<'color' | 'cmc' | 'rarity'>('rarity')
  const [colorFilter, setColorFilter] = useState<Set<string>>(new Set())
  const [colorMode, setColorMode] = useState<ColorOp>('<=')
  const [typeFilter, setTypeFilter] = useState<string | null>(null)
  const [searchText, setSearchText] = useState('')
  const [creatureTypeFilter, setCreatureTypeFilter] = useState<string | null>(null)
  const [archetypeFilter, setArchetypeFilter] = useState<Archetype | null>(null)
  // Restrict the pool view to cards inside the chosen commander's colour identity. Defaults to
  // true and resets to true each time a fresh commander is designated — that matches how a
  // player thinks ("only show me things I can actually play"). Hidden / inert when no commander
  // is chosen.
  const [restrictToCommanderIdentity, setRestrictToCommanderIdentity] = useState(true)

  const dfc = useDfcHoverFlip(hoveredCard)
  const resetDfcFlip = dfc.resetFlip

  const handleHover = useCallback((card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard((prev) => {
      if (prev?.name !== card?.name) resetDfcFlip()
      return card
    })
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [resetDfcFlip])

  // Count cards in deck — separate non-basic lands from spells
  const basicLandCount = Object.values(state.landCounts).reduce((a, b) => a + b, 0)
  const nonBasicLandCount = state.deck.filter((cardName) => {
    const info = state.cardPool.find((c) => c.name === cardName)
    return info != null && info.typeLine.toLowerCase().includes('land')
  }).length
  const spellCount = state.deck.length - nonBasicLandCount
  const landCount = basicLandCount + nonBasicLandCount
  // Commander shape: the chosen commander counts toward the deck total. The "must be in
  // state.deck" invariant (see effect below) means state.deck.length already includes the
  // commander once, so don't add another. Guard the !includes path defensively in case the
  // invariant is briefly broken between renders.
  const commanderAlreadyInDeck =
    state.commander != null && state.deck.includes(state.commander)
  const commanderInTotal =
    isCommanderShape && state.commander != null && !commanderAlreadyInDeck ? 1 : 0
  const totalCount = state.deck.length + basicLandCount + commanderInTotal
  const requiredSize = isCommanderShape ? commanderMinDeckSize : 40
  const isValidDeck = totalCount >= requiredSize && (!isCommanderShape || state.commander != null)

  // Pool-filtered eligible commanders for the limited commander formats. Matches the backend's
  // CommanderEligibility: legendary creature, legendary planeswalker, or any card with an
  // explicit "can be your commander" override clause in the oracle text. Returned as a Set so
  // each deck row can check eligibility in O(1) when deciding whether to enable its crown.
  const eligibleCommanderNames = useMemo<Set<string>>(() => {
    const out = new Set<string>()
    if (!isCommanderShape) return out
    const re = /can be your commander/i
    for (const card of state.cardPool) {
      const tl = card.typeLine.toLowerCase()
      const isLegendary = tl.includes('legendary')
      const isCreature = tl.includes('creature')
      const isPlaneswalker = tl.includes('planeswalker')
      const override = re.test(card.oracleText ?? '')
      if ((isLegendary && (isCreature || isPlaneswalker)) || override) {
        out.add(card.name)
      }
    }
    return out
  }, [state.cardPool, isCommanderShape])

  // The chosen commander's colour identity. Empty = colourless. Null = no commander chosen.
  // Used to decide which deck rows are off-identity.
  const commanderIdentity = useMemo<ReadonlySet<string> | null>(() => {
    if (!isCommanderShape || state.commander == null) return null
    const card = state.cardPool.find((c) => c.name === state.commander)
    return new Set(card?.colorIdentity ?? [])
  }, [state.cardPool, state.commander, isCommanderShape])

  // Reset the identity filter to ON whenever a new commander is designated, so picking a
  // commander immediately restricts the pool view. Clearing the commander leaves the toggle's
  // last value alone — it goes inert because the UI hides the chip.
  useEffect(() => {
    if (state.commander != null) setRestrictToCommanderIdentity(true)
  }, [state.commander])

  // If the designated commander is no longer in the deck (the user removed the last copy via
  // the deck-row click), clear the designation. The identity-filter chip is gated on
  // [commanderIdentity], so it disappears automatically as soon as state.commander goes null.
  // Mirrors the standalone /deckbuilder's "clear commander when removed from deckCards" effect.
  useEffect(() => {
    if (state.commander != null && !state.deck.includes(state.commander)) {
      setCommander(null)
    }
  }, [state.commander, state.deck, setCommander])

  // Deck cards (and chosen commander) whose colour identity escapes the commander's identity.
  // Matches the server's COLOR_IDENTITY_VIOLATION check; surfaced as a red left border on the
  // deck-list row so users get the same visual hint the standalone /deckbuilder shows.
  const offIdentityNames = useMemo<Set<string>>(() => {
    const out = new Set<string>()
    if (!commanderIdentity) return out
    const namesToCheck = new Set<string>(state.deck)
    if (state.commander != null) namesToCheck.add(state.commander)
    for (const name of namesToCheck) {
      const card = state.cardPool.find((c) => c.name === name)
      if (!card) continue
      // The commander itself is always within its own identity — skip it.
      if (name === state.commander) continue
      const id = card.colorIdentity ?? []
      for (const color of id) {
        if (!commanderIdentity.has(color)) {
          out.add(name)
          break
        }
      }
    }
    return out
  }, [state.cardPool, state.deck, state.commander, commanderIdentity])

  // Deck analytics
  const deckAnalytics = useMemo(() => {
    const cardInfos: SealedCardInfo[] = []
    for (const cardName of state.deck) {
      const info = state.cardPool.find((c) => c.name === cardName)
      if (info) cardInfos.push(info)
    }

    let creatureCount = 0
    let nonCreatureCount = 0
    for (const card of cardInfos) {
      if (card.typeLine.toLowerCase().includes('land')) continue
      if (card.typeLine.toLowerCase().includes('creature')) {
        creatureCount++
      } else {
        nonCreatureCount++
      }
    }

    // Mana curve
    const curve: Record<number, number> = {}
    for (const card of cardInfos) {
      const cmc = Math.min(getCmc(card), 7)
      curve[cmc] = (curve[cmc] || 0) + 1
    }

    // Color symbol counts in deck (for mana distribution)
    const colorSymbols: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
    for (const card of cardInfos) {
      const cost = card.manaCost || ''
      const matches = cost.match(/\{([^}]+)\}/g) || []
      for (const match of matches) {
        const inner = match.slice(1, -1)
        if (inner in colorSymbols) {
          colorSymbols[inner] = (colorSymbols[inner] ?? 0) + 1
        }
      }
    }

    // Land color counts (basic + non-basic)
    const landColors: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
    const landColorMap: Record<string, string[]> = {
      Plains: ['W'], Island: ['U'], Swamp: ['B'], Mountain: ['R'], Forest: ['G'],
    }
    for (const [landName, count] of Object.entries(state.landCounts)) {
      const colors = landColorMap[landName]
      if (colors) {
        for (const c of colors) {
          landColors[c] = (landColors[c] ?? 0) + count
        }
      }
    }
    // Non-basic lands in deck
    for (const card of cardInfos) {
      if (!card.typeLine.toLowerCase().includes('land')) continue
      const produced = detectProducedColors({ typeLine: card.typeLine, oracleText: card.oracleText ?? null })
      for (const c of produced) {
        landColors[c] = (landColors[c] ?? 0) + 1
      }
    }

    const creatureTypes = getCreatureSubtypes(cardInfos)

    return { creatureCount, nonCreatureCount, curve, colorSymbols, landColors, creatureTypes }
  }, [state.deck, state.cardPool, state.landCounts])

  // Pool-level creature type stats (across entire card pool, not just deck)
  const poolCreatureTypes = useMemo(() => {
    return getCreatureSubtypes(state.cardPool)
  }, [state.cardPool])

  // Pool-level color distribution (mana symbol counts across entire card pool)
  const poolColorDistribution = useMemo(() => {
    const symbols: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
    for (const card of state.cardPool) {
      const cost = card.manaCost || ''
      const matches = cost.match(/\{([^}]+)\}/g) || []
      for (const match of matches) {
        const inner = match.slice(1, -1)
        if (inner in symbols) {
          symbols[inner] = (symbols[inner] ?? 0) + 1
        }
      }
    }
    const total = Object.values(symbols).reduce((a, b) => a + b, 0)
    return { symbols, total }
  }, [state.cardPool])

  // Group and sort pool cards
  const poolCardGroups = useMemo(() => {
    const deckCardCounts = state.deck.reduce<Record<string, number>>((acc, name) => {
      acc[name] = (acc[name] || 0) + 1
      return acc
    }, {})

    const poolCardCounts: Record<string, { card: SealedCardInfo; totalCount: number }> = {}
    for (const card of state.cardPool) {
      const existing = poolCardCounts[card.name]
      if (existing) {
        existing.totalCount++
      } else {
        poolCardCounts[card.name] = { card, totalCount: 1 }
      }
    }

    const groups: { card: SealedCardInfo; availableCount: number }[] = []
    for (const [name, { card, totalCount }] of Object.entries(poolCardCounts)) {
      const inDeckCount = deckCardCounts[name] || 0
      const availableCount = totalCount - inDeckCount
      if (availableCount > 0) {
        if (archetypeFilter) {
          const cardColors = getCardColors(card)
          const archetypeColors = new Set(archetypeFilter.colors)
          let matches = false
          if (cardColors.size === 0) {
            matches = true // colorless cards match any archetype
          } else {
            matches = true
            for (const c of cardColors) {
              if (!archetypeColors.has(c)) {
                matches = false
                break
              }
            }
          }
          if (!matches) continue
        }
        if (colorFilter.size > 0) {
          const cardColors = getCardColors(card)
          if (!matchesColorIdentityFilter(cardColors, colorFilter, colorMode, card.manaCost || '')) continue
        }
        // Commander-identity restriction: every colour in the card's identity must appear in
        // the commander's identity. Mirrors the server's COLOR_IDENTITY_VIOLATION check.
        if (commanderIdentity != null && restrictToCommanderIdentity) {
          let outside = false
          for (const color of card.colorIdentity ?? []) {
            if (!commanderIdentity.has(color)) {
              outside = true
              break
            }
          }
          if (outside) continue
        }
        if (typeFilter) {
          if (!matchesTypeFilter(card, typeFilter)) continue
        }
        if (creatureTypeFilter) {
          if (!matchesCreatureTypeFilter(card, creatureTypeFilter)) continue
        }
        if (searchText) {
          if (!matchesSearch(card, searchText)) continue
        }
        groups.push({ card, availableCount })
      }
    }

    return groups.sort((a, b) => {
      if (sortBy === 'color') {
        return getColorOrder(a.card) - getColorOrder(b.card) || getCmc(a.card) - getCmc(b.card)
      } else if (sortBy === 'cmc') {
        return getCmc(a.card) - getCmc(b.card)
      } else {
        return getRarityOrder(a.card) - getRarityOrder(b.card) || getCmc(a.card) - getCmc(b.card)
      }
    })
  }, [state.cardPool, state.deck, sortBy, colorFilter, colorMode, typeFilter, creatureTypeFilter, searchText, archetypeFilter, commanderIdentity, restrictToCommanderIdentity])

  const totalPoolCards = poolCardGroups.reduce((sum, g) => sum + g.availableCount, 0)

  const poolByRarity = useMemo(() => {
    if (sortBy !== 'rarity') return null

    type PoolGroup = { card: SealedCardInfo; availableCount: number }
    const mythic: PoolGroup[] = []
    const rare: PoolGroup[] = []
    const uncommon: PoolGroup[] = []
    const common: PoolGroup[] = []

    for (const group of poolCardGroups) {
      const rarity = group.card.rarity.toUpperCase()
      if (rarity === 'MYTHIC') mythic.push(group)
      else if (rarity === 'RARE') rare.push(group)
      else if (rarity === 'UNCOMMON') uncommon.push(group)
      else common.push(group)
    }

    return { MYTHIC: mythic, RARE: rare, UNCOMMON: uncommon, COMMON: common }
  }, [poolCardGroups, sortBy])

  // Cards highlighted by archetype creature types OR by the LLM advisor.
  // LLM highlights replace archetype highlights when present.
  const highlightedCards = useMemo(() => {
    if (state.llmHighlightedCards && state.llmHighlightedCards.length > 0) {
      return new Set(state.llmHighlightedCards)
    }
    if (!archetypeFilter?.creatureTypes || archetypeFilter.creatureTypes.length === 0) return null
    const types = archetypeFilter.creatureTypes
    const names = new Set<string>()
    for (const card of state.cardPool) {
      const dashIndex = card.typeLine.indexOf('—')
      if (dashIndex === -1) continue
      const subtypes = card.typeLine.substring(dashIndex + 1).trim().split(/\s+/)
      for (const type of types) {
        if (subtypes.includes(type)) {
          names.add(card.name)
          break
        }
      }
    }
    return names
  }, [state.cardPool, archetypeFilter, state.llmHighlightedCards])

  // Group deck cards by name for vertical list
  const deckCardGroups = useMemo(() => {
    const groups: Record<string, { card: SealedCardInfo; count: number }> = {}
    for (const cardName of state.deck) {
      if (!groups[cardName]) {
        const cardInfo = state.cardPool.find((c) => c.name === cardName)
        if (cardInfo) {
          groups[cardName] = { card: cardInfo, count: 0 }
        }
      }
      if (groups[cardName]) {
        groups[cardName].count++
      }
    }
    return Object.values(groups).sort((a, b) => getCmc(a.card) - getCmc(b.card) || a.card.name.localeCompare(b.card.name))
  }, [state.deck, state.cardPool])

  const isSubmitted = state.phase === 'submitted'

  // Max bar height for mana curve
  const maxCurveCount = Math.max(1, ...Object.values(deckAnalytics.curve))
  const totalColorSymbols = Object.values(deckAnalytics.colorSymbols).reduce((a, b) => a + b, 0)

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: '#1a1a1a',
        display: 'flex',
        flexDirection: 'column',
        zIndex: 1000,
        overflow: 'hidden',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: responsive.isMobile ? '6px 12px' : '8px 24px',
          backgroundColor: '#222',
          borderBottom: '1px solid #444',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 16 : 20 }}>
            Deck Builder - {state.setNames.join(' + ')}
          </h2>
          <SetSynergiesButton
            setCodes={state.setCodes}
            cardPool={state.cardPool}
            onSelectArchetype={(archetype) => {
              // Picking an archetype takes over the highlight slot from the LLM advisor.
              setLlmHighlights(null)
              setArchetypeFilter((prev) => prev?.name === archetype.name ? null : archetype)
            }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {state.opponentReady && (
            <span style={{ color: '#4caf50', fontSize: responsive.fontSize.small }}>
              Opponent ready
            </span>
          )}

          <div
            style={{
              padding: '6px 14px',
              backgroundColor: isValidDeck ? '#2e7d32' : '#555',
              borderRadius: 6,
              color: 'white',
              fontWeight: 600,
              fontSize: responsive.fontSize.normal,
            }}
          >
            {totalCount} / {requiredSize}
          </div>
          {isCommanderShape && !isSubmitted && state.commander == null && (
            <div
              title="Click the crown next to a legendary creature in your deck to designate it as your commander."
              style={{
                padding: responsive.isMobile ? '4px 10px' : '6px 14px',
                fontSize: responsive.isMobile ? 11 : 12,
                backgroundColor: '#3a2b00',
                color: '#ffb300',
                border: '1px solid #b8860b',
                borderRadius: 6,
                fontWeight: 600,
              }}
            >
              ♛ Pick a commander
            </div>
          )}
          {isCommanderShape && !isSubmitted && state.commander != null && (
            <div
              title={`Commander: ${state.commander}`}
              style={{
                padding: responsive.isMobile ? '4px 10px' : '6px 14px',
                fontSize: responsive.isMobile ? 11 : 12,
                backgroundColor: '#3a2b00',
                color: '#ffd76b',
                border: '1px solid #daa520',
                borderRadius: 6,
                fontWeight: 600,
                maxWidth: responsive.isMobile ? 160 : 240,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              ♛ {state.commander}
            </div>
          )}

          {!isSubmitted && aiAssistEnabled && (
            <AutoBuildControl hasCards={totalCount > 0} responsive={responsive} />
          )}

          {isSubmitted ? (
            <button
              onClick={unsubmitDeck}
              style={{
                padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#ff9800',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Edit Deck
            </button>
          ) : (
            <button
              onClick={submitSealedDeck}
              disabled={!isValidDeck}
              title={
                isValidDeck
                  ? undefined
                  : isCommanderShape && state.commander == null
                  ? 'Designate a commander first — click the crown ♛ on a legendary creature in your deck'
                  : `Deck needs at least ${requiredSize} cards`
              }
              style={{
                padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: isValidDeck ? '#4caf50' : '#555',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: isValidDeck ? 'pointer' : 'not-allowed',
                fontWeight: 600,
              }}
            >
              Submit Deck
            </button>
          )}

          {isInLobby ? (
            isHost ? (
              <button
                onClick={stopLobby}
                style={{
                  padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: '#c0392b',
                  color: 'white',
                  border: 'none',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                Stop Game
              </button>
            ) : (
              <button
                onClick={leaveLobby}
                style={{
                  padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: '#c0392b',
                  color: 'white',
                  border: 'none',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                Leave
              </button>
            )
          ) : (
            <button
              onClick={cancelGame}
              style={{
                padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#c0392b',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Cancel Game
            </button>
          )}
        </div>
      </div>

      {/* Main content: Pool (left ~70%) | Deck (right ~30%) */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: responsive.isMobile ? 'column' : 'row',
          overflow: 'hidden',
        }}
      >
        {/* Card Pool (left) */}
        <div
          style={{
            flex: responsive.isMobile ? 1 : 7,
            display: 'flex',
            flexDirection: 'column',
            borderRight: responsive.isMobile ? 'none' : '1px solid #444',
            borderBottom: responsive.isMobile ? '1px solid #444' : 'none',
            minHeight: 0,
            minWidth: 0,
          }}
        >
          {/* Sort + Filter toolbar */}
          <div
            style={{
              padding: '6px 12px',
              backgroundColor: '#2a2a2a',
              borderBottom: '1px solid #333',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              flexWrap: 'wrap',
            }}
          >
            <span style={{ color: '#888', fontSize: 12 }}>Sort:</span>
            {(['color', 'cmc', 'rarity'] as const).map((option) => (
              <button
                key={option}
                onClick={() => setSortBy(option)}
                style={{
                  padding: '3px 10px',
                  fontSize: 12,
                  backgroundColor: sortBy === option ? '#4fc3f7' : '#444',
                  color: sortBy === option ? '#000' : '#ccc',
                  border: 'none',
                  borderRadius: 4,
                  cursor: 'pointer',
                  textTransform: 'capitalize',
                }}
              >
                {option}
              </button>
            ))}

            <div style={{ width: 1, height: 18, backgroundColor: '#444', margin: '0 4px' }} />

            <span style={{ color: '#888', fontSize: 12 }}>Filter:</span>
            <ColorModeSegmented mode={colorMode} onChange={setColorMode} />
            {COLOR_FILTER_OPTIONS.map(({ key, label }) => {
              const active = colorFilter.has(key)
              return (
                <button
                  key={key}
                  onClick={() => {
                    setColorFilter((prev) => {
                      const next = new Set(prev)
                      if (next.has(key)) next.delete(key)
                      else next.add(key)
                      return next
                    })
                  }}
                  title={label}
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: '50%',
                    border: active ? '2px solid #4fc3f7' : '2px solid transparent',
                    backgroundColor: active ? 'rgba(79, 195, 247, 0.15)' : 'transparent',
                    cursor: 'pointer',
                    opacity: active ? 1 : 0.5,
                    transition: 'all 0.15s',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: 0,
                  }}
                >
                  <ManaSymbol symbol={key === 'M' ? 'WUBRG' : key} size={20} />
                </button>
              )
            })}
            {commanderIdentity != null && (
              <button
                onClick={() => setRestrictToCommanderIdentity((v) => !v)}
                title={
                  restrictToCommanderIdentity
                    ? `Showing only cards inside ${state.commander}'s colour identity. Click to show the full pool.`
                    : `Restrict the pool to cards inside ${state.commander}'s colour identity.`
                }
                style={{
                  padding: '3px 10px',
                  fontSize: 11,
                  backgroundColor: restrictToCommanderIdentity ? '#3a2b00' : '#444',
                  color: restrictToCommanderIdentity ? '#ffd76b' : '#ccc',
                  border: restrictToCommanderIdentity ? '1px solid #daa520' : '1px solid #555',
                  borderRadius: 4,
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                ♛ Identity {restrictToCommanderIdentity ? 'on' : 'off'}
              </button>
            )}
            {colorFilter.size > 0 && (
              <button
                onClick={() => setColorFilter(new Set())}
                style={{
                  padding: '2px 8px',
                  fontSize: 11,
                  backgroundColor: 'transparent',
                  color: '#888',
                  border: '1px solid #555',
                  borderRadius: 4,
                  cursor: 'pointer',
                }}
              >
                Clear
              </button>
            )}

            <div style={{ width: 1, height: 18, backgroundColor: '#444', margin: '0 4px' }} />

            <span style={{ color: '#888', fontSize: 12 }}>Type:</span>
            {TYPE_FILTER_OPTIONS.map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setTypeFilter(typeFilter === key ? null : key)}
                style={{
                  padding: '3px 10px',
                  fontSize: 11,
                  backgroundColor: typeFilter === key ? '#4fc3f7' : '#444',
                  color: typeFilter === key ? '#000' : '#ccc',
                  border: 'none',
                  borderRadius: 4,
                  cursor: 'pointer',
                }}
              >
                {label}
              </button>
            ))}

            <div style={{ width: 1, height: 18, backgroundColor: '#444', margin: '0 4px' }} />

            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <input
                type="text"
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                placeholder="Search cards..."
                style={{
                  padding: '3px 24px 3px 8px',
                  fontSize: 12,
                  backgroundColor: '#333',
                  color: '#ddd',
                  border: searchText ? '1px solid #4fc3f7' : '1px solid #555',
                  borderRadius: 4,
                  outline: 'none',
                  width: 150,
                }}
              />
              {searchText && (
                <button
                  onClick={() => setSearchText('')}
                  style={{
                    position: 'absolute',
                    right: 4,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: '#888',
                    cursor: 'pointer',
                    fontSize: 14,
                    padding: '0 2px',
                    lineHeight: 1,
                  }}
                >
                  ×
                </button>
              )}
            </div>

            <span style={{ color: '#666', fontSize: 12, marginLeft: 'auto' }}>
              Pool: {totalPoolCards}
            </span>
          </div>

          {/* Pool creature types and color distribution overview */}
          {(poolCreatureTypes.length > 0 || poolColorDistribution.total > 0) && (
            <div
              style={{
                padding: '5px 12px',
                backgroundColor: '#242424',
                borderBottom: '1px solid #333',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                flexWrap: 'wrap',
              }}
            >
              <span style={{ color: '#666', fontSize: 10, textTransform: 'uppercase', letterSpacing: '0.05em', flexShrink: 0 }}>
                Pool Types
              </span>
              {poolCreatureTypes.slice(0, 8).map(({ type, count, legendaryCount }) => (
                <span
                  key={type}
                  onClick={() => setCreatureTypeFilter(creatureTypeFilter === type ? null : type)}
                  style={{
                    padding: '1px 6px',
                    backgroundColor: creatureTypeFilter === type ? '#3a5a2a' : '#2e2e2e',
                    borderRadius: 3,
                    fontSize: 10,
                    color: creatureTypeFilter === type ? '#c5e1a5' : '#999',
                    border: `1px solid ${creatureTypeFilter === type ? '#8bc34a' : '#3a3a3a'}`,
                    whiteSpace: 'nowrap',
                    cursor: 'pointer',
                  }}
                >
                  {type} <span style={{ color: '#8bc34a', fontWeight: 600 }}>{count}</span>
                  {legendaryCount > 0 && <span style={{ color: '#ffd54f', fontWeight: 600, fontSize: 9 }} title={`${legendaryCount} legendary`}>{'\u2605'}{legendaryCount}</span>}
                </span>
              ))}
              {poolColorDistribution.total > 0 && (
                <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                  <span style={{ color: '#666', fontSize: 10, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Colors
                  </span>
                  <div style={{ display: 'flex', gap: 1, height: 6, width: 120, borderRadius: 3, overflow: 'hidden' }}>
                    {(['W', 'U', 'B', 'R', 'G'] as const).map((c) => {
                      const pct = ((poolColorDistribution.symbols[c] ?? 0) / poolColorDistribution.total) * 100
                      if (pct === 0) return null
                      return (
                        <div
                          key={c}
                          style={{
                            flex: pct,
                            backgroundColor: MANA_COLORS[c],
                          }}
                          title={`${c}: ${poolColorDistribution.symbols[c]} symbols (${Math.round(pct)}%)`}
                        />
                      )
                    })}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Selected archetype banner */}
          {archetypeFilter && (
            <div
              style={{
                padding: '6px 12px',
                backgroundColor: 'rgba(124, 58, 237, 0.12)',
                borderBottom: '1px solid rgba(124, 58, 237, 0.25)',
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <div style={{ display: 'flex', gap: 3, flexShrink: 0 }}>
                {archetypeFilter.colors.map((c) => (
                  <ManaSymbol key={c} symbol={c} size={16} />
                ))}
              </div>
              <span style={{ color: '#b388ff', fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', flexShrink: 0 }}>
                {archetypeFilter.name}
              </span>
              <span style={{ color: 'rgba(255, 255, 255, 0.5)', fontSize: 11, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {archetypeFilter.description}
              </span>
              <button
                onClick={() => setArchetypeFilter(null)}
                style={{
                  marginLeft: 'auto',
                  background: 'none',
                  border: '1px solid rgba(124, 58, 237, 0.3)',
                  color: '#b388ff',
                  fontSize: 11,
                  padding: '2px 8px',
                  borderRadius: 4,
                  cursor: 'pointer',
                  flexShrink: 0,
                }}
              >
                Clear
              </button>
            </div>
          )}

          {/* Pool cards */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: 8,
            }}
          >
            {sortBy === 'rarity' && poolByRarity ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {(['MYTHIC', 'RARE', 'UNCOMMON', 'COMMON'] as const).map((rarity) => {
                  const groups = poolByRarity[rarity] ?? []
                  if (groups.length === 0) return null
                  const totalInSection = groups.reduce((sum, g) => sum + g.availableCount, 0)
                  return (
                    <div key={rarity}>
                      <RaritySectionHeader rarity={rarity} count={totalInSection} />
                      <div
                        style={{
                          display: 'flex',
                          flexWrap: 'wrap',
                          gap: 6,
                          justifyContent: 'flex-start',
                        }}
                      >
                        {groups.map(({ card, availableCount }) => (
                          <PoolCard
                            key={card.name}
                            card={card}
                            count={availableCount}
                            onClick={() => {
                              if (isSubmitted) return
                              addCardToDeck(card.name)
                              setHoveredCard(null)
                            }}
                            onHover={handleHover}
                            disabled={isSubmitted}
                            highlighted={highlightedCards != null ? highlightedCards.has(card.name) : undefined}
                            score={deckCardScores?.[card.name]?.score ?? null}
                            reason={deckCardScores?.[card.name]?.reason}
                          />
                        ))}
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 6,
                  justifyContent: 'flex-start',
                }}
              >
                {poolCardGroups.map(({ card, availableCount }) => (
                  <PoolCard
                    key={card.name}
                    card={card}
                    count={availableCount}
                    onClick={() => {
                      if (isSubmitted) return
                      addCardToDeck(card.name)
                      setHoveredCard(null)
                    }}
                    onHover={handleHover}
                    disabled={isSubmitted}
                    highlighted={highlightedCards != null ? highlightedCards.has(card.name) : undefined}
                    score={deckCardScores?.[card.name]?.score ?? null}
                    reason={deckCardScores?.[card.name]?.reason}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Deck Panel (right) */}
        <div
          style={{
            flex: responsive.isMobile ? 1 : 3,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: '#1e1e1e',
            minHeight: 0,
            minWidth: 0,
          }}
        >
          {/* Deck Analytics */}
          <div
            style={{
              padding: '8px 12px',
              backgroundColor: '#252525',
              borderBottom: '1px solid #333',
            }}
          >
            {/* Live counters */}
            <div style={{ display: 'flex', gap: 12, marginBottom: 8, flexWrap: 'wrap' }}>
              <DeckStat label="Creatures" value={deckAnalytics.creatureCount} color="#8bc34a" />
              <DeckStat label="Spells" value={deckAnalytics.nonCreatureCount} color="#4fc3f7" />
              <DeckStat label="Lands" value={landCount} color="#a1887f" />
            </div>

            {/* Mana curve histogram */}
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 48 }}>
              {[0, 1, 2, 3, 4, 5, 6, 7].map((cmc) => {
                const count = deckAnalytics.curve[cmc] || 0
                const height = maxCurveCount > 0 ? (count / maxCurveCount) * 40 : 0
                return (
                  <div key={cmc} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, minWidth: 0 }}>
                    {count > 0 && (
                      <span style={{ color: '#aaa', fontSize: 9, marginBottom: 2 }}>{count}</span>
                    )}
                    <div
                      style={{
                        width: '100%',
                        maxWidth: 24,
                        height: Math.max(height, count > 0 ? 3 : 0),
                        backgroundColor: count > 0 ? '#4fc3f7' : 'transparent',
                        borderRadius: '2px 2px 0 0',
                        transition: 'height 0.2s',
                      }}
                    />
                    <span style={{ color: '#666', fontSize: 9, marginTop: 2 }}>
                      {cmc >= 7 ? '7+' : cmc}
                    </span>
                  </div>
                )
              })}
            </div>

            {/* Color distribution bars + mana source counts */}
            {(totalColorSymbols > 0 || Object.values(deckAnalytics.landColors).some((v) => v > 0)) && (
              <div style={{ marginTop: 8 }}>
                {/* Spell pips bar */}
                <div style={{ display: 'flex', gap: 1, height: 6, borderRadius: 3, overflow: 'hidden' }}>
                  {(['W', 'U', 'B', 'R', 'G'] as const).map((c) => {
                    const pct = totalColorSymbols > 0 ? ((deckAnalytics.colorSymbols[c] ?? 0) / totalColorSymbols) * 100 : 0
                    if (pct === 0) return null
                    return (
                      <div
                        key={c}
                        style={{
                          flex: pct,
                          backgroundColor: MANA_COLORS[c],
                          transition: 'flex 0.2s',
                        }}
                        title={`${c}: ${deckAnalytics.colorSymbols[c]} symbols (${Math.round(pct)}%)`}
                      />
                    )
                  })}
                </div>
                {/* Land sources bar */}
                <div style={{ display: 'flex', gap: 1, height: 6, borderRadius: 3, overflow: 'hidden', marginTop: 2 }}>
                  {(['W', 'U', 'B', 'R', 'G'] as const).map((c) => {
                    const count = deckAnalytics.landColors[c] ?? 0
                    if (count === 0) return null
                    return (
                      <div
                        key={c}
                        style={{
                          flex: count,
                          backgroundColor: MANA_COLORS[c],
                          opacity: 0.5,
                          transition: 'flex 0.2s',
                        }}
                        title={`${c} sources: ${count}`}
                      />
                    )
                  })}
                  {Object.values(deckAnalytics.landColors).every((v) => v === 0) && (
                    <div style={{ flex: 1, backgroundColor: '#333' }} />
                  )}
                </div>
                {/* Per-color counts with mana symbols */}
                <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 4 }}>
                  {(['W', 'U', 'B', 'R', 'G'] as const).map((c) => {
                    const pips = deckAnalytics.colorSymbols[c] ?? 0
                    const lands = deckAnalytics.landColors[c] ?? 0
                    if (pips === 0 && lands === 0) return null
                    return (
                      <div
                        key={c}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 2,
                        }}
                        title={`${c}: ${pips} pip${pips !== 1 ? 's' : ''} / ${lands} source${lands !== 1 ? 's' : ''}`}
                      >
                        <ManaSymbol symbol={c} size={12} />
                        <span style={{ fontSize: 10, color: '#999' }}>{pips}</span>
                        <span style={{ fontSize: 10, color: '#555' }}>/</span>
                        <span style={{ fontSize: 10, color: lands < pips ? '#d32f2f' : '#6a6' }}>{lands}</span>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {/* Creature types */}
            {deckAnalytics.creatureTypes.length > 0 && (
              <div style={{ marginTop: 8 }}>
                <div style={{ color: '#666', fontSize: 9, marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Top Creature Types
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                  {deckAnalytics.creatureTypes.slice(0, 6).map(({ type, count, legendaryCount }) => (
                    <span
                      key={type}
                      onClick={() => setCreatureTypeFilter(creatureTypeFilter === type ? null : type)}
                      style={{
                        padding: '1px 6px',
                        backgroundColor: creatureTypeFilter === type ? '#3a5a2a' : '#333',
                        borderRadius: 3,
                        fontSize: 9,
                        color: creatureTypeFilter === type ? '#c5e1a5' : '#bbb',
                        border: `1px solid ${creatureTypeFilter === type ? '#8bc34a' : '#444'}`,
                        whiteSpace: 'nowrap',
                        cursor: 'pointer',
                      }}
                    >
                      {type} <span style={{ color: '#8bc34a', fontWeight: 600 }}>{count}</span>
                      {legendaryCount > 0 && <span style={{ color: '#ffd54f', fontWeight: 600, fontSize: 9 }} title={`${legendaryCount} legendary`}>{'\u2605'}{legendaryCount}</span>}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Deck header */}
          <div
            style={{
              padding: '6px 12px',
              backgroundColor: '#222',
              borderBottom: '1px solid #333',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <span style={{ color: '#ccc', fontSize: 13, fontWeight: 600 }}>
              Your Deck
            </span>
            <span style={{ color: '#888', fontSize: 11 }}>
              ({spellCount} spells + {landCount} lands)
            </span>
            {!isSubmitted && totalCount > 0 && (
              <button
                onClick={clearDeck}
                style={{
                  marginLeft: 'auto',
                  padding: '2px 8px',
                  fontSize: 10,
                  backgroundColor: '#444',
                  color: '#ef5350',
                  border: '1px solid #555',
                  borderRadius: 4,
                  cursor: 'pointer',
                }}
              >
                Clear
              </button>
            )}
          </div>

          {/* Deck card list (vertical) */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: '4px 0',
            }}
          >
            {deckCardGroups.map(({ card, count }) => (
              <DeckListRow
                key={card.name}
                card={card}
                count={count}
                onClick={() => {
                  if (isSubmitted) return
                  removeCardFromDeck(card.name)
                  setHoveredCard(null)
                }}
                onHover={handleHover}
                disabled={isSubmitted}
                showCommanderControls={isCommanderShape && !isSubmitted}
                isCommander={state.commander === card.name}
                canBeCommander={eligibleCommanderNames.has(card.name)}
                offIdentity={offIdentityNames.has(card.name)}
                onToggleCommander={() => {
                  setCommander(state.commander === card.name ? null : card.name)
                }}
              />
            ))}

            {/* Basic lands */}
            <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid #333' }}>
              <div style={{ padding: '0 12px 4px', display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ color: '#666', fontSize: 10, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1 }}>
                  Basic Lands
                </span>
                {!isSubmitted && state.deck.length > 0 && (
                  <button
                    onClick={() => suggestLands(state, state.deck.length, setLandCount)}
                    style={{
                      padding: '2px 8px',
                      fontSize: 10,
                      backgroundColor: '#444',
                      color: '#4fc3f7',
                      border: '1px solid #555',
                      borderRadius: 4,
                      cursor: 'pointer',
                    }}
                  >
                    Suggest
                  </button>
                )}
              </div>
              {state.basicLands.map((land) => (
                <LandRow
                  key={land.name}
                  land={land}
                  count={state.landCounts[land.name] || 0}
                  onIncrement={() => !isSubmitted && setLandCount(land.name, (state.landCounts[land.name] || 0) + 1)}
                  onDecrement={() => !isSubmitted && setLandCount(land.name, (state.landCounts[land.name] || 0) - 1)}
                  onHover={handleHover}
                  disabled={isSubmitted}
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Card preview on hover - positioned near cursor */}
      {hoveredCard && !responsive.isMobile && (
        <HoverCardPreview
          name={dfc.displayName ?? hoveredCard.name}
          imageUri={dfc.displayImageUri ?? hoveredCard.imageUri}
          pos={hoverPos}
          rulings={hoveredCard.rulings}
          overlay={dfc.hint}
        />
      )}

      {/* Submitted overlay */}
      {isSubmitted && (
        <div
          style={{
            position: 'absolute',
            bottom: 24,
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: 'rgba(76, 175, 80, 0.9)',
            color: 'white',
            padding: '16px 28px',
            borderRadius: 8,
            fontWeight: 600,
            fontSize: responsive.fontSize.large,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 6,
          }}
        >
          <span>Deck submitted!</span>
          {!state.opponentReady && (
            <span style={{ fontSize: responsive.fontSize.small, opacity: 0.9 }}>
              Opponent is still building their deck
              <AnimatedDots />
            </span>
          )}
        </div>
      )}

      <DeckbuilderChatPanel state={state} />
    </div>
  )
}

/**
 * Animated ellipsis dots for loading/waiting indicators.
 */
/**
 * "Auto-build" control for the deckbuild header: an optional engine dropdown (shown when more than
 * one engine is registered) plus a button that builds the deck from the pool. An empty deck builds
 * fresh; a partial deck is completed (existing picks kept). Rendered only when AI assistance is
 * enabled for the tournament.
 */
function AutoBuildControl({
  hasCards,
  responsive,
}: {
  hasCards: boolean
  responsive: ReturnType<typeof useResponsive>
}) {
  const autoBuildDeck = useGameStore((s) => s.autoBuildDeck)
  const applyAutoBuildOption = useGameStore((s) => s.applyAutoBuildOption)
  const aiAssistBusy = useGameStore((s) => s.aiAssistBusy)
  const aiAssistError = useGameStore((s) => s.aiAssistError)
  const autoBuildResult = useGameStore((s) => s.autoBuildResult)
  const scoreDeckCards = useGameStore((s) => s.scoreDeckCards)
  const clearDeckCardScores = useGameStore((s) => s.clearDeckCardScores)
  const hasCardScores = useGameStore((s) => s.deckCardScores != null)
  // The selected engine lives in the store so it survives this control remounting on every edit.
  const advisorId = useGameStore((s) => s.deckbuildAdvisorId)
  const setAdvisorId = useGameStore((s) => s.setDeckbuildAdvisorId)

  const [advisors, setAdvisors] = useState<readonly AdvisorInfo[]>([])

  useEffect(() => {
    let cancelled = false
    fetchAdvisors()
      .then((r) => {
        if (cancelled) return
        setAdvisors(r.deckbuild)
        // Keep the player's saved choice; only fall back to the default if it's unset or stale.
        const saved = useGameStore.getState().deckbuildAdvisorId
        if (saved == null || !r.deckbuild.some((a) => a.id === saved)) {
          setAdvisorId(r.deckbuild[0]?.id ?? null)
        }
      })
      .catch(() => {
        // Dropdown stays empty; the button still works using the server's default engine.
      })
    return () => {
      cancelled = true
    }
  }, [setAdvisorId])

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      {advisors.length > 1 && (
        <select
          value={advisorId ?? ''}
          onChange={(e) => setAdvisorId(e.target.value)}
          title="AI engine"
          style={{
            padding: '6px 8px',
            fontSize: 13,
            backgroundColor: '#333',
            color: '#ddd',
            border: '1px solid #555',
            borderRadius: 6,
          }}
        >
          {advisors.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
      )}
      <button
        onClick={() => void autoBuildDeck(advisorId ?? undefined)}
        disabled={aiAssistBusy}
        title={
          hasCards
            ? 'Keep your current picks and fill the rest of the deck from the pool'
            : 'Build the best deck from your pool'
        }
        style={{
          padding: responsive.isMobile ? '6px 14px' : '8px 20px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#7e57c2',
          color: 'white',
          border: 'none',
          borderRadius: 6,
          cursor: aiAssistBusy ? 'wait' : 'pointer',
          fontWeight: 600,
          opacity: aiAssistBusy ? 0.7 : 1,
        }}
      >
        {aiAssistBusy ? 'Building…' : hasCards ? '🪄 Complete Deck' : '🪄 Auto-build'}
      </button>
      <button
        onClick={() => void scoreDeckCards(advisorId ?? undefined)}
        disabled={aiAssistBusy}
        title="Score every card in your pool (badge on each card); uses your current deck for colour context"
        style={{
          padding: responsive.isMobile ? '6px 12px' : '8px 16px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#5e7ec2',
          color: 'white',
          border: 'none',
          borderRadius: 6,
          cursor: aiAssistBusy ? 'wait' : 'pointer',
          fontWeight: 600,
          opacity: aiAssistBusy ? 0.7 : 1,
        }}
      >
        {aiAssistBusy ? 'Scoring…' : hasCardScores ? 'Re-score cards' : '✨ Score cards'}
      </button>
      {hasCardScores && !aiAssistBusy && (
        <button
          onClick={clearDeckCardScores}
          title="Hide the per-card score badges"
          style={{
            padding: '6px 10px',
            fontSize: 13,
            backgroundColor: '#444',
            color: '#ccc',
            border: 'none',
            borderRadius: 6,
            cursor: 'pointer',
          }}
        >
          Hide scores
        </button>
      )}
      {!aiAssistBusy && autoBuildResult && (
        <BuildOptionPicker result={autoBuildResult} onPick={applyAutoBuildOption} />
      )}
      {aiAssistError && <span style={{ color: '#ff6b6b', fontSize: 12 }}>{aiAssistError}</span>}
    </div>
  )
}

/**
 * The Auto-build outcome: the candidate decks as selectable chips. The recommended deck is applied
 * immediately (highlighted here); clicking another chip swaps the deck to that candidate. With a
 * single candidate (heuristic / completion) it renders one non-interactive score badge.
 */
function BuildOptionPicker({
  result,
  onPick,
}: {
  result: AutoBuildResult
  onPick: (index: number) => void
}) {
  const multiple = result.options.length > 1
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
      {multiple && <span style={{ color: '#b9a6e0', fontSize: 12, fontWeight: 600 }}>Builds:</span>}
      {result.options.map((option, i) => {
        const isApplied = i === result.appliedIndex
        const scoreText = option.score != null ? formatBuildScore(result.advisorId, option.score) : null
        return (
          <button
            key={i}
            onClick={() => onPick(i)}
            disabled={!multiple}
            title={
              multiple
                ? isApplied
                  ? 'This build is applied to your deck'
                  : 'Switch your deck to this build'
                : 'Score of the most recent Auto-build'
            }
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              padding: '4px 10px',
              fontSize: 13,
              fontWeight: 600,
              color: isApplied ? '#e8d9ff' : '#b9a6e0',
              backgroundColor: isApplied ? 'rgba(126, 87, 194, 0.35)' : 'rgba(126, 87, 194, 0.12)',
              border: `1px solid ${isApplied ? '#9b7fd4' : 'rgba(126, 87, 194, 0.5)'}`,
              borderRadius: 6,
              cursor: multiple ? 'pointer' : 'default',
            }}
          >
            {option.colors.length > 0 && (
              <span style={{ display: 'inline-flex', gap: 2 }}>
                {option.colors.map((c) => (
                  <ManaSymbol key={c} symbol={c} size={14} />
                ))}
              </span>
            )}
            {option.archetype && <span>{option.archetype}</span>}
            {scoreText && (
              <span style={{ color: isApplied ? '#fff' : '#cdbbef', fontWeight: 700 }}>{scoreText}</span>
            )}
          </button>
        )
      })}
    </div>
  )
}

/** Draftsim reports a 0–10 deck score; the heuristic engine a raw rating sum. */
function formatBuildScore(advisorId: string, score: number): string {
  return advisorId === 'draftsim' ? `${score.toFixed(1)}/10` : score.toFixed(1)
}

function AnimatedDots() {
  const [dotCount, setDotCount] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => {
      setDotCount((prev) => (prev + 1) % 4)
    }, 500)
    return () => clearInterval(interval)
  }, [])

  return <span style={{ display: 'inline-block', width: '1.5em', textAlign: 'left' }}>{'.'.repeat(dotCount)}</span>
}

/**
 * Small stat display for deck analytics.
 */
function DeckStat({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
      <span style={{ color, fontSize: 14, fontWeight: 700 }}>{value}</span>
      <span style={{ color: '#888', fontSize: 10 }}>{label}</span>
    </div>
  )
}

/**
 * Card in the pool (available to add to deck).
 */
function PoolCard({
  card,
  count,
  onClick,
  onHover,
  disabled,
  highlighted,
  score = null,
  reason,
}: {
  card: SealedCardInfo
  count: number
  onClick: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  disabled: boolean
  highlighted?: boolean | undefined
  /** AI per-card score 0–100, or null when scoring is off. */
  score?: number | null
  /** Tooltip justification for the score. */
  reason?: string | undefined
}) {
  const cardWidth = 110
  const cardHeight = Math.round(cardWidth * 1.4)
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'small')
  const defaultBorder = highlighted ? '2px solid #a78bfa' : '2px solid #444'
  // Score badge colour: green (great) → amber → grey (weak).
  const scoreColor = score == null ? '#555' : score >= 70 ? '#4caf50' : score >= 45 ? '#ff9800' : '#777'

  return (
    <div
      onClick={disabled ? undefined : onClick}
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      title={score != null && reason ? `AI score ${score}/100 — ${reason}` : undefined}
      style={{
        position: 'relative',
        width: cardWidth,
        height: cardHeight,
        borderRadius: 6,
        overflow: 'hidden',
        cursor: disabled ? 'default' : 'pointer',
        border: defaultBorder,
        opacity: disabled ? 0.6 : 1,
        transition: 'all 0.15s',
        boxShadow: highlighted ? '0 0 8px rgba(167, 139, 250, 0.4)' : undefined,
      }}
      onMouseOver={(e) => {
        if (!disabled) {
          e.currentTarget.style.border = '2px solid #4fc3f7'
          e.currentTarget.style.transform = 'scale(1.05)'
        }
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.border = defaultBorder
        e.currentTarget.style.transform = 'scale(1)'
      }}
      data-testid="pool-card"
    >
      <img
        src={imageUrl}
        alt={card.name}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
        }}
      />
      {count > 1 && (
        <div
          style={{
            position: 'absolute',
            bottom: 4,
            right: 4,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            color: '#4fc3f7',
            borderRadius: 4,
            padding: '2px 6px',
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          x{count}
        </div>
      )}
      {score != null && (
        <div
          style={{
            position: 'absolute',
            top: 4,
            left: 4,
            backgroundColor: scoreColor,
            color: '#fff',
            borderRadius: 4,
            padding: '1px 5px',
            fontSize: 12,
            fontWeight: 700,
            boxShadow: '0 1px 3px rgba(0,0,0,0.5)',
          }}
        >
          {score}
        </div>
      )}
    </div>
  )
}

/**
 * Compact deck list row with card image strip.
 */
function DeckListRow({
  card,
  count,
  onClick,
  onHover,
  disabled,
  showCommanderControls,
  isCommander,
  canBeCommander,
  offIdentity,
  onToggleCommander,
}: {
  card: SealedCardInfo
  count: number
  onClick: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  disabled: boolean
  showCommanderControls?: boolean
  isCommander?: boolean
  canBeCommander?: boolean
  offIdentity?: boolean
  onToggleCommander?: () => void
}) {
  const cmc = getCmc(card)

  return (
    <div
      onClick={disabled ? undefined : onClick}
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        alignItems: 'center',
        height: 28,
        padding: '0 8px 0 0',
        cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.8 : 1,
        position: 'relative',
        overflow: 'hidden',
        borderBottom: '1px solid #2a2a2a',
        // Off-identity rows get a red left strip + faint red tint, matching the standalone
        // /deckbuilder's deckRowViolation visual treatment.
        borderLeft: offIdentity ? '3px solid #ef5350' : '3px solid transparent',
        backgroundColor: offIdentity ? 'rgba(239, 83, 80, 0.06)' : undefined,
      }}
      onMouseOver={(e) => {
        if (!disabled) e.currentTarget.style.backgroundColor = 'rgba(79, 195, 247, 0.1)'
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.backgroundColor = offIdentity ? 'rgba(239, 83, 80, 0.06)' : 'transparent'
      }}
      title={offIdentity ? `${card.name} is outside the commander's colour identity` : undefined}
    >
      {/* Count */}
      <span
        style={{
          width: 28,
          textAlign: 'center',
          color: '#4fc3f7',
          fontWeight: 600,
          fontSize: 12,
          flexShrink: 0,
        }}
      >
        {count}
      </span>
      {/* Card name */}
      <span style={{
        color: offIdentity ? '#ef9a9a' : '#ddd',
        fontSize: 12,
        flex: 1,
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
      }}>
        {card.name}
      </span>
      {/* Commander crown — only in commander-shape lobbies */}
      {showCommanderControls && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            if (canBeCommander && onToggleCommander) onToggleCommander()
          }}
          disabled={!canBeCommander}
          aria-pressed={!!isCommander}
          aria-label={isCommander ? `Unset ${card.name} as commander` : `Set ${card.name} as commander`}
          title={
            !canBeCommander
              ? 'Only legendary creatures or planeswalkers can be commanders'
              : isCommander
              ? 'Commander — click to unset'
              : 'Set as commander'
          }
          style={{
            marginLeft: 4,
            marginRight: 4,
            padding: '0 4px',
            fontSize: 14,
            backgroundColor: 'transparent',
            color: isCommander ? '#ffd76b' : canBeCommander ? '#666' : '#333',
            border: 'none',
            borderRadius: 3,
            cursor: canBeCommander ? 'pointer' : 'default',
            opacity: !canBeCommander ? 0.35 : isCommander ? 1 : 0.7,
            flexShrink: 0,
            lineHeight: 1,
          }}
        >
          ♛
        </button>
      )}
      {/* Mana cost */}
      <span style={{ marginLeft: 4, flexShrink: 0 }}>
        {card.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : <span style={{ color: '#666', fontSize: 10 }}>({cmc})</span>}
      </span>
    </div>
  )
}

/**
 * Land row in the deck list with +/- buttons.
 */
function LandRow({
  land,
  count,
  onIncrement,
  onDecrement,
  onHover,
  disabled,
}: {
  land: SealedCardInfo
  count: number
  onIncrement: () => void
  onDecrement: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  disabled: boolean
}) {
  return (
    <div
      onMouseEnter={(e) => onHover(land, e)}
      onMouseMove={(e) => onHover(land, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        alignItems: 'center',
        height: 28,
        padding: '0 8px',
        borderBottom: '1px solid #2a2a2a',
      }}
    >
      {/* Count */}
      <span style={{ width: 28, textAlign: 'center', color: '#a1887f', fontWeight: 600, fontSize: 12, flexShrink: 0 }}>
        {count}
      </span>
      {/* Name */}
      <span style={{ color: '#aaa', fontSize: 12, flex: 1 }}>{land.name}</span>
      {/* Buttons */}
      <div style={{ display: 'flex', gap: 2, flexShrink: 0 }}>
        <button
          onClick={disabled ? undefined : onDecrement}
          disabled={disabled || count <= 0}
          style={{
            width: 20,
            height: 20,
            borderRadius: 3,
            border: 'none',
            backgroundColor: count > 0 && !disabled ? '#555' : '#333',
            color: count > 0 && !disabled ? 'white' : '#555',
            cursor: count > 0 && !disabled ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 0,
          }}
        >
          -
        </button>
        <button
          onClick={disabled ? undefined : onIncrement}
          disabled={disabled}
          style={{
            width: 20,
            height: 20,
            borderRadius: 3,
            border: 'none',
            backgroundColor: disabled ? '#333' : '#4caf50',
            color: disabled ? '#555' : 'white',
            cursor: disabled ? 'not-allowed' : 'pointer',
            fontWeight: 600,
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 0,
          }}
        >
          +
        </button>
      </div>
    </div>
  )
}

/**
 * Section header for rarity grouping.
 */
function RaritySectionHeader({ rarity, count }: { rarity: string; count: number }) {
  const colors: Record<string, string> = {
    MYTHIC: '#ff8b00',
    RARE: '#ffd700',
    UNCOMMON: '#c0c0c0',
    COMMON: '#888888',
  }

  const labels: Record<string, string> = {
    MYTHIC: 'Mythic Rare',
    RARE: 'Rare',
    UNCOMMON: 'Uncommon',
    COMMON: 'Common',
  }

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        marginBottom: 6,
        paddingBottom: 4,
        borderBottom: `2px solid ${colors[rarity] || '#888'}`,
      }}
    >
      <span style={{ color: colors[rarity] || '#888', fontWeight: 600, fontSize: 13 }}>
        {labels[rarity] || rarity}
      </span>
      <span style={{ color: '#666', fontSize: 11 }}>({count})</span>
    </div>
  )
}

// Constants

const COLOR_FILTER_OPTIONS = [
  { key: 'W', label: 'White' },
  { key: 'U', label: 'Blue' },
  { key: 'B', label: 'Black' },
  { key: 'R', label: 'Red' },
  { key: 'G', label: 'Green' },
  { key: 'C', label: 'Colorless' },
]

const TYPE_FILTER_OPTIONS = [
  { key: 'creature', label: 'Creature' },
  { key: 'instant', label: 'Instant' },
  { key: 'sorcery', label: 'Sorcery' },
  { key: 'enchantment', label: 'Enchantment' },
  { key: 'artifact', label: 'Artifact' },
  // Substring filter against the type line — matches "Legendary Creature ...",
  // "Legendary Planeswalker — ...", "Legendary Artifact", etc. Useful as a
  // commander-shortlist filter in Commander Draft / Sealed lobbies.
  { key: 'legendary', label: 'Legendary' },
]

const MANA_COLORS: Record<string, string> = {
  W: '#f9faf4',
  U: '#0e68ab',
  B: '#6a6a6a',
  R: '#d32f2f',
  G: '#388e3c',
}

// Helper functions

type ColorOp = ':' | '=' | '<='

/**
 * Filter a card by its color identity against a chip-based selection (W/U/B/R/G/C)
 * using one of three modes that mirror the main deckbuilder's `ColorModeSegmented`:
 *
 * - `:`  Includes — card identity contains all chosen WUBRG colors (AND).
 * - `=`  Exactly — card identity equals exactly the chosen WUBRG set.
 * - `<=` At most — card is *playable* in a deck limited to the chosen WUBRG set; colorless always
 *   passes. Hybrid pips give a choice, so `{R/W}` survives "at most W" (it's castable in mono-white).
 *   See [playableWithinColors].
 *
 * The `C` chip is treated as a separate "include colorless" toggle that ORs with the colored
 * predicate in `:` and `=` modes (in `<=` mode colorless cards always match anyway).
 */
function matchesColorIdentityFilter(
  cardColors: Set<string>,
  filter: Set<string>,
  mode: ColorOp,
  manaCost: string,
): boolean {
  const wanted = new Set<string>()
  let includeColorless = false
  for (const c of filter) {
    if (c === 'C') includeColorless = true
    else wanted.add(c)
  }
  const isColorless = cardColors.size === 0

  if (mode === '<=') {
    if (isColorless) return true
    return playableWithinColors(manaCost, cardColors, wanted)
  }

  if (mode === '=') {
    if (includeColorless && isColorless) return true
    if (wanted.size === 0) return false
    if (cardColors.size !== wanted.size) return false
    for (const w of wanted) if (!cardColors.has(w)) return false
    return true
  }

  // mode === ':' (Includes — AND of chosen colors)
  if (includeColorless && isColorless) return true
  if (wanted.size === 0) return false
  for (const w of wanted) if (!cardColors.has(w)) return false
  return true
}

function ColorModeSegmented({
  mode,
  onChange,
}: {
  mode: ColorOp
  onChange: (op: ColorOp) => void
}) {
  const options: Array<{ op: ColorOp; label: string; title: string }> = [
    { op: ':', label: 'Includes', title: 'Cards that include the chosen colour(s)' },
    { op: '=', label: 'Exactly', title: 'Cards whose colour identity is exactly the chosen set' },
    { op: '<=', label: 'At most', title: 'Cards whose colour identity is a subset of the chosen set' },
  ]
  return (
    <div
      role="group"
      aria-label="Colour comparison mode"
      style={{ display: 'inline-flex', gap: 0, border: '1px solid #444', borderRadius: 4, overflow: 'hidden' }}
    >
      {options.map((opt) => {
        const active = mode === opt.op
        return (
          <button
            key={opt.op}
            type="button"
            onClick={() => onChange(opt.op)}
            title={opt.title}
            style={{
              padding: '4px 8px',
              fontSize: 11,
              backgroundColor: active ? '#4fc3f7' : 'transparent',
              color: active ? '#000' : '#ccc',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            {opt.label}
          </button>
        )
      })}
    </div>
  )
}

/**
 * Color identity (CR 903.4) of [card] as single-letter codes. The server-side identity already
 * folds in oracle-text colored symbols, basic-land subtype colors, and the Scryfall override,
 * so it correctly catches off-color activation costs and dual lands. Falls back to parsing the
 * printed mana cost when the server didn't ship `colorIdentity` (older clients / older messages).
 */
function getCardColors(card: SealedCardInfo): Set<string> {
  const colors = new Set<string>()
  if (card.colorIdentity && card.colorIdentity.length > 0) {
    for (const name of card.colorIdentity) {
      const letter = COLOR_NAME_TO_LETTER[name]
      if (letter) colors.add(letter)
    }
    return colors
  }
  const cost = card.manaCost || ''
  if (cost.includes('W')) colors.add('W')
  if (cost.includes('U')) colors.add('U')
  if (cost.includes('B')) colors.add('B')
  if (cost.includes('R')) colors.add('R')
  if (cost.includes('G')) colors.add('G')
  return colors
}

const COLOR_NAME_TO_LETTER: Record<string, string> = {
  WHITE: 'W',
  BLUE: 'U',
  BLACK: 'B',
  RED: 'R',
  GREEN: 'G',
}

const BASIC_LAND_COLOR: Record<string, LandColor> = {
  Plains: 'W', Island: 'U', Swamp: 'B', Mountain: 'R', Forest: 'G',
}

/**
 * Build curve-aware basic-land suggestions from sealed deck state and apply
 * them via `setLandCount`. Thin adapter over the shared `suggestBasicLands`.
 */
function suggestLands(
  state: DeckBuildingState,
  _spellCount: number,
  setLandCount: (name: string, count: number) => void,
) {
  const basicLandNames = new Set(state.basicLands.map((l) => l.name))

  // Tally deck cards into name → count, skipping basics (they're driven by landCounts).
  const counts = new Map<string, number>()
  for (const name of state.deck) counts.set(name, (counts.get(name) ?? 0) + 1)

  const entries: DeckEntry[] = []
  for (const [name, count] of counts) {
    const info = state.cardPool.find((c) => c.name === name)
    if (!info) continue
    const isBasic = basicLandNames.has(info.name)
    if (isBasic) continue
    const isLand = info.typeLine.toLowerCase().includes('land')
    entries.push({
      name: info.name,
      manaCost: info.manaCost ?? '',
      cmc: getCmc(info),
      isLand,
      isBasicLand: false,
      producedColors: detectProducedColors({
        typeLine: info.typeLine,
        oracleText: info.oracleText ?? null,
      }),
      count,
    })
  }

  const availableBasics: BasicLand[] = state.basicLands
    .map((l) => {
      const color = BASIC_LAND_COLOR[l.name]
      return color ? { name: l.name, color } : null
    })
    .filter((b): b is BasicLand => b !== null)

  const result = suggestBasicLands({ entries, availableBasics, minDeckSize: 40 })

  for (const land of state.basicLands) setLandCount(land.name, result[land.name] ?? 0)
}

function matchesSearch(card: SealedCardInfo, query: string): boolean {
  const q = query.toLowerCase()
  return (
    card.name.toLowerCase().includes(q) ||
    card.typeLine.toLowerCase().includes(q) ||
    (card.oracleText != null && card.oracleText.toLowerCase().includes(q))
  )
}

function matchesTypeFilter(card: SealedCardInfo, filter: string): boolean {
  const typeLine = card.typeLine.toLowerCase()
  return typeLine.includes(filter)
}

function matchesCreatureTypeFilter(card: SealedCardInfo, subtype: string): boolean {
  const typeLine = card.typeLine
  const dashIndex = typeLine.indexOf('\u2014')
  const hyphenIndex = typeLine.indexOf(' - ')
  const splitIndex = dashIndex !== -1 ? dashIndex : hyphenIndex
  if (splitIndex === -1) return false
  const subtypePart = typeLine.slice(splitIndex + (dashIndex !== -1 ? 1 : 3)).trim()
  return subtypePart.split(/\s+/).some((s) => s === subtype)
}

function getColorOrder(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  if (cost.includes('W')) return 1
  if (cost.includes('U')) return 2
  if (cost.includes('B')) return 3
  if (cost.includes('R')) return 4
  if (cost.includes('G')) return 5
  if (cost === '' || cost.match(/^\{[0-9X]+\}$/)) return 6
  return 7
}

function getCmc(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  let cmc = 0
  const matches = cost.match(/\{([^}]+)\}/g) || []
  for (const match of matches) {
    const inner = match.slice(1, -1)
    const num = parseInt(inner, 10)
    if (!isNaN(num)) {
      cmc += num
    } else if (inner !== 'X') {
      cmc += 1
    }
  }
  return cmc
}

function getCreatureSubtypes(cards: readonly SealedCardInfo[]): Array<{ type: string; count: number; legendaryCount: number }> {
  const counts = new Map<string, number>()
  const legendaryCounts = new Map<string, number>()
  for (const card of cards) {
    if (!card.typeLine.toLowerCase().includes('creature')) continue
    const dashIndex = card.typeLine.indexOf('\u2014')
    const hyphenIndex = card.typeLine.indexOf(' - ')
    const splitIndex = dashIndex !== -1 ? dashIndex : hyphenIndex
    if (splitIndex === -1) continue
    const isLegendary = card.typeLine.toLowerCase().includes('legendary')
    const subtypePart = card.typeLine.slice(splitIndex + (dashIndex !== -1 ? 1 : 3)).trim()
    for (const subtype of subtypePart.split(/\s+/)) {
      const trimmed = subtype.trim()
      if (trimmed) {
        counts.set(trimmed, (counts.get(trimmed) || 0) + 1)
        if (isLegendary) {
          legendaryCounts.set(trimmed, (legendaryCounts.get(trimmed) || 0) + 1)
        }
      }
    }
  }
  return Array.from(counts.entries())
    .map(([type, count]) => ({ type, count, legendaryCount: legendaryCounts.get(type) ?? 0 }))
    .sort((a, b) => b.count - a.count)
}

function getRarityOrder(card: SealedCardInfo): number {
  switch (card.rarity.toUpperCase()) {
    case 'MYTHIC':
      return 1
    case 'RARE':
      return 2
    case 'UNCOMMON':
      return 3
    case 'COMMON':
      return 4
    default:
      return 5
  }
}
