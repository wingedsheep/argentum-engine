import { useState, useMemo } from 'react'
import { ManaSymbol } from '../ui/ManaSymbols'

/**
 * Archetype definition for a set's draft strategy.
 */
interface Archetype {
  readonly name: string
  /** Mana color symbols, e.g. ['W', 'U'] */
  readonly colors: readonly string[]
  readonly description: string
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
        description: 'Attach auras to Wizards for repeatable removal, and use shapeshifters for extra tribal synergy. A spell-based control deck.',
      },
      {
        name: 'Clerics',
        colors: ['W', 'B'],
        description: 'White Clerics defend and heal while black Clerics drain life. The tribe rewards a slow, grinding game plan built on incremental advantages.',
      },
      {
        name: 'Zombies',
        colors: ['B'],
        description: 'Recursive threats and strong removal fuel an attrition-based strategy that grinds opponents down relentlessly.',
      },
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        description: 'Red aggression paired with black removal. Goblin tribal synergies create explosive starts backed by efficient creature destruction.',
      },
      {
        name: 'Elves',
        colors: ['G'],
        description: 'Elves generate mana, gain life, and pump each other. The more Elves you draft, the stronger every individual Elf becomes.',
      },
      {
        name: 'Beasts',
        colors: ['R', 'G'],
        description: 'Green\'s large beasts combined with red removal create a midrange deck that wins through creature quality and raw combat dominance.',
      },
      {
        name: 'Soldiers / Flyers',
        colors: ['W', 'U'],
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
        description: 'Soldiers and Birds combine for a tempo-evasion strategy. Fly over stalled boards while tribal lords pump your ground forces.',
      },
      {
        name: 'Clerics',
        colors: ['W', 'B'],
        description: 'The cleric synergy engine continues. In an all-creature set with no removal spells, the lifegain/drain plan is even more dominant.',
      },
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        description: 'Morph Goblins provide rare removal in a removal-starved set. Amplify lords reward drafting Goblins aggressively for explosive starts.',
      },
      {
        name: 'Elves',
        colors: ['G', 'W'],
        description: 'Elf lords that tap to pump make every Elf a threat. Amplify and provoke create an engine of growth that overwhelms with sheer numbers.',
      },
      {
        name: 'Zombies',
        colors: ['U', 'B'],
        description: 'Zombie tribal mixed with blue evasion. Morph creatures provide tempo plays with face-down threats that flip into value.',
      },
      {
        name: 'Slivers',
        colors: ['W', 'U', 'B', 'R', 'G'],
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
        description: 'White landcyclers ensure land drops while black provides efficient removal. A solid control strategy.',
      },
      {
        name: 'Wizards',
        colors: ['U', 'R'],
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
        description: 'Dedicated Dragon tribal support rewards these devastating flying threats. High mana costs are a feature, not a bug.',
      },
      {
        name: 'Landcycling',
        colors: ['W', 'G'],
        description: 'Landcycling creatures fix your mana early and provide large bodies late. Smooths draws across multiple colors.',
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
 * Button that opens the set synergies overlay.
 * Renders nothing if no synergy data is available for the given sets.
 */
export function SetSynergiesButton({
  setCodes,
  style,
}: {
  setCodes: readonly string[]
  style?: React.CSSProperties
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
}: {
  synergies: SetSynergies[]
  onClose: () => void
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
              <ArchetypeCard key={archetype.name} archetype={archetype} />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
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

function ArchetypeCard({ archetype }: { archetype: Archetype }) {
  return (
    <div
      style={{
        padding: '14px 16px',
        borderRadius: 8,
        backgroundColor: getArchetypeBgColor(archetype.colors),
        border: `1px solid ${getArchetypeBorderColor(archetype.colors)}`,
        transition: 'background-color 0.15s',
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
