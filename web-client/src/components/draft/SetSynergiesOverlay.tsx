import { useState, useMemo } from 'react'
import type { SealedCardInfo } from '@/types'
import { ManaSymbol } from '../ui/ManaSymbols'

/**
 * Archetype definition for a set's draft strategy.
 */
export interface Archetype {
  readonly name: string
  /** Mana color symbols, e.g. ['W', 'U'] */
  readonly colors: readonly string[]
  readonly description: string
  /** Creature subtypes this archetype cares about, e.g. ['Wizard'] */
  readonly creatureTypes?: readonly string[]
}

interface SetSynergies {
  readonly setCode: string
  readonly setName: string
  readonly archetypes: readonly Archetype[]
}

/**
 * Static synergy data for each supported set.
 */
const SET_SYNERGIES: Record<string, SetSynergies> = {
  POR: {
    setCode: 'POR',
    setName: 'Portal',
    archetypes: [
      {
        name: 'White Weenie',
        colors: ['W'],
        description: 'Small efficient creatures with combat bonuses. Go wide with cheap soldiers and pump effects to overwhelm before your opponent stabilizes.',
      },
      {
        name: 'Blue Flyers',
        colors: ['U'],
        description: 'Evasive flying creatures backed by card draw sorceries. A tempo-oriented strategy that wins in the air.',
      },
      {
        name: 'Black Control',
        colors: ['B'],
        description: 'Creature removal sorceries and drain effects paired with midrange creatures. Grind opponents out with efficient answers.',
      },
      {
        name: 'Red Aggro',
        colors: ['R'],
        description: 'Aggressive cheap creatures and direct damage sorceries to burn the opponent out before they can set up defenses.',
      },
      {
        name: 'Green Stompy',
        colors: ['G'],
        description: 'Large efficient creatures that overpower opponents through raw stats and size. Simple but effective.',
      },
    ],
  },
  ONS: {
    setCode: 'ONS',
    setName: 'Onslaught',
    archetypes: [
      {
        name: 'Wizards',
        colors: ['U', 'R'],
        creatureTypes: ['Wizard'],
        description: 'Attach auras to Wizards for repeatable removal, and use shapeshifters for extra tribal synergy. A spell-based control deck.',
      },
      {
        name: 'Clerics',
        colors: ['W', 'B'],
        creatureTypes: ['Cleric'],
        description: 'White Clerics defend and heal while black Clerics drain life. The tribe rewards a slow, grinding game plan built on incremental advantages.',
      },
      {
        name: 'Zombies',
        colors: ['B'],
        creatureTypes: ['Zombie'],
        description: 'Recursive threats and strong removal fuel an attrition-based strategy that grinds opponents down relentlessly.',
      },
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        description: 'Red aggression paired with black removal. Goblin tribal synergies create explosive starts backed by efficient creature destruction.',
      },
      {
        name: 'Elves',
        colors: ['G'],
        creatureTypes: ['Elf'],
        description: 'Elves generate mana, gain life, and pump each other. The more Elves you draft, the stronger every individual Elf becomes.',
      },
      {
        name: 'Beasts',
        colors: ['R', 'G'],
        creatureTypes: ['Beast'],
        description: 'Green\'s large beasts combined with red removal create a midrange deck that wins through creature quality and raw combat dominance.',
      },
      {
        name: 'Soldiers / Flyers',
        colors: ['W', 'U'],
        creatureTypes: ['Soldier', 'Bird'],
        description: 'Cheap evasive flyers and Soldier tribal put opponents on a fast clock. A tempo-aggro deck that races on the ground and in the air.',
      },
      {
        name: 'Morph',
        colors: ['U', 'G'],
        description: 'Face-down creatures conceal deadly surprises. Keep your opponent guessing while you set up devastating flip triggers and tempo plays.',
      },
      {
        name: 'Cycling',
        colors: ['W', 'R'],
        description: 'Cycle through your deck to find answers at the right time. Cycling triggers and payoffs generate incremental value while smoothing draws.',
      },
    ],
  },
  LGN: {
    setCode: 'LGN',
    setName: 'Legions',
    archetypes: [
      {
        name: 'Soldiers / Flyers',
        colors: ['W', 'U'],
        creatureTypes: ['Soldier', 'Bird'],
        description: 'Soldiers and Birds combine for a tempo-evasion strategy. Fly over stalled boards while tribal lords pump your ground forces.',
      },
      {
        name: 'Clerics',
        colors: ['W', 'B'],
        creatureTypes: ['Cleric'],
        description: 'The cleric synergy engine continues. In an all-creature set with no removal spells, the lifegain/drain plan is even more dominant.',
      },
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        description: 'Morph Goblins provide rare removal in a removal-starved set. Amplify lords reward drafting Goblins aggressively for explosive starts.',
      },
      {
        name: 'Elves',
        colors: ['G', 'W'],
        creatureTypes: ['Elf'],
        description: 'Elf lords that tap to pump make every Elf a threat. Amplify and provoke create an engine of growth that overwhelms with sheer numbers.',
      },
      {
        name: 'Zombies',
        colors: ['U', 'B'],
        creatureTypes: ['Zombie'],
        description: 'Zombie tribal mixed with blue evasion. Morph creatures provide tempo plays with face-down threats that flip into value.',
      },
      {
        name: 'Slivers',
        colors: ['W', 'U', 'B', 'R', 'G'],
        creatureTypes: ['Sliver'],
        description: 'Every Sliver shares its abilities with all others. A risky but powerful tribal strategy — if the pieces come together, the hive is unstoppable.',
      },
      {
        name: 'Morph',
        colors: ['U', 'G'],
        description: 'An all-creature set means face-down threats are everywhere. Bluff and outmaneuver your opponent with morph mind games on every turn.',
      },
    ],
  },
  SCG: {
    setCode: 'SCG',
    setName: 'Scourge',
    archetypes: [
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        description: 'Black removal plus Goblin tribal synergies. Storm spells can steal games out of nowhere.',
      },
      {
        name: 'Graveyard Value',
        colors: ['B', 'G'],
        description: 'Use cycling and self-mill to fill the graveyard, then recur creatures en masse. Landcycling fixes mana while providing late-game bodies.',
      },
      {
        name: 'Clerics / Control',
        colors: ['W', 'B'],
        creatureTypes: ['Cleric'],
        description: 'White landcyclers ensure land drops while black provides efficient removal. A solid control strategy.',
      },
      {
        name: 'Wizards',
        colors: ['U', 'R'],
        creatureTypes: ['Wizard'],
        description: 'Draw cards proportional to your highest mana cost for massive card advantage. Wizard synergies from the block still anchor the deck.',
      },
      {
        name: 'Ramp',
        colors: ['U', 'G'],
        description: 'Ramp into large green creatures, then leverage their high mana cost for powerful draw spells. Bury opponents in card advantage.',
      },
      {
        name: 'Dragons',
        colors: ['R'],
        creatureTypes: ['Dragon'],
        description: 'Dedicated Dragon tribal support rewards these devastating flying threats. High mana costs are a feature, not a bug.',
      },
      {
        name: 'Landcycling',
        colors: ['W', 'G'],
        description: 'Landcycling creatures fix your mana early and provide large bodies late. Smooths draws across multiple colors.',
      },
    ],
  },
  DOM: {
    setCode: 'DOM',
    setName: 'Dominaria',
    archetypes: [
      {
        name: 'Historic Fliers',
        colors: ['W', 'U'],
        description: 'Evasive flying creatures backed by historic synergies. Artifacts, legendaries, and Sagas trigger payoffs while your flyers close the game in the air.',
      },
      {
        name: 'Historic Control',
        colors: ['U', 'B'],
        description: 'Grind opponents out with Sagas, removal, and card advantage. Sagas schedule value over three turns while efficient answers keep the board in check.',
      },
      {
        name: 'Reckless Aggro',
        colors: ['B', 'R'],
        description: 'Fast creatures with first strike and haste backed by sacrifice synergies. Tokens serve as expendable resources while you race to close the game.',
      },
      {
        name: 'Kicker Ramp',
        colors: ['R', 'G'],
        description: 'Ramp into large creatures and leverage kicker for late-game flexibility. Cards scale from on-curve plays to devastating threats when you have extra mana.',
      },
      {
        name: 'Tokens',
        colors: ['G', 'W'],
        description: 'Flood the board with Saproling and Knight tokens, then use anthems and equipment to turn your wide board into lethal damage.',
      },
      {
        name: 'Legendary Creatures',
        colors: ['W', 'B'],
        description: 'Individually powerful legendary creatures backed by premium removal. A minor Knight tribal subtheme adds synergy to this value-oriented strategy.',
        creatureTypes: ['Knight'],
      },
      {
        name: 'Wizard Spells',
        colors: ['U', 'R'],
        creatureTypes: ['Wizard'],
        description: 'Wizards grow stronger with every instant and sorcery you cast. Build a critical mass of spells and Wizards for explosive prowess-style turns.',
      },
      {
        name: 'Saproling Sacrifice',
        colors: ['B', 'G'],
        description: 'Generate Saprolings and sacrifice them for value. Thallids and fungus creatures create a self-sustaining engine of tokens and drain effects.',
      },
      {
        name: 'Auras & Equipment',
        colors: ['R', 'W'],
        description: 'Cheap creatures augmented by Auras and Equipment for an aggressive voltron strategy. Tiana recovers lost Auras to mitigate card disadvantage.',
      },
      {
        name: 'Ramp',
        colors: ['G', 'U'],
        description: 'Green mana acceleration paired with blue card draw and kicker payoffs. Reach expensive threats and draw extra cards off land drops with Tatyova.',
      },
    ],
  },
  KTK: {
    setCode: 'KTK',
    setName: 'Khans of Tarkir',
    archetypes: [
      {
        name: 'Abzan',
        colors: ['W', 'B', 'G'],
        description: 'Outlast your creatures to grow +1/+1 counters, then share keywords like flying and lifelink across your bolstered army. A grindy, resilient midrange strategy.',
      },
      {
        name: 'Jeskai',
        colors: ['U', 'R', 'W'],
        description: 'Cast noncreature spells to trigger prowess, turning every instant and sorcery into a combat trick. Combines card selection, burn, and tempo.',
      },
      {
        name: 'Sultai',
        colors: ['B', 'G', 'U'],
        description: 'Fill your graveyard to fuel powerful delve spells at reduced cost. A midrange-to-control strategy built on exploiting the graveyard as a resource.',
      },
      {
        name: 'Mardu',
        colors: ['R', 'W', 'B'],
        description: 'Attack each turn to trigger raid bonuses. Wide board presence with tokens and warriors, backed by removal to clear blockers.',
      },
      {
        name: 'Temur',
        colors: ['G', 'U', 'R'],
        description: 'Ferocious abilities activate when you control a creature with power 4 or greater. Ramp into the biggest threats in the format.',
      },
      {
        name: 'Warriors',
        colors: ['W', 'B'],
        creatureTypes: ['Warrior'],
        description: 'Flood the board with cheap Warriors and pump them with tribal lords for an aggressive, low-curve strategy.',
      },
      {
        name: 'Morph',
        colors: ['U', 'G'],
        description: 'Build around face-down creatures for card advantage and tempo. Morph rewards ramping to flip costs first and keeping opponents guessing.',
      },
      {
        name: 'Prowess / Spells',
        colors: ['U', 'R'],
        description: 'Spell-heavy tempo deck. Noncreature spells trigger prowess on your creatures and activate enchantments that tax or generate tokens.',
      },
      {
        name: 'Toughness Matters',
        colors: ['B', 'G'],
        description: 'Draft high-toughness creatures and defenders, then convert that toughness into offense with spells that create tokens based on toughness.',
      },
      {
        name: 'Token Aggro',
        colors: ['R', 'W'],
        description: 'Generate tokens with creature and spell makers, then use mass pump effects to turn a wide board into lethal damage.',
      },
    ],
  },
  BLB: {
    setCode: 'BLB',
    setName: 'Bloomburrow',
    archetypes: [
      {
        name: 'Birds',
        colors: ['W', 'U'],
        creatureTypes: ['Bird'],
        description: 'Mix fliers and ground support creatures. Flier payoffs buff your non-flying forces and vice versa, creating a two-axis attack that keeps opponents stretched thin.',
      },
      {
        name: 'Bats',
        colors: ['W', 'B'],
        creatureTypes: ['Bat'],
        description: 'Use life as a resource — white gains it, black spends it. Bats reward you for both gaining and losing life, letting you flip between aggression and defense as needed.',
      },
      {
        name: 'Mice',
        colors: ['W', 'R'],
        creatureTypes: ['Mouse'],
        description: 'Built around Valiant — the first time each turn a creature is targeted by a spell or ability, it gets a bonus. Target your own creatures with spells and abilities for fast, combat-focused aggro.',
      },
      {
        name: 'Rats',
        colors: ['U', 'B'],
        creatureTypes: ['Rat'],
        description: 'Threshold deck that unlocks powerful bonuses once you have 7+ cards in your graveyard. Self-mill fills the yard quickly, and the deck can tilt aggro or control depending on the matchup.',
      },
      {
        name: 'Otters',
        colors: ['U', 'R'],
        creatureTypes: ['Otter'],
        description: 'Cast a high volume of instants and sorceries to trigger Prowess on your Otter creatures. Classic Izzet spell-slinger that gets bigger the more spells you cast.',
      },
      {
        name: 'Frogs',
        colors: ['U', 'G'],
        creatureTypes: ['Frog'],
        description: 'Value engine built around bouncing and blinking Frogs to re-trigger ETB effects. Mana-intensive but generates enormous card advantage in the late game.',
      },
      {
        name: 'Lizards',
        colors: ['B', 'R'],
        creatureTypes: ['Lizard'],
        description: 'Aggressive deck that rewards dealing damage to opponents and forcing life loss. Creatures gain bonuses when opponents lose life, combining relentless pressure with black removal.',
      },
      {
        name: 'Squirrels',
        colors: ['B', 'G'],
        creatureTypes: ['Squirrel'],
        description: 'Grindy midrange built around the Forage mechanic — sacrifice Food tokens or exile cards from graveyards for value. Squirrels synergize with food production and graveyard interactions.',
      },
      {
        name: 'Raccoons',
        colors: ['R', 'G'],
        creatureTypes: ['Raccoon'],
        description: 'Ramp into massive Raccoon threats, all of which have Expend 4 triggered abilities that fire when you spend 4 or more mana in a turn. Play mana dorks into oversized haymakers.',
      },
      {
        name: 'Rabbits',
        colors: ['G', 'W'],
        creatureTypes: ['Rabbit'],
        description: 'Flood the board with Rabbit tokens, then use mass pump and anthem effects to swing for lethal all at once. One of the most popular and powerful archetypes in the format.',
      },
    ],
  },
  ECL: {
    setCode: 'ECL',
    setName: 'Lorwyn Eclipsed',
    archetypes: [
      {
        name: 'Kithkin',
        colors: ['G', 'W'],
        creatureTypes: ['Kithkin'],
        description: 'Flood the board with 1/1 Kithkin tokens, then use Convoke to cast expensive spells for free. Mass pump effects turn the token horde lethal.',
      },
      {
        name: 'Merfolk',
        colors: ['W', 'U'],
        creatureTypes: ['Merfolk'],
        description: 'Merfolk untap each other after combat and chain together card draw. Control the pace of the game with bounce and tempo plays while building an unstoppable Merrow tide.',
      },
      {
        name: 'Faeries',
        colors: ['U', 'B'],
        creatureTypes: ['Faerie'],
        description: 'Operate entirely at flash speed — drop Faeries at end of turn, steal blockers, and lock out opponents with aerial superiority. A control deck that wins with evasion.',
      },
      {
        name: 'Boggarts',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        description: 'Relentless Boggart aggro backed by Wither. Your attackers deal damage as -1/-1 counters, permanently shrinking blockers. Blight provides flexible sacrifice payoffs.',
      },
      {
        name: 'Giants',
        colors: ['R', 'G'],
        creatureTypes: ['Giant'],
        description: 'Ramp into enormous Giants and close the game with overwhelming power. Green acceleration feeds Red haymakers that are simply too large to deal with.',
      },
      {
        name: 'Elves',
        colors: ['B', 'G'],
        creatureTypes: ['Elf'],
        description: 'Tribal midrange that uses Blight as a resource. Elves build critical mass while Blight costs enable powerful sacrifice synergies and graveyard recursion.',
      },
      {
        name: 'Treefolk',
        colors: ['W', 'B'],
        creatureTypes: ['Treefolk'],
        description: 'High-toughness Treefolk form an impenetrable wall while grinding out advantage. Evoke spells like Emptiness provide burst value and the deck wins through sheer attrition.',
      },
      {
        name: 'Elementals',
        colors: ['U', 'R'],
        creatureTypes: ['Elemental'],
        description: 'Flamekin Elementals reward casting instants and sorceries. Evoke provides flexible tempo plays — pay the cheap cost for a burst of value, or go full price for a permanent threat.',
      },
      {
        name: 'Warriors',
        colors: ['W', 'R'],
        creatureTypes: ['Warrior'],
        description: 'Aggressive Warriors with mana-spent payoffs. Hybrid mana costs unlock dual bonuses — spend white for tokens and resilience, red for pump and haste — giving the deck flexible tools to close games.',
      },
      {
        name: 'Changelings',
        colors: ['U', 'G'],
        creatureTypes: ['Shapeshifter'],
        description: 'Changelings count as every creature type simultaneously, triggering Kithkin, Merfolk, Elf, and Faerie payoffs all at once. A tribal swiss-army-knife deck with enormous flexibility.',
      },
    ],
  },
}

