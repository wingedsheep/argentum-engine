import { useCard, useZoneCards } from '@/store/selectors.ts'
import { command } from '@/types'
import type { ClientPlayer } from '@/types'
import { useResponsiveContext } from './shared'
import { GameCard } from '../card'

/**
 * Command zone widget for Commander-format games.
 *
 * Renders the player's commander(s) face-up alongside the player's main row, mirroring how
 * deck/graveyard/exile sit on the opposite side. Click and drag routing is handled by
 * `GameCard`'s existing flows — the server emits `CastSpell` legal actions with
 * `sourceZone == "COMMAND"` and a post-tax `manaCostString`, so the same hand-cast cost overlay
 * (red/green badge) shows the commander tax automatically. `enableDragToCast` opts the card
 * into hand-style drag-to-play without inheriting hand-only behaviours.
 *
 * A revealed companion (CR 702.139) waits beside it, labelled "Companion": it is outside the game,
 * not in the command zone, but it is public and its owner can pay {3} to put it into hand — the
 * server offers that as a `PutCompanionIntoHand` legal action, which a click on the card takes.
 * It shows in every format, not just Commander. When a commander is there too, the two sit side by
 * side as one pair — stacking them doubled the zone's height, which clipped the commander off the
 * top of your own row and the companion's label off the bottom of an opponent's cell.
 *
 * Renders nothing when the command zone is empty and there is no companion waiting.
 */
export function CommandZone({
  player,
  isOpponent = false,
  compact = false,
}: {
  player: ClientPlayer
  isOpponent?: boolean
  /**
   * A multiplayer board cell (table overview, team rows). The cell's battlefield sizes its cards
   * from the cell, but this zone sat outside that slot and kept the full-board card width — so a
   * commander rendered two or three times the size of everything around it and spilled into the
   * neighbouring cell. Compact matches the zone-pile column on the cell's other side instead.
   */
  compact?: boolean
}) {
  const cards = useZoneCards(command(player.playerId))
  const companion = useCard(player.companionId ?? null)
  const responsive = useResponsiveContext()

  if (cards.length === 0 && !companion) return null

  // Render at battlefield-card size — bigger than the deck/graveyard pile column on the right
  // so the commander reads as a real, interactive card rather than a shrunken pile thumbnail.
  // (`smallCardWidth` is *smaller* than `pileWidth` on desktop, despite the name; battlefield
  // is the correct "regular card" reference size.)
  const cardWidth = compact ? responsive.pileWidth : responsive.battlefieldCardWidth

  const narrow = responsive.isMobile || cardWidth < 72
  const labelStyle = {
    color: '#d4af37',
    fontSize: narrow ? 7 : compact ? 8 : 10,
    textTransform: 'uppercase' as const,
    // On a small card the label is wider than the card at full tracking, pushing the pair apart.
    letterSpacing: narrow ? 0.3 : 1,
    fontWeight: 600,
  }

  // Mirror ZonePile's vertical alignment so the command zone hugs the same edge of the row.
  const verticalOffset = isOpponent
    ? { alignSelf: 'flex-start' as const }
    : { alignSelf: 'flex-end' as const, marginBottom: responsive.sectionGap * 2 }

  const columnStyle = { display: 'flex', flexDirection: 'column' as const, alignItems: 'center', gap: compact ? 3 : 6 }

  return (
    <div
      data-zone={isOpponent ? 'opponent-command' : 'player-command'}
      style={{
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'flex-start',
        gap: compact ? 4 : 8,
        padding: compact ? '0 4px' : '0 8px',
        minWidth: cardWidth + (compact ? 8 : 16),
        ...verticalOffset,
      }}
    >
      {cards.length > 0 && (
        <div style={columnStyle}>
          {cards.map((card) => (
            <GameCard
              key={card.id}
              card={card}
              interactive={!isOpponent}
              overrideWidth={cardWidth}
              isOpponentCard={isOpponent}
              enableDragToCast={!isOpponent}
            />
          ))}
          <span style={labelStyle}>Command</span>
        </div>
      )}
      {companion && (
        <div
          data-zone={isOpponent ? 'opponent-companion' : 'player-companion'}
          title="Companion — outside the game. Its owner may pay {3} to put it into their hand once, any time they could cast a sorcery."
          style={columnStyle}
        >
          <GameCard
            card={companion}
            interactive={!isOpponent}
            overrideWidth={cardWidth}
            isOpponentCard={isOpponent}
          />
          <span style={{ ...labelStyle, color: '#9ec9e8' }}>Companion</span>
        </div>
      )}
    </div>
  )
}
