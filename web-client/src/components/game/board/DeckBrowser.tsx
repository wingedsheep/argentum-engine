/**
 * Full-screen overlay behind a Deck pile, with two views.
 *
 * - **Deck list** — what you're playing and what's left of it: every distinct card with a
 *   `remaining/copies` count, fully-drawn rows dimmed, next-draw odds, plus the deckbuilder's mana
 *   curve and colour pips. This is the in-game equivalent of a companion-app deck tracker, and it's
 *   built entirely from aggregated counts, so it never reveals library *order*.
 * - **Library order** — the actual library, top to bottom. Cards revealed to the viewer (Scry,
 *   Surveil, look-at-top-N) show face up in position; everything else is a card back.
 *
 * The server only ever sends `deck` for the viewing player (never for spectators, never describing
 * an opponent), so an opponent's Deck pile falls through to the Library-order view alone with no
 * tab strip. That masking is the server's call — this component just renders what it was given.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState } from '@/store/selectors.ts'
import type { ClientDeckCard, EntityId } from '@/types'
import { CARD_BACK_IMAGE_URL, getCardImageUrl } from '@/utils/cardImages.ts'
import { DeckCardBody, type DeckViewCard } from '@/components/deck/GameDeckView'
import { useResponsiveContext, handleImageError } from './shared'
import { styles } from './styles'

type Tab = 'deck' | 'order'

export function DeckBrowser({
  ownerLabel,
  slots,
  deck,
  onClose,
}: {
  /** Title prefix, e.g. "Your" / "Alice's". */
  ownerLabel: string
  /** The library top first: the card where the viewer knows it, `null` for a card back. */
  slots: readonly (EntityId | null)[]
  /** The viewer's own decklist. Empty when this isn't their pile — hides the deck-list tab. */
  deck: readonly ClientDeckCard[]
  onClose: () => void
}) {
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)
  // Default to the deck list when there is one: "what am I playing" is the common question, and
  // library order is only ever interesting right after a scry or surveil.
  const [tab, setTab] = useState<Tab>(deck.length > 0 ? 'deck' : 'order')

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (minimized) setMinimized(false)
        else onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, minimized])

  if (minimized) {
    return (
      <button onClick={() => setMinimized(false)} style={restoreButtonStyle(responsive.isMobile, responsive.fontSize.normal)}>
        ↑ Return to Deck
      </button>
    )
  }

  const showTabs = deck.length > 0

  return (
    <div style={styles.libraryOverlay} onClick={onClose}>
      <div style={styles.libraryBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.libraryBrowserHeader}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <h2 style={styles.libraryBrowserTitle}>{ownerLabel} Deck</h2>
            {showTabs && (
              <div style={tabStrip}>
                <TabButton active={tab === 'deck'} onClick={() => setTab('deck')}>
                  Deck list
                </TabButton>
                <TabButton active={tab === 'order'} onClick={() => setTab('order')}>
                  Library order ({slots.length})
                </TabButton>
              </div>
            )}
          </div>
          <button style={styles.libraryCloseButton} onClick={onClose}>
            ✕
          </button>
        </div>

        {tab === 'deck' ? (
          <DeckListTab deck={deck} librarySize={slots.length} />
        ) : (
          <LibraryOrderTab slots={slots} />
        )}

        <div style={{ display: 'flex', gap: 16 }}>
          <button onClick={() => setMinimized(true)} style={footerButton(responsive.isMobile, responsive.fontSize.normal, true)}>
            View Battlefield
          </button>
          <button onClick={onClose} style={footerButton(responsive.isMobile, responsive.fontSize.normal, false)}>
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * The deck-tracker view. `remaining` counts copies the player can't currently see — normally
 * "still in the library", but a copy hidden from them elsewhere (their card exiled face down)
 * stays counted here too, which is what keeps the panel from leaking what an opponent exiled.
 * The summary line says "unseen" rather than "in library" for exactly that reason.
 */
function DeckListTab({ deck, librarySize }: { deck: readonly ClientDeckCard[]; librarySize: number }) {
  const total = deck.reduce((sum, c) => sum + c.copies, 0)
  const remaining = deck.reduce((sum, c) => sum + c.remaining, 0)
  const cards: readonly DeckViewCard[] = deck.map((c) => ({
    cardName: c.cardName,
    copies: c.copies,
    remaining: c.remaining,
    cmc: c.cmc,
    cardTypes: c.cardTypes,
    colors: c.colors,
    imageUri: c.imageUri,
  }))

  return (
    <div style={deckTabBody}>
      <div style={summaryLine}>
        <strong style={{ color: '#bfdbfe' }}>{remaining}</strong> of {total} cards unseen
        <span style={{ color: '#475569' }}> · </span>
        {librarySize} in library
      </div>
      <DeckCardBody cards={cards} drawPoolSize={librarySize} emptyLabel="No deck recorded for this game." />
    </div>
  )
}

/**
 * The library top-to-bottom. Order matches the real library; a shuffle on the server clears every
 * reveal, so a freshly shuffled library shows entirely face-down.
 */
function LibraryOrderTab({ slots }: { slots: readonly (EntityId | null)[] }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const cardsMap = useGameStore((state) => selectGameState(state)?.cards)
  const responsive = useResponsiveContext()

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)
  const revealedCount = slots.reduce((acc, id) => acc + (id && cardsMap?.[id] ? 1 : 0), 0)

  return (
    <>
      <span style={{ color: '#64748b', fontSize: 11, letterSpacing: 0.5 }}>
        Reading order: top → bottom, left to right
        {revealedCount > 0 ? ` · ${revealedCount} known` : ''}
      </span>
      <div style={styles.libraryCardGrid}>
        {slots.map((id, index) => {
          const card = id ? cardsMap?.[id] : undefined
          const isTop = index === 0
          const isBottom = index === slots.length - 1 && slots.length > 1
          const accent = isTop ? '#fde68a' : isBottom ? '#fb923c' : null
          return (
            <div
              key={id ?? `back-${index}`}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: 6,
                overflow: 'hidden',
                flexShrink: 0,
                position: 'relative',
                boxShadow: accent ? `0 0 0 2px ${accent}, 0 0 14px ${accent}66` : 'none',
              }}
              onMouseEnter={(e) => {
                if (card) hoverCard(card.id, { x: e.clientX, y: e.clientY })
              }}
              onMouseLeave={() => hoverCard(null)}
            >
              {card ? (
                <img
                  src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                  alt={card.name}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={(e) => handleImageError(e, card.name, 'normal')}
                />
              ) : (
                <img
                  src={CARD_BACK_IMAGE_URL}
                  alt="Card back"
                  style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: 0.85 }}
                />
              )}
              {/* Position badge — shown on every card */}
              <div
                style={{
                  position: 'absolute',
                  top: 4,
                  left: 4,
                  fontSize: 10,
                  color: '#bfdbfe',
                  backgroundColor: 'rgba(0,0,0,0.65)',
                  padding: '2px 6px',
                  borderRadius: 3,
                  pointerEvents: 'none',
                }}
              >
                #{index + 1}
              </div>
              {/* Anchor banner across the bottom of the first/last card so orientation
                  is unambiguous regardless of how the grid wraps */}
              {accent && (
                <div
                  style={{
                    position: 'absolute',
                    bottom: 0,
                    left: 0,
                    right: 0,
                    backgroundColor: accent,
                    color: '#1c1917',
                    fontSize: 11,
                    fontWeight: 700,
                    letterSpacing: 1,
                    textAlign: 'center',
                    padding: '4px 0',
                    textTransform: 'uppercase',
                    pointerEvents: 'none',
                  }}
                >
                  {isTop ? 'Top · Next draw' : 'Bottom'}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </>
  )
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: '4px 12px',
        fontSize: 12,
        fontWeight: 600,
        borderRadius: 6,
        cursor: 'pointer',
        border: `1px solid ${active ? '#3b82f6' : '#1e293b'}`,
        backgroundColor: active ? '#1e3a8a' : 'transparent',
        color: active ? '#dbeafe' : '#64748b',
      }}
    >
      {children}
    </button>
  )
}

