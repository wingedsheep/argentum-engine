import { useState, useMemo } from 'react'
import type { SealedCardInfo } from '@/types'
import { ManaSymbol } from '../ui/ManaSymbols'
import { getCdnArtCropUrl, getScryfallArtCropUrl } from '@/utils/cardImages'

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
  /**
   * Exact name of a card that represents this archetype. Its Scryfall art crop
   * is painted as the tile backdrop. Omit (or use a card that fails to resolve)
   * to fall back to a color-identity gradient.
   */
  readonly keyCard?: string
}

interface SetSynergies {
  readonly setCode: string
  readonly setName: string
  readonly archetypes: readonly Archetype[]
}

/**
 * Static synergy data for each supported set.
 *
 * Trimmed to the defining archetypes per set — kept in lockstep with the AI
 * deckbuilder's `SetArchetypes.kt`. `keyCard` only names a card whose exact
 * Scryfall name is known and representative; otherwise the tile uses a gradient.
 */
const SET_SYNERGIES: Record<string, SetSynergies> = {
  POR: {
    setCode: 'POR',
    setName: 'Portal',
    archetypes: [
      {
        name: 'White Weenie',
        colors: ['W'],
        keyCard: 'Charging Paladin',
        description: 'Small efficient creatures with combat bonuses. Go wide with cheap soldiers and pump effects to overwhelm before your opponent stabilizes.',
      },
      {
        name: 'Blue Flyers',
        colors: ['U'],
        keyCard: 'Storm Crow',
        description: 'Evasive flying creatures backed by card draw sorceries. A tempo-oriented strategy that wins in the air.',
      },
      {
        name: 'Black Control',
        colors: ['B'],
        keyCard: 'Cackling Fiend',
        description: 'Creature removal sorceries and drain effects paired with midrange creatures. Grind opponents out with efficient answers.',
      },
      {
        name: 'Red Aggro',
        colors: ['R'],
        keyCard: 'Anaba Bodyguard',
        description: 'Aggressive cheap creatures and direct damage sorceries to burn the opponent out before they can set up defenses.',
      },
      {
        name: 'Green Stompy',
        colors: ['G'],
        keyCard: 'Spined Wurm',
        description: 'Large efficient creatures that overpower opponents through raw stats and size. Simple but effective.',
      },
    ],
  },
  ONS: {
    setCode: 'ONS',
    setName: 'Onslaught',
    archetypes: [
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        keyCard: 'Sparksmith',
        description: 'Red aggression paired with black removal. Goblin tribal synergies create explosive starts backed by efficient creature destruction.',
      },
      {
        name: 'Clerics',
        colors: ['W', 'B'],
        creatureTypes: ['Cleric'],
        keyCard: 'Battlefield Medic',
        description: 'White Clerics defend and heal while black Clerics drain life. The tribe rewards a slow, grinding game plan built on incremental advantages.',
      },
      {
        name: 'Beasts',
        colors: ['R', 'G'],
        creatureTypes: ['Beast'],
        keyCard: 'Krosan Tusker',
        description: 'Green\'s large beasts combined with red removal create a midrange deck that wins through creature quality and raw combat dominance.',
      },
      {
        name: 'Elves',
        colors: ['G'],
        creatureTypes: ['Elf'],
        keyCard: 'Timberwatch Elf',
        description: 'Elves generate mana, gain life, and pump each other. The more Elves you draft, the stronger every individual Elf becomes.',
      },
      {
        name: 'Soldiers / Flyers',
        colors: ['W', 'U'],
        creatureTypes: ['Soldier', 'Bird'],
        keyCard: 'Gustcloak Harrier',
        description: 'Cheap evasive flyers and Soldier tribal put opponents on a fast clock. A tempo-aggro deck that races on the ground and in the air.',
      },
      {
        name: 'Morph',
        colors: ['U', 'G'],
        keyCard: 'Willbender',
        description: 'Face-down creatures conceal deadly surprises. Keep your opponent guessing while you set up devastating flip triggers and tempo plays.',
      },
    ],
  },
  LGN: {
    setCode: 'LGN',
    setName: 'Legions',
    archetypes: [
      {
        name: 'Slivers',
        colors: ['W', 'U', 'B', 'R', 'G'],
        creatureTypes: ['Sliver'],
        keyCard: 'Ward Sliver',
        description: 'Every Sliver shares its abilities with all others. A risky but powerful tribal strategy — if the pieces come together, the hive is unstoppable.',
      },
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        keyCard: 'Gempalm Incinerator',
        description: 'Morph Goblins provide rare removal in a removal-starved set. Amplify lords reward drafting Goblins aggressively for explosive starts.',
      },
      {
        name: 'Soldiers / Flyers',
        colors: ['W', 'U'],
        creatureTypes: ['Soldier', 'Bird'],
        keyCard: 'Gempalm Avenger',
        description: 'Soldiers and Birds combine for a tempo-evasion strategy. Fly over stalled boards while tribal lords pump your ground forces.',
      },
      {
        name: 'Clerics',
        colors: ['W', 'B'],
        creatureTypes: ['Cleric'],
        keyCard: 'Daru Spiritualist',
        description: 'The cleric synergy engine continues. In an all-creature set with no removal spells, the lifegain/drain plan is even more dominant.',
      },
      {
        name: 'Elves',
        colors: ['G', 'W'],
        creatureTypes: ['Elf'],
        keyCard: 'Wirewood Channeler',
        description: 'Elf lords that tap to pump make every Elf a threat. Amplify and provoke create an engine of growth that overwhelms with sheer numbers.',
      },
    ],
  },
  SCG: {
    setCode: 'SCG',
    setName: 'Scourge',
    archetypes: [
      {
        name: 'Dragons',
        colors: ['R'],
        creatureTypes: ['Dragon'],
        keyCard: 'Rorix Bladewing',
        description: 'Dedicated Dragon tribal support rewards these devastating flying threats. High mana costs are a feature, not a bug.',
      },
      {
        name: 'Goblins',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        keyCard: 'Goblin Warchief',
        description: 'Black removal plus Goblin tribal synergies. Storm spells can steal games out of nowhere.',
      },
      {
        name: 'Wizards',
        colors: ['U', 'R'],
        creatureTypes: ['Wizard'],
        keyCard: 'Mistform Ultimus',
        description: 'Draw cards proportional to your highest mana cost for massive card advantage. Wizard synergies from the block still anchor the deck.',
      },
      {
        name: 'Landcycling',
        colors: ['W', 'G'],
        keyCard: 'Eternal Dragon',
        description: 'Landcycling creatures fix your mana early and provide large bodies late. Smooths draws across multiple colors.',
      },
      {
        name: 'Ramp',
        colors: ['U', 'G'],
        keyCard: 'Root Elemental',
        description: 'Ramp into large green creatures, then leverage their high mana cost for powerful draw spells. Bury opponents in card advantage.',
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
        keyCard: "Raff Capashen, Ship's Mage",
        description: 'Evasive flying creatures backed by historic synergies. Artifacts, legendaries, and Sagas trigger payoffs while your flyers close the game in the air.',
      },
      {
        name: 'Historic Control',
        colors: ['U', 'B'],
        keyCard: "Chainer's Torment",
        description: 'Grind opponents out with Sagas, removal, and card advantage. Sagas schedule value over three turns while efficient answers keep the board in check.',
      },
      {
        name: 'Reckless Aggro',
        colors: ['B', 'R'],
        keyCard: 'Goblin Chainwhirler',
        description: 'Fast creatures with first strike and haste backed by sacrifice synergies. Tokens serve as expendable resources while you race to close the game.',
      },
      {
        name: 'Kicker Ramp',
        colors: ['R', 'G'],
        keyCard: 'Verix Bladewing',
        description: 'Ramp into large creatures and leverage kicker for late-game flexibility. Cards scale from on-curve plays to devastating threats when you have extra mana.',
      },
      {
        name: 'Saproling Sacrifice',
        colors: ['B', 'G'],
        keyCard: 'Slimefoot, the Stowaway',
        description: 'Generate Saprolings and sacrifice them for value. Thallids and fungus creatures create a self-sustaining engine of tokens and drain effects.',
      },
      {
        name: 'Wizard Spells',
        colors: ['U', 'R'],
        creatureTypes: ['Wizard'],
        keyCard: 'Adeliz, the Cinder Wind',
        description: 'Wizards grow stronger with every instant and sorcery you cast. Build a critical mass of spells and Wizards for explosive prowess-style turns.',
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
        keyCard: 'Anafenza, the Foremost',
        description: 'Outlast your creatures to grow +1/+1 counters, then share keywords like flying and lifelink across your bolstered army. A grindy, resilient midrange strategy.',
      },
      {
        name: 'Jeskai',
        colors: ['U', 'R', 'W'],
        keyCard: 'Narset, Enlightened Master',
        description: 'Cast noncreature spells to trigger prowess, turning every instant and sorcery into a combat trick. Combines card selection, burn, and tempo.',
      },
      {
        name: 'Sultai',
        colors: ['B', 'G', 'U'],
        keyCard: 'Sidisi, Brood Tyrant',
        description: 'Fill your graveyard to fuel powerful delve spells at reduced cost. A midrange-to-control strategy built on exploiting the graveyard as a resource.',
      },
      {
        name: 'Mardu',
        colors: ['R', 'W', 'B'],
        keyCard: 'Butcher of the Horde',
        description: 'Attack each turn to trigger raid bonuses. Wide board presence with tokens and warriors, backed by removal to clear blockers.',
      },
      {
        name: 'Temur',
        colors: ['G', 'U', 'R'],
        keyCard: 'Savage Knuckleblade',
        description: 'Ferocious abilities activate when you control a creature with power 4 or greater. Ramp into the biggest threats in the format.',
      },
    ],
  },
  TDM: {
    setCode: 'TDM',
    setName: 'Tarkir: Dragonstorm',
    archetypes: [
      {
        name: 'Abzan',
        colors: ['W', 'B', 'G'],
        keyCard: 'Felothar, Dawn of the Abzan',
        description: 'Endure pads your board with +1/+1 counters or Spirit tokens. A resilient counters-matter midrange that grows wider and taller every turn and refuses to stay down.',
      },
      {
        name: 'Jeskai',
        colors: ['U', 'R', 'W'],
        keyCard: 'Narset, Jeskai Waymaster',
        description: 'Cast your second spell each turn to trigger flurry, turning a steady stream of cheap instants and sorceries into damage, tokens, and card advantage. A snowballing tempo deck.',
      },
      {
        name: 'Sultai',
        colors: ['B', 'G', 'U'],
        keyCard: 'Kotis, the Fangkeeper',
        description: 'Fill your graveyard, then renew creatures from it to deal out +1/+1 counters and refill your hand. A grindy graveyard-value midrange that wins the long game.',
      },
      {
        name: 'Mardu',
        colors: ['R', 'W', 'B'],
        creatureTypes: ['Warrior'],
        keyCard: "Zurgo, Thunder's Decree",
        description: 'Mobilize creates temporary Warrior tokens every time you attack. Go wide, swing hard, and back the assault with removal to push through the last points of damage.',
      },
      {
        name: 'Temur',
        colors: ['G', 'U', 'R'],
        creatureTypes: ['Dragon'],
        keyCard: 'Eshki Dragonclaw',
        description: "Behold a Dragon to unlock discounts and bonuses, then ramp into the format's biggest fliers. A ferocious ramp-and-Dragons strategy.",
      },
    ],
  },
  BLB: {
    setCode: 'BLB',
    setName: 'Bloomburrow',
    archetypes: [
      {
        name: 'Bats',
        colors: ['W', 'B'],
        creatureTypes: ['Bat'],
        keyCard: 'Zoraline, Cosmos Caller',
        description: 'Use life as a resource — white gains it, black spends it. Bats reward you for both gaining and losing life, letting you flip between aggression and defense as needed.',
      },
      {
        name: 'Otters',
        colors: ['U', 'R'],
        creatureTypes: ['Otter'],
        keyCard: 'Stormsplitter',
        description: 'Cast a high volume of instants and sorceries to trigger Prowess on your Otter creatures. Classic Izzet spell-slinger that gets bigger the more spells you cast.',
      },
      {
        name: 'Frogs',
        colors: ['U', 'G'],
        creatureTypes: ['Frog'],
        keyCard: 'Helga, Skittish Seer',
        description: 'Value engine built around bouncing and blinking Frogs to re-trigger ETB effects. Mana-intensive but generates enormous card advantage in the late game.',
      },
      {
        name: 'Lizards',
        colors: ['B', 'R'],
        creatureTypes: ['Lizard'],
        keyCard: 'Gev, Scaled Scorch',
        description: 'Aggressive deck that rewards dealing damage to opponents and forcing life loss. Creatures gain bonuses when opponents lose life, combining relentless pressure with black removal.',
      },
      {
        name: 'Squirrels',
        colors: ['B', 'G'],
        creatureTypes: ['Squirrel'],
        keyCard: 'Camellia, the Seedmiser',
        description: 'Grindy midrange built around the Forage mechanic — sacrifice Food tokens or exile cards from graveyards for value. Squirrels synergize with food production and graveyard interactions.',
      },
      {
        name: 'Rabbits',
        colors: ['G', 'W'],
        creatureTypes: ['Rabbit'],
        keyCard: 'Finneas, Ace Archer',
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
        keyCard: "Brigid, Clachan's Heart",
        description: 'Flood the board with 1/1 Kithkin tokens, then use Convoke to cast expensive spells for free. Mass pump effects turn the token horde lethal.',
      },
      {
        name: 'Merfolk',
        colors: ['W', 'U'],
        creatureTypes: ['Merfolk'],
        keyCard: 'Sygg, Wanderwine Wisdom',
        description: 'Merfolk untap each other after combat and chain together card draw. Control the pace of the game with bounce and tempo plays while building an unstoppable Merrow tide.',
      },
      {
        name: 'Faeries',
        colors: ['U', 'B'],
        creatureTypes: ['Faerie'],
        keyCard: 'Maralen, Fae Ascendant',
        description: 'Operate entirely at flash speed — drop Faeries at end of turn, steal blockers, and lock out opponents with aerial superiority. A control deck that wins with evasion.',
      },
      {
        name: 'Boggarts',
        colors: ['B', 'R'],
        creatureTypes: ['Goblin'],
        keyCard: 'Grub, Storied Matriarch',
        description: 'Relentless Boggart aggro backed by Wither. Your attackers deal damage as -1/-1 counters, permanently shrinking blockers. Blight provides flexible sacrifice payoffs.',
      },
      {
        name: 'Elementals',
        colors: ['U', 'R'],
        creatureTypes: ['Elemental'],
        keyCard: 'Ashling, Rekindled',
        description: 'Flamekin Elementals reward casting instants and sorceries. Evoke provides flexible tempo plays — pay the cheap cost for a burst of value, or go full price for a permanent threat.',
      },
      {
        name: 'Changelings',
        colors: ['U', 'G'],
        creatureTypes: ['Shapeshifter'],
        keyCard: 'Omni-Changeling',
        description: 'Changelings count as every creature type simultaneously, triggering Kithkin, Merfolk, Elf, and Faerie payoffs all at once. A tribal swiss-army-knife deck with enormous flexibility.',
      },
    ],
  },
  EOE: {
    setCode: 'EOE',
    setName: 'Edge of Eternities',
    archetypes: [
      {
        name: 'Second Spell',
        colors: ['W', 'U'],
        keyCard: 'Space-Time Anomaly',
        description: 'Cast two spells in a single turn to fire off a stream of payoffs. Cheap interaction and artifacts keep the spells flowing while flyers and tokens reward every double-spell turn. A consistent tempo-value deck.',
      },
      {
        name: 'Sacrifice',
        colors: ['W', 'B'],
        keyCard: 'Syr Vondam, Sunstar Exemplar',
        description: 'Field a wide board of efficient creatures and feed them to sacrifice payoffs. Warp lets your threats land early and return later, fueling drain, removal, and relentless pressure.',
      },
      {
        name: 'Spacecraft',
        colors: ['W', 'R'],
        creatureTypes: ['Spacecraft'],
        keyCard: 'Sami, Wildcat Captain',
        description: 'Tap your creatures to station up Spacecraft into enormous artifact threats, then back the assault with aggressive bodies. Go fast on the ground while your ships charge toward liftoff.',
      },
      {
        name: 'Lander Ramp',
        colors: ['U', 'G'],
        keyCard: 'Genemorph Imago',
        description: "Crack Lander tokens for mana and fixing, ramping into the format's biggest threats. The premier two-for-one ramp deck that out-resources the table and splashes bombs.",
      },
      {
        name: 'Void',
        colors: ['B', 'R'],
        keyCard: 'Mutinous Massacre',
        description: 'Sacrifice your own creatures to switch on void payoffs, turning every permanent that dies into value. A grindy sacrifice-aggro deck that controls the board while bleeding the opponent.',
      },
      {
        name: 'Counters',
        colors: ['G', 'W'],
        keyCard: 'Dyadrine, Synthesis Amalgam',
        description: "Take the board early with cheap creatures and pile on +1/+1 counters. Station synergies and counter payoffs build a sticky, go-tall-and-wide deck that's hard to climb back against.",
      },
    ],
  },
  OTJ: {
    setCode: 'OTJ',
    setName: 'Outlaws of Thunder Junction',
    archetypes: [
      {
        name: 'Flash Plot',
        colors: ['W', 'U'],
        keyCard: 'Jem Lightfoote, Sky Explorer',
        description: "Plot cards on your turn, then deploy flash creatures and tricks on the opponent's. A tempo deck that always keeps mana up and threatens free spells from exile.",
      },
      {
        name: 'Crime',
        colors: ['U', 'B'],
        keyCard: 'Tinybones, the Pickpocket',
        description: 'Commit crimes — target opponents and their stuff — to snowball value and disruption. A tempo-control deck that drains the opponent while answering their threats.',
      },
      {
        name: 'Ramp Plot',
        colors: ['U', 'G'],
        keyCard: 'Bonny Pall, Clearcutter',
        description: "Ramp and discount plot cards to set up powerful future turns, then land the format's biggest threats ahead of schedule. A controlling midrange that out-values the table.",
      },
      {
        name: 'Mercenaries',
        colors: ['R', 'W'],
        creatureTypes: ['Mercenary'],
        keyCard: 'Taii Wakeen, Perfect Shot',
        description: 'Go wide with Mercenary tokens and aggressive bodies, then convert the swarm to damage with go-wide payoffs and mass pump. A fast, token-fueled aggro deck.',
      },
      {
        name: 'Outlaws',
        colors: ['B', 'R'],
        keyCard: 'Rakdos, the Muscle',
        description: 'The dedicated outlaw deck — Assassins, Mercenaries, Pirates, Rogues, and Warlocks trigger aggressive payoffs. Apply relentless pressure backed by burn and removal.',
      },
      {
        name: 'Mounts',
        colors: ['G', 'W'],
        creatureTypes: ['Mount'],
        keyCard: 'Miriam, Herd Whisperer',
        description: 'Saddle your Mounts to unlock powerful attack triggers, backed by removal like Throw from the Saddle. The format\'s premier midrange deck, going wide and tall at once.',
      },
    ],
  },
  DSK: {
    setCode: 'DSK',
    setName: 'Duskmourn: House of Horror',
    archetypes: [
      {
        name: 'Eerie Enchantments',
        colors: ['W', 'U'],
        keyCard: 'Overlord of the Mistmoors',
        description: 'Trigger Eerie by playing enchantments and unlocking Rooms. A tempo-value deck that builds an enchantment engine, churning out Glimmer tokens and incremental advantage while flyers close in the air.',
      },
      {
        name: 'Survival',
        colors: ['G', 'W'],
        keyCard: 'Overlord of the Hauntwoods',
        description: 'Keep creatures untapped through your second main phase to switch on Survival payoffs. A go-tall midrange that piles on +1/+1 counters and rewards a sturdy, defensive board.',
      },
      {
        name: 'Manifest Dread',
        colors: ['U', 'G'],
        keyCard: 'Overlord of the Floodpits',
        description: 'Manifest dread to flood the board with face-down 2/2s while stocking your graveyard. A value-midrange that converts card selection into board presence and creatures-entering payoffs.',
      },
      {
        name: 'Delirium',
        colors: ['B', 'G'],
        keyCard: 'Overlord of the Balemurk',
        description: 'Fill your graveyard with four or more card types to unlock Delirium bonuses. A grindy graveyard-value deck that mills, sacrifices, and recurs threats to out-attrition the table.',
      },
      {
        name: 'Sacrifice Aggro',
        colors: ['B', 'R'],
        keyCard: 'Unstoppable Slasher',
        description: 'Throw expendable creatures and tokens at sacrifice outlets for value and reach. An aggressive deck that drains the opponent and turns dying bodies into damage.',
      },
      {
        name: 'Impending',
        colors: ['R', 'G'],
        keyCard: 'Overlord of the Boilerbilges',
        description: "Deploy big threats early with Impending time counters, then ramp into the format's largest creatures. A midrange-ramp deck that overpowers opponents once its haymakers come online.",
      },
    ],
  },
  STX: {
    setCode: 'STX',
    setName: 'Strixhaven: School of Mages',
    archetypes: [
      {
        name: 'Silverquill',
        colors: ['W', 'B'],
        keyCard: 'Silverquill Pledgemage',
        description: 'Build a wide board of Inkling tokens, then pile on +1/+1 counters and push through with evasion and lifegain. An aggressive counters-and-tokens deck that closes the game fast.',
      },
      {
        name: 'Prismari',
        colors: ['U', 'R'],
        keyCard: 'Prismari Pledgemage',
        description: 'Cast a high volume of instants and sorceries to trigger magecraft, churning out Treasure and big Elemental payoffs. A spell-slinging tempo deck that snowballs with every cast.',
      },
      {
        name: 'Witherbloom',
        colors: ['B', 'G'],
        keyCard: 'Witherbloom Pledgemage',
        description: 'Drain the opponent with Pests and lifeloss payoffs, sacrificing tokens for value while recurring threats from the graveyard. A grindy attrition deck that bleeds the table dry.',
      },
      {
        name: 'Lorehold',
        colors: ['R', 'W'],
        keyCard: 'Lorehold Pledgemage',
        description: 'Recur instants and sorceries from your graveyard and reward casting historic spells, turning Spirit tokens and value loops into relentless pressure. A spells-matter midrange deck.',
      },
      {
        name: 'Quandrix',
        colors: ['G', 'U'],
        keyCard: 'Quandrix Pledgemage',
        description: 'Ramp into extra mana and pile +1/+1 counters onto Fractal tokens, scaling your board with the size of your mana. A go-big ramp-and-counters deck that overwhelms in the late game.',
      },
    ],
  },
  TMT: {
    setCode: 'TMT',
    setName: 'Teenage Mutant Ninja Turtles',
    archetypes: [
      {
        name: 'Blink Tempo',
        colors: ['W', 'U'],
        keyCard: 'Don & Leo, Problem Solvers',
        description: 'Bounce and blink your artifacts and creatures to re-trigger their enter-the-battlefield abilities, grinding out value while evasive Turtles and Robots seize the tempo. A patient value-control deck.',
      },
      {
        name: 'Mill Control',
        colors: ['U', 'B'],
        keyCard: 'Krang & Shredder',
        description: "Mill your opponents and steal their best cards, leaning on removal and evasion to control the game. A grindy control deck that wins by turning the opponent's own library against them.",
      },
      {
        name: 'Graveyard Value',
        colors: ['B', 'G'],
        keyCard: 'The Last Ronin',
        description: 'Fill your graveyard, then reanimate fallen creatures and pile on +1/+1 counters. A grindy recursion deck that out-attritions the table and simply refuses to stay dead.',
      },
      {
        name: 'Mutant Beatdown',
        colors: ['R', 'G'],
        keyCard: 'Raph & Mikey, Troublemakers',
        description: 'Deploy hasty, trampling threats and flood the board with 2/2 Mutant tokens, then swing with overwhelming force. A fast midrange-aggro deck that wins through raw board presence.',
      },
      {
        name: 'Alliance Aggro',
        colors: ['R', 'W'],
        keyCard: 'Raph & Leo, Sibling Rivals',
        description: 'Go wide so every creature that enters triggers Alliance, then take extra combat phases to swing again and again. An aggressive go-wide deck built on relentless attacks.',
      },
      {
        name: 'Mutant Ramp',
        colors: ['G', 'U'],
        keyCard: 'Mikey & Don, Party Planners',
        description: 'Ramp and dig to cast Mutant, Ninja, and Turtle creatures from the top of your library, each entering with a bonus +1/+1 counter. A creature-type value deck that snowballs card advantage.',
      },
    ],
  },
  INV: {
    setCode: 'INV',
    setName: 'Invasion',
    archetypes: [
      {
        name: 'Bird Tempo',
        colors: ['W', 'U'],
        keyCard: 'Kangee, Aerie Keeper',
        description: 'Take to the skies with evasive fliers and kicker-pumped Birds, riding tempo to victory. A flier-centric deck where kicker turns your lord into a finisher.',
      },
      {
        name: 'Tempo Control',
        colors: ['U', 'B'],
        keyCard: 'Undermine',
        description: 'Counter spells and bounce threats while bleeding the opponent with discard. A tempo-control deck that answers everything and grinds out the long game.',
      },
      {
        name: 'Sacrifice Discard',
        colors: ['B', 'R'],
        keyCard: 'Blazing Specter',
        description: 'Strip the opponent\'s hand with discard and evasive damage, then turn dying creatures into reanimation value. An aggressive disruption deck that attacks resources and life total at once.',
      },
      {
        name: 'Kicker Aggro',
        colors: ['R', 'G'],
        keyCard: 'Fires of Yavimaya',
        description: 'Give your creatures haste and ramp into kicker payoffs, swinging the moment threats hit the board. A fast aggro-ramp deck powered by Kavu and big kicked spells.',
      },
      {
        name: 'Aura Legends',
        colors: ['G', 'W'],
        keyCard: 'Captain Sisay',
        description: 'Suit up a creature with powerful Auras like Armadillo Cloak and tutor up your best legends. A midrange deck that goes tall and grinds out incremental value.',
      },
      {
        name: 'Domain',
        colors: ['W', 'U', 'B', 'R', 'G'],
        keyCard: 'Darigaaz, the Igniter',
        description: "Splash every color to switch on domain and unleash the Coalition Dragons, the format's premier finishers. A greedy five-color goodstuff deck built on mana fixing.",
      },
    ],
  },
  LTR: {
    setCode: 'LTR',
    setName: 'The Lord of the Rings: Tales of Middle-earth',
    archetypes: [
      {
        name: 'Spirits',
        colors: ['W', 'B'],
        keyCard: 'King of the Oathbreakers',
        description: 'Phase your Spirits out of removal and back in to spawn flying tokens. A resilient tempo deck that protects its threats and wins through the air.',
      },
      {
        name: 'Spellslinger',
        colors: ['U', 'R'],
        keyCard: 'Gandalf the Grey',
        description: 'Cast a high volume of instants and sorceries to trigger modal payoffs and recur your spells. A spell-matters tempo-control deck that out-values the table.',
      },
      {
        name: 'Orc Army',
        colors: ['B', 'R'],
        keyCard: 'Mauhúr, Uruk-hai Captain',
        description: 'Amass Orc Army and pump your token horde, where every +1/+1 counter lands amplified. A go-wide aggro deck backed by menace and removal that swarms the board.',
      },
      {
        name: 'Food Sacrifice',
        colors: ['B', 'G'],
        keyCard: 'Shelob, Child of Ungoliant',
        description: 'Turn dying creatures and Spider combat into Food, then sacrifice it for value and reach. A grindy graveyard-and-Food midrange deck that out-attritions the opponent.',
      },
      {
        name: 'Rohirrim Aggro',
        colors: ['R', 'W'],
        keyCard: 'Théoden, King of Rohan',
        description: 'Flood the board with aggressive Humans and grant double strike on entry, then swing for the win. A fast go-wide combat deck built on Rohirrim synergies.',
      },
      {
        name: 'Ring-bearers',
        colors: ['G', 'W'],
        keyCard: 'Frodo Baggins',
        description: 'Lean on legendary creatures and "the Ring tempts you" triggers, growing an evasive Ring-bearer into a game-ender. A midrange deck built on legends and steady Ring value.',
      },
    ],
  },
  SOS: {
    setCode: 'SOS',
    setName: 'Secrets of Strixhaven',
    archetypes: [
      {
        name: 'Silverquill',
        colors: ['W', 'B'],
        keyCard: 'Silverquill, the Disputant',
        description: 'Sling instants and sorceries with casualty, sacrificing a creature to copy each spell while evasive fliers carry the game. A spell-and-sacrifice tempo deck that doubles up on its best cards.',
      },
      {
        name: 'Prismari',
        colors: ['U', 'R'],
        keyCard: 'Prismari, the Inspiration',
        description: 'Chain instants and sorceries to fuel storm, copying each spell for every one cast before it that turn. A spell-velocity tempo deck that snowballs into an explosive turn.',
      },
      {
        name: 'Witherbloom',
        colors: ['B', 'G'],
        keyCard: 'Witherbloom, the Balancer',
        description: 'Flood the board with creatures to power affinity, casting your spells at a discount while lifegain and death payoffs grind the opponent down. A creatures-and-spells midrange deck.',
      },
      {
        name: 'Lorehold',
        colors: ['R', 'W'],
        keyCard: 'Lorehold, the Historian',
        description: 'Give your instants and sorceries miracle and reward drawing them off the top, looting to dig for the perfect topdeck. A draw-matters tempo deck that casts spells at a steep discount.',
      },
      {
        name: 'Quandrix',
        colors: ['G', 'U'],
        keyCard: 'Quandrix, the Proof',
        description: 'Ramp into expensive instants and sorceries that cascade for free value, snowballing card advantage with Increment counters. A go-big spells-matter ramp deck.',
      },
    ],
  },
  FIN: {
    setCode: 'FIN',
    setName: 'Final Fantasy',
    archetypes: [
      {
        name: 'Artifacts',
        colors: ['W', 'U'],
        keyCard: 'Cid, Timeless Artificer',
        description: 'Flood the board with artifacts — Equipment, Treasures, and artifact creatures — then cash in go-wide payoffs while card filtering keeps the gas flowing. A tempo-value deck that snowballs as its trinkets pile up.',
      },
      {
        name: 'Sacrifice',
        colors: ['W', 'B'],
        keyCard: 'Judge Magister Gabranth',
        description: "Feed a steady stream of creatures and tokens to sacrifice outlets, wringing two-for-one value and drain out of everything that dies. A grindy attrition deck built on the set's villains.",
      },
      {
        name: 'Equipment Aggro',
        colors: ['W', 'R'],
        keyCard: 'Adelbert Steiner',
        description: "Suit up aggressive creatures with Equipment — much of it attached to a body already — and push damage through with combat keywords like double strike. A fast go-tall aggro deck that's hard to block profitably.",
      },
      {
        name: 'Go-Wide Tokens',
        colors: ['W', 'G'],
        keyCard: 'Rinoa Heartilly',
        description: 'Summons and lower-rarity Eikons flood the board with tokens, then mass pump and anthem effects close the game before attrition sets in. A go-wide aggro deck that ends games early.',
      },
      {
        name: 'Control',
        colors: ['U', 'B'],
        keyCard: "Jill, Shiva's Dominant",
        description: 'Prolong the game with removal and counters, recurring your spells with flashback before taking over with powerful late-game bombs. A grindy control deck that out-values the table.',
      },
      {
        name: 'Big Spells',
        colors: ['U', 'R'],
        keyCard: 'Tellah, Great Sage',
        description: 'Lean on expensive instants and sorceries, where spending four or more mana on a noncreature spell unlocks extra value. A spell-velocity tempo deck that rewards going big.',
      },
      {
        name: 'Towns Ramp',
        colors: ['U', 'G'],
        keyCard: 'Ignis Scientia',
        description: "Play Towns and other nonbasic lands for repeatable value, ramping into the format's largest threats. A two-for-one ramp deck that out-resources opponents in the late game.",
      },
      {
        name: 'Black Mages',
        colors: ['B', 'R'],
        creatureTypes: ['Wizard'],
        keyCard: 'Black Waltz No. 3',
        description: 'A cheap removal package spits out Wizard tokens that add reach and combat math, recreating the Black Mage dream of slinging elemental damage. An aggressive tempo deck backed by relentless burn.',
      },
      {
        name: 'Graveyard',
        colors: ['B', 'G'],
        keyCard: 'Jenova, Ancient Calamity',
        description: 'Fill your graveyard, then grind the game out with recursion and graveyard payoffs that flip into game-ending threats. A patient value-midrange that simply refuses to stay dead.',
      },
      {
        name: 'Landfall',
        colors: ['R', 'G'],
        keyCard: 'Sazh Katzroy',
        description: 'Trigger landfall every time a land enters, turning even late-game land draws into damage with angry Chocobos and big beaters. A ramp-and-aggro deck that overwhelms with raw board presence.',
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

/** Saturated identity colors for the fallback backdrop gradient. */
const COLOR_HEX: Record<string, string> = {
  W: '#d9d2a6',
  U: '#2f6fd0',
  B: '#4a4550',
  R: '#d6463f',
  G: '#2f9b50',
}

/**
 * A color-identity gradient used as the tile backdrop when no card art is
 * available (or the art crop fails to load). Mirrors the saved-deck banner look.
 */
function getArchetypeGradient(colors: readonly string[]): string {
  if (colors.length === 0) return 'linear-gradient(135deg, #2c2c38, #17171f)'
  if (colors.length === 1 && colors[0]) {
    const c = COLOR_HEX[colors[0]] ?? '#555'
    return `linear-gradient(135deg, ${c} 0%, #16161e 92%)`
  }
  const stops = colors.map((c, i) => {
    const hex = COLOR_HEX[c] ?? '#555'
    const pct = Math.round((i / (colors.length - 1)) * 100)
    return `${hex} ${pct}%`
  })
  return `linear-gradient(135deg, ${stops.join(', ')})`
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

/**
 * A single archetype rendered as a polished, full-bleed banner: a representative
 * card's art crop as the backdrop, a scrim for legibility, and the name, color
 * pips, description, and (when a pool is supplied) rarity/creature-type counts
 * laid over it. Falls back to a color-identity gradient when no art is available.
 */
function ArchetypeCard({
  archetype,
  clickable,
  onClick,
  cardPool,
}: {
  archetype: Archetype
  clickable?: boolean
  onClick?: () => void
  cardPool?: readonly SealedCardInfo[]
}) {
  const [artFailed, setArtFailed] = useState(false)
  const [hovered, setHovered] = useState(false)

  // Prefer a direct CDN art crop derived from the key card's own image URL when that card
  // is in the supplied pool — it skips the rate-limited api.scryfall.com lookup + redirect.
  // Otherwise fall back to the by-name API lookup (the key card often isn't in the pool).
  const artUrl = useMemo(() => {
    if (!archetype.keyCard) return null
    const poolMatch = cardPool?.find((c) => c.name === archetype.keyCard)
    return getCdnArtCropUrl(poolMatch?.imageUri) ?? getScryfallArtCropUrl(archetype.keyCard)
  }, [archetype.keyCard, cardPool])
  const showArt = artUrl != null && !artFailed

  const rarities = useMemo(() => {
    if (!cardPool || cardPool.length === 0) return null
    return computeArchetypeRarities(cardPool, archetype)
  }, [cardPool, archetype])

  const creatureTypeCounts = useMemo(() => {
    if (!cardPool || cardPool.length === 0 || !archetype.creatureTypes || archetype.creatureTypes.length === 0) return null
    return countCreatureTypes(cardPool, archetype.creatureTypes)
  }, [cardPool, archetype])

  const borderColor = getArchetypeBorderColor(archetype.colors)

  return (
    <div
      onClick={clickable ? onClick : undefined}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        position: 'relative',
        borderRadius: 10,
        overflow: 'hidden',
        minHeight: 148,
        border: `1px solid ${hovered && clickable ? 'rgba(124, 58, 237, 0.65)' : borderColor}`,
        boxShadow: hovered
          ? '0 12px 28px rgba(0, 0, 0, 0.55)'
          : '0 1px 3px rgba(0, 0, 0, 0.45)',
        cursor: clickable ? 'pointer' : 'default',
        transform: hovered && clickable ? 'translateY(-2px)' : 'none',
        transition: 'box-shadow 0.18s ease, border-color 0.18s ease, transform 0.18s ease',
        isolation: 'isolate',
      }}
    >
      {/* Backdrop: color-identity gradient, also the art fallback */}
      <div
        aria-hidden
        style={{ position: 'absolute', inset: 0, background: getArchetypeGradient(archetype.colors) }}
      />

      {/* Backdrop: representative card art crop */}
      {showArt && (
        <img
          src={artUrl}
          alt=""
          aria-hidden
          onError={() => setArtFailed(true)}
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            objectPosition: 'center 22%',
            transform: hovered ? 'scale(1.06)' : 'scale(1)',
            transition: 'transform 0.4s ease',
          }}
        />
      )}

      {/* Scrims: darken left (text) and bottom for legibility */}
      <div
        aria-hidden
        style={{
          position: 'absolute',
          inset: 0,
          background:
            'linear-gradient(90deg, rgba(10, 10, 15, 0.94) 0%, rgba(10, 10, 15, 0.85) 34%, rgba(10, 10, 15, 0.5) 66%, rgba(10, 10, 15, 0.22) 100%)',
        }}
      />
      <div
        aria-hidden
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(0deg, rgba(10, 10, 15, 0.55) 0%, rgba(10, 10, 15, 0) 55%)',
        }}
      />

      {/* Content */}
      <div
        style={{
          position: 'relative',
          zIndex: 2,
          minHeight: 148,
          boxSizing: 'border-box',
          padding: '13px 16px',
          display: 'flex',
          flexDirection: 'column',
          gap: 7,
          justifyContent: 'center',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ display: 'flex', gap: 3, filter: 'drop-shadow(0 1px 2px rgba(0, 0, 0, 0.85))' }}>
            {archetype.colors.map((c) => (
              <ManaSymbol key={c} symbol={c} size={16} />
            ))}
          </div>
          <span
            style={{
              fontSize: 15,
              fontWeight: 800,
              color: '#fff',
              textTransform: 'uppercase',
              letterSpacing: '0.04em',
              textShadow: '0 1px 3px rgba(0, 0, 0, 0.95)',
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
                    color: 'rgba(255, 255, 255, 0.8)',
                    backgroundColor: 'rgba(0, 0, 0, 0.5)',
                    padding: '1px 6px',
                    borderRadius: 4,
                    backdropFilter: 'blur(2px)',
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
                backgroundColor: 'rgba(6, 6, 10, 0.62)',
                borderRadius: 6,
                padding: '2px 8px',
                backdropFilter: 'blur(3px)',
                textShadow: '0 1px 2px rgba(0, 0, 0, 0.95)',
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
                <span style={{ fontSize: 11, fontWeight: 600, color: '#d2a878' }} title="Lands">
                  {rarities.lands}L
                </span>
              )}
              <span style={{ fontSize: 11, fontWeight: 500, color: 'rgba(255,255,255,0.55)', marginLeft: 2 }}>
                ({rarities.onColor.total + rarities.colorless + rarities.lands})
              </span>
            </div>
          )}
        </div>
        <p
          style={{
            margin: 0,
            maxWidth: '68%',
            color: 'rgba(255, 255, 255, 0.85)',
            fontSize: 13,
            lineHeight: 1.45,
            textShadow: '0 1px 3px rgba(0, 0, 0, 0.95)',
          }}
        >
          {archetype.description}
        </p>
      </div>
    </div>
  )
}