/**
 * Resolves synergy data for the given set codes.
 * Falls back to a generic entry for unknown sets.
 */
function getSynergiesForSets(setCodes: readonly string[]): SetSynergies[] {
  return setCodes
    .map((code) => SET_SYNERGIES[code])
    .filter((s): s is SetSynergies => s != null)
}

/**
 * Returns all archetypes for the given set codes.
 */
export function getArchetypesForSets(setCodes: readonly string[]): Archetype[] {
  return getSynergiesForSets(setCodes).flatMap((s) => s.archetypes)
}

/**
 * Button that opens the set synergies overlay.
 * Renders nothing if no synergy data is available for the given sets.
 */
export function SetSynergiesButton({
  setCodes,
  style,
  onSelectArchetype,
  cardPool,
}: {
  setCodes: readonly string[]
  style?: React.CSSProperties
  onSelectArchetype?: (archetype: Archetype) => void
  cardPool?: readonly SealedCardInfo[]
}) {
  const [isOpen, setIsOpen] = useState(false)

  const synergies = useMemo(() => getSynergiesForSets(setCodes), [setCodes])

  if (synergies.length === 0) return null

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        style={{
          padding: '6px 14px',
          fontSize: 13,
          backgroundColor: 'rgba(124, 58, 237, 0.2)',
          color: '#b388ff',
          border: '1px solid rgba(124, 58, 237, 0.4)',
          borderRadius: 6,
          cursor: 'pointer',
          fontWeight: 600,
          transition: 'background-color 0.15s ease',
          ...style,
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.backgroundColor = 'rgba(124, 58, 237, 0.35)'
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.backgroundColor = 'rgba(124, 58, 237, 0.2)'
        }}
      >
        Archetypes
      </button>
      {isOpen && (
        <SetSynergiesOverlay
          synergies={synergies}
          onClose={() => setIsOpen(false)}
          {...(cardPool != null ? { cardPool } : {})}
          {...(onSelectArchetype != null ? { onSelectArchetype } : {})}
        />
      )}
    </>
  )
}