const tabStrip: React.CSSProperties = { display: 'flex', gap: 6 }

// Unlike the library-order grid — which is a wall of card art and needs no surface of its own —
// the deck list is text, so it gets a panel to sit on or it's unreadable over the battlefield.
const deckTabBody: React.CSSProperties = {
  overflowY: 'auto',
  minWidth: 420,
  maxWidth: 560,
  backgroundColor: '#12121c',
  border: '1px solid #2a2a3e',
  borderRadius: 12,
  padding: '14px 18px',
}

const summaryLine: React.CSSProperties = {
  color: '#94a3b8',
  fontSize: 12,
  fontVariantNumeric: 'tabular-nums',
  marginBottom: 4,
}

const restoreButtonStyle = (isMobile: boolean, fontSize: number | string): React.CSSProperties => ({
  position: 'fixed',
  bottom: 70,
  left: '50%',
  transform: 'translateX(-50%)',
  padding: isMobile ? '10px 16px' : '12px 24px',
  fontSize,
  backgroundColor: '#1e3a8a',
  color: 'white',
  border: 'none',
  borderRadius: 8,
  cursor: 'pointer',
  fontWeight: 600,
  boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
  zIndex: 100,
  display: 'flex',
  alignItems: 'center',
  gap: 8,
})

const footerButton = (isMobile: boolean, fontSize: number | string, primary: boolean): React.CSSProperties => ({
  padding: isMobile ? '10px 20px' : '12px 28px',
  fontSize,
  backgroundColor: primary ? '#1e3a8a' : '#333',
  color: primary ? 'white' : '#aaa',
  border: primary ? 'none' : '1px solid #555',
  borderRadius: 8,
  cursor: 'pointer',
})