/**
 * Full-screen overlay showing set synergies/archetypes.
 */
function SetSynergiesOverlay({
  synergies,
  onClose,
  onSelectArchetype,
  cardPool,
}: {
  synergies: SetSynergies[]
  onClose: () => void
  onSelectArchetype?: (archetype: Archetype) => void
  cardPool?: readonly SealedCardInfo[]
}) {
  const [activeTab, setActiveTab] = useState(0)

  const activeSynergy = synergies[activeTab] ?? synergies[0]
  if (!activeSynergy) return null

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.88)',
        zIndex: 1100,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        style={{
          backgroundColor: '#16161e',
          borderRadius: 12,
          border: '1px solid rgba(255, 255, 255, 0.08)',
          maxWidth: 680,
          width: '100%',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 24px 64px rgba(0, 0, 0, 0.7), 0 0 0 1px rgba(255, 255, 255, 0.05)',
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '20px 24px 16px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            flexShrink: 0,
          }}
        >
          <div>
            <h2
              style={{
                margin: 0,
                color: '#fff',
                fontSize: 22,
                fontWeight: 700,
                letterSpacing: '-0.01em',
              }}
            >
              Draft Archetypes
            </h2>
            <p
              style={{
                margin: '4px 0 0',
                color: 'rgba(255, 255, 255, 0.4)',
                fontSize: 13,
              }}
            >
              Synergies and strategies for this format
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'rgba(255, 255, 255, 0.4)',
              fontSize: 24,
              cursor: 'pointer',
              padding: '0 4px',
              lineHeight: 1,
              transition: 'color 0.15s',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.color = '#fff' }}
            onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255, 255, 255, 0.4)' }}
          >
            ×
          </button>
        </div>

        {/* Set Tabs (only if multiple sets) */}
        {synergies.length > 1 && (
          <div
            style={{
              display: 'flex',
              gap: 0,
              borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
              flexShrink: 0,
              padding: '0 24px',
            }}
          >
            {synergies.map((s, i) => (
              <button
                key={s.setCode}
                onClick={() => setActiveTab(i)}
                style={{
                  padding: '10px 18px',
                  fontSize: 13,
                  fontWeight: 600,
                  color: i === activeTab ? '#fff' : 'rgba(255, 255, 255, 0.45)',
                  background: 'none',
                  border: 'none',
                  borderBottom: i === activeTab
                    ? '2px solid #7c3aed'
                    : '2px solid transparent',
                  cursor: 'pointer',
                  transition: 'color 0.15s, border-color 0.15s',
                  letterSpacing: '0.02em',
                }}
                onMouseEnter={(e) => {
                  if (i !== activeTab) e.currentTarget.style.color = 'rgba(255, 255, 255, 0.7)'
                }}
                onMouseLeave={(e) => {
                  if (i !== activeTab) e.currentTarget.style.color = 'rgba(255, 255, 255, 0.45)'
                }}
              >
                {s.setName}
              </button>
            ))}
          </div>
        )}

        {/* Archetype List */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '16px 24px 24px',
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {activeSynergy.archetypes.map((archetype) => (
              <ArchetypeCard
                key={archetype.name}
                archetype={archetype}
                clickable={onSelectArchetype != null}
                {...(cardPool != null ? { cardPool } : {})}
                onClick={() => {
                  if (onSelectArchetype) {
                    onSelectArchetype(archetype)
                    onClose()
                  }
                }}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

interface RarityCounts {
  readonly mythic: number
  readonly rare: number
  readonly uncommon: number
  readonly common: number
  readonly total: number
}

function getCardColors(card: SealedCardInfo): Set<string> {
  const cost = card.manaCost || ''
  const colors = new Set<string>()
  if (cost.includes('W')) colors.add('W')
  if (cost.includes('U')) colors.add('U')
  if (cost.includes('B')) colors.add('B')
  if (cost.includes('R')) colors.add('R')
  if (cost.includes('G')) colors.add('G')
  return colors
}

function isLand(card: SealedCardInfo): boolean {
  return card.typeLine.toLowerCase().includes('land')
}

function getCreatureSubtypes(card: SealedCardInfo): string[] {
  const dashIndex = card.typeLine.indexOf('—')
  if (dashIndex === -1) return []
  return card.typeLine.substring(dashIndex + 1).trim().split(/\s+/)
}

function countCreatureTypes(
  cardPool: readonly SealedCardInfo[],
  creatureTypes: readonly string[],
): Map<string, number> {
  const counts = new Map<string, number>()
  for (const type of creatureTypes) {
    counts.set(type, 0)
  }
  for (const card of cardPool) {
    const subtypes = getCreatureSubtypes(card)
    for (const type of creatureTypes) {
      if (subtypes.includes(type)) {
        counts.set(type, (counts.get(type) ?? 0) + 1)
      }
    }
  }
  return counts
}

function cardMatchesArchetype(card: SealedCardInfo, archetypeColors: Set<string>): boolean {
  if (isLand(card)) return true
  const cardColors = getCardColors(card)
  if (cardColors.size === 0) return true // colorless
  for (const c of cardColors) {
    if (!archetypeColors.has(c)) return false
  }
  return true
}

function countRarities(cards: readonly SealedCardInfo[]): RarityCounts {
  let mythic = 0, rare = 0, uncommon = 0, common = 0
  for (const card of cards) {
    switch (card.rarity.toUpperCase()) {
      case 'MYTHIC': mythic++; break
      case 'RARE': rare++; break
      case 'UNCOMMON': uncommon++; break
      case 'COMMON': common++; break
    }
  }
  return { mythic, rare, uncommon, common, total: mythic + rare + uncommon + common }
}

function computeArchetypeRarities(
  cardPool: readonly SealedCardInfo[],
  archetype: Archetype,
): { onColor: RarityCounts; colorless: number; lands: number } {
  const archetypeColors = new Set(archetype.colors)
  const onColorCards: SealedCardInfo[] = []
  let colorless = 0
  let lands = 0

  for (const card of cardPool) {
    if (!cardMatchesArchetype(card, archetypeColors)) continue
    if (isLand(card)) {
      lands++
    } else if (getCardColors(card).size === 0) {
      colorless++
    } else {
      onColorCards.push(card)
    }
  }

  return { onColor: countRarities(onColorCards), colorless, lands }
}

const MANA_COLOR_STYLES: Record<string, { bg: string; border: string }> = {
  W: { bg: 'rgba(248, 246, 216, 0.08)', border: 'rgba(248, 246, 216, 0.15)' },
  U: { bg: 'rgba(14, 104, 171, 0.12)', border: 'rgba(14, 104, 171, 0.25)' },
  B: { bg: 'rgba(100, 80, 70, 0.12)', border: 'rgba(100, 80, 70, 0.25)' },
  R: { bg: 'rgba(211, 32, 42, 0.1)', border: 'rgba(211, 32, 42, 0.2)' },
  G: { bg: 'rgba(0, 115, 62, 0.1)', border: 'rgba(0, 115, 62, 0.2)' },
}

function getArchetypeBorderColor(colors: readonly string[]): string {
  if (colors.length === 1 && colors[0]) {
    return MANA_COLOR_STYLES[colors[0]]?.border ?? 'rgba(255, 255, 255, 0.06)'
  }
  if (colors.length >= 3) {
    return 'rgba(252, 186, 3, 0.2)' // gold for 3+ colors
  }
  return 'rgba(255, 255, 255, 0.08)'
}

function getArchetypeBgColor(colors: readonly string[]): string {
  if (colors.length === 1 && colors[0]) {
    return MANA_COLOR_STYLES[colors[0]]?.bg ?? 'rgba(255, 255, 255, 0.02)'
  }
  if (colors.length >= 3) {
    return 'rgba(252, 186, 3, 0.04)'
  }
  return 'rgba(255, 255, 255, 0.02)'
}

const RARITY_COLORS: Record<string, string> = {
  mythic: '#ff8b00',
  rare: '#ffd700',
  uncommon: '#c0c0c0',
  common: '#888888',
}

function RarityBadge({ label, count, color }: { label: string; count: number; color: string }) {
  if (count === 0) return null
  return (
    <span
      style={{
        fontSize: 11,
        fontWeight: 600,
        color,
        letterSpacing: '0.02em',
      }}
      title={label}
    >
      {count}{label[0]}
    </span>
  )
}

function ArchetypeCard({ archetype, clickable, onClick, cardPool }: { archetype: Archetype; clickable?: boolean; onClick?: () => void; cardPool?: readonly SealedCardInfo[] }) {
  const rarities = useMemo(() => {
    if (!cardPool || cardPool.length === 0) return null
    return computeArchetypeRarities(cardPool, archetype)
  }, [cardPool, archetype])

  const creatureTypeCounts = useMemo(() => {
    if (!cardPool || cardPool.length === 0 || !archetype.creatureTypes || archetype.creatureTypes.length === 0) return null
    return countCreatureTypes(cardPool, archetype.creatureTypes)
  }, [cardPool, archetype])

  return (
    <div
      onClick={clickable ? onClick : undefined}
      style={{
        padding: '14px 16px',
        borderRadius: 8,
        backgroundColor: getArchetypeBgColor(archetype.colors),
        border: `1px solid ${getArchetypeBorderColor(archetype.colors)}`,
        transition: 'background-color 0.15s',
        cursor: clickable ? 'pointer' : undefined,
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginBottom: 6,
        }}
      >
        <div style={{ display: 'flex', gap: 3 }}>
          {archetype.colors.map((c) => (
            <ManaSymbol key={c} symbol={c} size={16} />
          ))}
        </div>
        <span
          style={{
            fontSize: 15,
            fontWeight: 700,
            color: '#fff',
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
          }}
        >
          {archetype.name}
        </span>
        {creatureTypeCounts && (
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            {[...creatureTypeCounts.entries()].map(([type, count]) => (
              <span
                key={type}
                style={{
                  fontSize: 11,
                  fontWeight: 600,
                  color: 'rgba(255, 255, 255, 0.55)',
                  backgroundColor: 'rgba(255, 255, 255, 0.06)',
                  padding: '1px 6px',
                  borderRadius: 4,
                }}
                title={`${count} ${type}${count !== 1 ? 's' : ''} in pool`}
              >
                {count} {type}{count !== 1 ? 's' : ''}
              </span>
            ))}
          </div>
        )}
        {rarities && (
          <div
            style={{
              marginLeft: 'auto',
              display: 'flex',
              gap: 8,
              alignItems: 'center',
            }}
          >
            <RarityBadge label="Mythic" count={rarities.onColor.mythic} color={RARITY_COLORS.mythic!} />
            <RarityBadge label="Rare" count={rarities.onColor.rare} color={RARITY_COLORS.rare!} />
            <RarityBadge label="Uncommon" count={rarities.onColor.uncommon} color={RARITY_COLORS.uncommon!} />
            <RarityBadge label="Common" count={rarities.onColor.common} color={RARITY_COLORS.common!} />
            {rarities.colorless > 0 && (
              <span style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.5)' }} title="Colorless">
                {rarities.colorless}A
              </span>
            )}
            {rarities.lands > 0 && (
              <span style={{ fontSize: 11, fontWeight: 600, color: '#8d6e4a' }} title="Lands">
                {rarities.lands}L
              </span>
            )}
            <span style={{ fontSize: 11, fontWeight: 500, color: 'rgba(255,255,255,0.35)', marginLeft: 2 }}>
              ({rarities.onColor.total + rarities.colorless + rarities.lands})
            </span>
          </div>
        )}
      </div>
      <p
        style={{
          margin: 0,
          color: 'rgba(255, 255, 255, 0.6)',
          fontSize: 13,
          lineHeight: 1.5,
        }}
      >
        {archetype.description}
      </p>
    </div>
  )
}
