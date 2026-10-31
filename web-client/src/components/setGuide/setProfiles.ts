/**
 * The Set guide's hand-written profile of every fully playable set: what the set is, how its
 * Limited games play, and three ratings to compare sets by.
 *
 * Every set that is sealed-supported and complete (`fullyImplemented` on the server) needs an entry
 * here — `SetGuideProfileCoverageTest` in `:mtg-sets` fails otherwise — so the PR that finishes a set
 * adds its profile alongside its "What's new" announcement.
 *
 * The ratings are relative to the other sets on this list, not to Magic as a whole, and describe
 * the set as a sealed/draft environment:
 *  - `power`      — how strong the cards are. 1 is Portal's gentle vanilla creatures, 3 a typical
 *                   modern set, 5 Alpha's Power Nine or Modern Horizons 3.
 *  - `speed`      — how fast games end. 1 is board stalls broken by bombs, 5 races decided by turn six.
 *  - `complexity` — how much there is to track. 1 suits a first draft, 5 is several new mechanics
 *                   interacting on every card.
 *
 * Archetypes are not repeated here: the guide reads them from `SetSynergiesOverlay`'s data, the
 * same one the draft and deckbuilding screens show.
 */

export type Rating = 1 | 2 | 3 | 4 | 5

export type SetTheme =
  | 'aggro'
  | 'flyers'
  | 'tribal'
  | 'graveyard'
  | 'spells'
  | 'artifacts'
  | 'tokens'
  | 'counters'
  | 'sacrifice'
  | 'multicolor'
  | 'lifegain'
  | 'bigThings'
  | 'legends'

/** Theme chips in display order. */
export const SET_THEMES: readonly { readonly id: SetTheme; readonly label: string }[] = [
  { id: 'aggro', label: 'Aggro' },
  { id: 'flyers', label: 'Fliers' },
  { id: 'tribal', label: 'Creature types' },
  { id: 'tokens', label: 'Go wide' },
  { id: 'counters', label: 'Counters' },
  { id: 'graveyard', label: 'Graveyard' },
  { id: 'spells', label: 'Spells' },
  { id: 'artifacts', label: 'Artifacts' },
  { id: 'sacrifice', label: 'Sacrifice' },
  { id: 'multicolor', label: 'Multicolour' },
  { id: 'lifegain', label: 'Life gain' },
  { id: 'bigThings', label: 'Ramp & big threats' },
  { id: 'legends', label: 'Legends' },
]

export interface SetProfile {
  /**
   * A short summary for someone choosing a set: what it is about, how it plays and what makes it
   * special. Shown on its tile, in the lobby's set picker and under the play screen's set choice.
   */
  readonly tagline: string
  /** The world and what the set is about. */
  readonly about: string
  /** How a sealed or draft game of it plays. */
  readonly playstyle: string
  /** Completes "Try it if you like …". */
  readonly tryIf: string
  readonly power: Rating
  readonly speed: Rating
  readonly complexity: Rating
  readonly themes: readonly SetTheme[]
  /** The set's signature mechanics, most important first. */
  readonly mechanics: readonly string[]
}

export const SET_PROFILES: Readonly<Record<string, SetProfile>> = {
  LEA: {
    tagline: 'The original 1993 game in its rawest form. Mostly simple creatures and burn, slow-ish games, and a few absurdly strong rares that win whoever opens them.',
    about: 'The first Magic set ever printed: no planes, no story, just the original five colours and the cards every later set was measured against.',
    playstyle: 'Wildly uneven. Most cards are plain, a handful are the strongest ever made, and whoever opens Mox, Time Walk or Wrath of God plays a different game from everyone else.',
    tryIf: 'history, raw power and the original rules quirks',
    power: 5, speed: 2, complexity: 2,
    themes: ['bigThings', 'flyers'],
    mechanics: ['Power Nine', 'Regeneration', 'Banding', 'Dual lands'],
  },
  LEB: {
    tagline: 'Alpha\'s second print run: the original 1993 game. Simple creatures and burn, slow-ish games, and swingy rares like Serra Angel and Shivan Dragon that decide a lot of them.',
    about: "Beta fixed Alpha's misprints and added the missing Circle of Protection: Black and Volcanic Island. The card pool is otherwise the original 1993 set.",
    playstyle: 'Just like Alpha: simple creatures and burn punctuated by game-breaking rares. Removal is precious and Serra Angel or Shivan Dragon decides a lot of games.',
    tryIf: 'history, raw power and the original rules quirks',
    power: 5, speed: 2, complexity: 2,
    themes: ['bigThings', 'flyers'],
    mechanics: ['Power Nine', 'Regeneration', 'Banding', 'Dual lands'],
  },
  ARN: {
    tagline: 'A small Arabian Nights set of Djinn, Efreet and flying carpets. Evasive, efficient creatures carry the games; the colours are uneven, so draft what the packs give you.',
    about: "Magic's first expansion, drawn from the Arabian Nights tales: genies, sultans, magic carpets and Library of Alexandria.",
    playstyle: 'A small, odd pool with no basic lands of its own. Efficient evasive Djinn and Efreet carry the games, and the colour balance is uneven, so read the packs as they come.',
    tryIf: 'oddball old-school cards and evasive beaters',
    power: 3, speed: 3, complexity: 2,
    themes: ['flyers'],
    mechanics: ['Djinn & Efreet', 'Lands with abilities', 'Drawbacks for power'],
  },
  ATQ: {
    tagline: 'Urza and Mishra\'s artifact war. Slow, board-stall games where colourless artifact creatures fit any deck and assembling the Urza lands powers out the biggest threats.',
    about: 'The Brothers\' War as it was first told: an artifact-heavy expansion where colourless machines, Urza\'s Tower and Mishra\'s Factory define the set.',
    playstyle: 'Slow and artifact-driven. Colourless creatures go in any deck, artifact hate punishes them, and assembling the Urza lands powers out the biggest threats.',
    tryIf: 'artifacts and colourless decks',
    power: 3, speed: 1, complexity: 3,
    themes: ['artifacts', 'bigThings'],
    mechanics: ['Artifact creatures', "Urza's lands", 'Artifact hate'],
  },
  LEG: {
    tagline: 'The first legends and gold cards. Slow, grindy games with old-school banding combat, decided by big multicolour legends once they land.',
    about: 'Legends introduced legendary creatures and multicolour gold cards: Elder Dragons, wandering heroes and a famously powerful rare sheet.',
    playstyle: 'Slow and grindy, with banding combat that rewards careful attacks and big multicolour legends that win once they land.',
    tryIf: 'legendary creatures and old-school gold cards',
    power: 3, speed: 1, complexity: 4,
    themes: ['legends', 'multicolor', 'bigThings'],
    mechanics: ['Legendary creatures', 'Banding', 'Rampage', 'Gold cards'],
  },
  DRK: {
    tagline: 'A gloomy, low-power gothic set. Quick, scrappy creature fights where small Dwarf and Goblin tribes and cheap tricks decide who wins the board.',
    about: "The Dark is set in Dominaria's dark age: witch hunts, fungus, Dwarves and Goblins, with many cards that hurt their own controller.",
    playstyle: 'Low power and grindy-but-quick creature combat. Small tribal payoffs for Dwarves and Goblins and cheap tricks decide who wins the board.',
    tryIf: 'old-school flavour and low-power creature battles',
    power: 2, speed: 3, complexity: 2,
    themes: ['tribal'],
    mechanics: ['Dwarves & Goblins', 'Self-damaging spells', 'Fungus'],
  },
  FEM: {
    tagline: 'A dying world of fungus and Thrulls. Low power and grindy: grow Saproling tokens, sacrifice creatures for value, and outlast your opponent.',
    about: 'Fallen Empires follows the collapse of the Sarpadian empires, with each colour fielding a tribe that makes or sacrifices its own creatures.',
    playstyle: 'A low-power token engine format: grow Thallid spores into Saprolings, sacrifice Thrulls for value, and grind out tribal boards.',
    tryIf: 'tribal tokens and sacrifice engines',
    power: 2, speed: 3, complexity: 2,
    themes: ['tribal', 'tokens', 'sacrifice'],
    mechanics: ['Spore counters', 'Saproling tokens', 'Sacrifice outlets'],
  },
  POR: {
    tagline: 'Magic stripped to the basics, made for first-time players. Pure creature combat with no instants: everything happens on the board, so it is the best place to learn.',
    about: 'Portal was designed to teach Magic: no instants, no activated-ability tangles, and plain-English cards.',
    playstyle: 'Pure creature combat with sorcery-speed removal. Every decision is on the board, so it is a great place to learn attacking and blocking.',
    tryIf: 'learning the game or a relaxed, straightforward match',
    power: 1, speed: 3, complexity: 1,
    themes: ['aggro', 'flyers'],
    mechanics: ['Sorcery speed only', 'Vanilla creatures', 'Simple removal'],
  },
  P02: {
    tagline: 'Portal\'s sequel, still beginner-friendly with no instants at all. Straightforward creature battles, a little stronger than Portal, with a few creature-type payoffs.',
    about: 'Portal Second Age returns to the beginner-friendly Portal rules with the Goblins, Elves and Pirates of Caliman.',
    playstyle: 'Straightforward creature combat with no instants. Slightly stronger creatures than Portal and a few creature-type payoffs.',
    tryIf: 'learning the game or a relaxed, straightforward match',
    power: 2, speed: 3, complexity: 1,
    themes: ['aggro', 'tribal'],
    mechanics: ['Sorcery speed only', 'Creature-type payoffs'],
  },
  PTK: {
    tagline: 'China\'s Three Kingdoms, in simple Portal rules. Aggressive and quick to learn: cavalry with horsemanship races past blockers.',
    about: 'Portal Three Kingdoms retells the Romance of the Three Kingdoms: Wei, Shu and Wu generals, strategists and armies.',
    playstyle: 'Simple Portal-style rules where horsemanship cavalry races past blockers. Aggressive and quick to learn.',
    tryIf: 'simple rules and evasive aggro',
    power: 2, speed: 4, complexity: 2,
    themes: ['aggro', 'legends'],
    mechanics: ['Horsemanship', 'Sorcery speed only', 'Legendary generals'],
  },
  S99: {
    tagline: 'A starter set of classic, no-frills cards — the gentlest way into sealed. Fair, creature-focused games with very little to track; fliers and simple removal decide most of them.',
    about: 'Starter 1999 was built for new players: evergreen keywords and readable effects, plus a few gems like Thorn Elemental.',
    playstyle: 'Fair, creature-centred games with little to keep track of. Fliers and simple removal decide most of them.',
    tryIf: 'learning the game or a quick, simple draft',
    power: 2, speed: 3, complexity: 1,
    themes: ['aggro', 'flyers'],
    mechanics: ['Evergreen keywords', 'Simple removal'],
  },
  INV: {
    tagline: 'The Phyrexian invasion of Dominaria, a celebrated multicolour set. Removal-rich drafts where gold cards reward stretching into extra colours and kicker spells scale with your mana.',
    about: 'Invasion was the first set built around multicolour: allied colours band together against the Phyrexian onslaught.',
    playstyle: 'Rich, removal-heavy multicolour drafts. Kicker gives every spell an early and a late mode, and domain rewards stretching your mana.',
    tryIf: 'multicolour decks and flexible spells',
    power: 4, speed: 3, complexity: 3,
    themes: ['multicolor'],
    mechanics: ['Kicker', 'Domain', 'Gold cards', 'Split cards'],
  },
  ONS: {
    tagline: 'A plane of creature types: Goblins, Elves, Clerics, Wizards and more. Slow, thoughtful games of face-down morph bluffs, cycling and tribal lords.',
    about: 'Onslaught is a tribal set on Otaria: Goblins, Elves, Clerics, Soldiers, Wizards and Zombies, all caring about their own kind.',
    playstyle: 'Morph bluffing makes combat a guessing game, cycling smooths draws, and tribal lords reward drafting one creature type.',
    tryIf: 'tribal decks and face-down bluffing',
    power: 3, speed: 2, complexity: 3,
    themes: ['tribal'],
    mechanics: ['Morph', 'Cycling', 'Creature types'],
  },
  LGN: {
    tagline: 'An all-creature set where every card is a creature. Board-focused combat with no removal spells; morph bluffs and provoke stand in for interaction.',
    about: 'Legions continues the Onslaught block with nothing but creatures: no spells, just more tribal bodies and morphs.',
    playstyle: 'Board-centred and combat-heavy, with no removal spells of its own. Morph tricks and provoke stand in for interaction.',
    tryIf: 'tribal decks and pure creature combat',
    power: 2, speed: 3, complexity: 3,
    themes: ['tribal'],
    mechanics: ['Morph', 'Provoke', 'Amplify'],
  },
  SCG: {
    tagline: 'Dragons and storm close out the Onslaught block. Big spells and big creatures, with landcycling keeping your mana flowing toward the top end.',
    about: 'Scourge brings the Dragons back to Otaria and rewards big mana values, with storm debuting alongside landcycling.',
    playstyle: 'Big spells and big creatures. Landcycling keeps the mana flowing toward Dragons, while storm rewards a turn of cheap spells.',
    tryIf: 'dragons and expensive spells',
    power: 3, speed: 3, complexity: 3,
    themes: ['bigThings', 'tribal'],
    mechanics: ['Storm', 'Landcycling', 'Morph', 'Mana value matters'],
  },
  MRD: {
    tagline: 'A metal plane where artifacts rule. Fast and swingy: cheap Equipment turns any creature into a threat and affinity decks empty their hands early.',
    about: 'Mirrodin is an artificial world of metal, and the set fills every colour with artifacts, equipment and artifact lands.',
    playstyle: 'Fast and swingy. Equipment such as Bonesplitter turns any creature into a threat, and affinity decks dump their hands early.',
    tryIf: 'artifacts and equipment-fuelled aggro',
    power: 4, speed: 4, complexity: 3,
    themes: ['artifacts', 'aggro'],
    mechanics: ['Equipment', 'Affinity for artifacts', 'Imprint', 'Entwine'],
  },
  CHK: {
    tagline: 'Spirits against samurai in a Japanese-inspired war. Deliberate, combat-focused games where bushido makes blocks costly and Spirits return from the graveyard.',
    about: 'Champions of Kamigawa sets mortals against the angry kami: samurai with bushido, ninja-adjacent rogues and spirit-filled Arcane magic.',
    playstyle: 'Deliberate and combat-focused. Bushido makes blocks costly, Arcane spells splice onto each other, and soulshift returns dead Spirits.',
    tryIf: 'samurai, spirits and careful combat',
    power: 3, speed: 2, complexity: 3,
    themes: ['tribal', 'spells'],
    mechanics: ['Bushido', 'Splice onto Arcane', 'Soulshift', 'Flip cards'],
  },
  RAV: {
    tagline: 'The original city of ten guilds. Two-colour guild decks, each with its own trick — convoke armies, graveyard dredging, radiance — in fair, interactive games.',
    about: 'Ravnica: City of Guilds introduced the guilds Selesnya, Golgari, Boros and Dimir, each a two-colour faction with its own mechanic.',
    playstyle: 'Two-colour guild drafting with bounce lands for mana. Convoke armies, dredge graveyards and radiance tricks give each guild a distinct deck.',
    tryIf: 'guild decks and two-colour synergies',
    power: 4, speed: 3, complexity: 3,
    themes: ['multicolor', 'graveyard', 'tokens'],
    mechanics: ['Convoke', 'Dredge', 'Radiance', 'Transmute', 'Hybrid mana'],
  },
  LRW: {
    tagline: 'Eight fairy-tale tribes: Elves, Faeries, Giants, Goblins and more. One of the deepest, most intricate drafts, with tribal synergies and many decisions every turn.',
    about: 'Lorwyn is a sunlit world of Kithkin, Merfolk, Faeries, Elves, Giants, Elementals, Goblins and Treefolk, plus the very first planeswalkers.',
    playstyle: 'Deep tribal synergies and many decisions per turn. Evoke, champion and clash interact constantly, and changelings fit every tribe.',
    tryIf: 'tribal synergy and puzzling over every turn',
    power: 4, speed: 3, complexity: 5,
    themes: ['tribal'],
    mechanics: ['Evoke', 'Champion', 'Changeling', 'Clash', 'Hideaway'],
  },
  KTK: {
    tagline: 'Five three-colour clans on a dragonless Tarkir. Slow, long games full of face-down morphs, with plenty of mana fixing to build a three-colour deck.',
    about: 'Khans of Tarkir is built on wedges: the Abzan, Jeskai, Sultai, Mardu and Temur clans each ally two colours with their enemy.',
    playstyle: 'Slow three-colour decks with plenty of morphs. Mana fixing is generous, games go long, and the right clan deck grinds out a win.',
    tryIf: 'three-colour decks and face-down morph mind games',
    power: 3, speed: 1, complexity: 4,
    themes: ['multicolor', 'graveyard'],
    mechanics: ['Morph', 'Raid', 'Delve', 'Prowess', 'Outlast', 'Ferocious'],
  },
  DOM: {
    tagline: 'A return to Magic\'s home plane, where Sagas tell its history in chapters. Slow, value-oriented games with lots of removal and flexible kicker spells.',
    about: "Dominaria revisits Magic's original setting, with Sagas retelling its past and legendary heroes from decades of story.",
    playstyle: 'Slow and value-oriented. Removal is plentiful, kicker gives flexibility, and historic spells reward artifacts, legends and Sagas.',
    tryIf: 'legends, Sagas and slower value-driven games',
    power: 3, speed: 2, complexity: 3,
    themes: ['legends', 'artifacts'],
    mechanics: ['Historic', 'Kicker', 'Sagas', 'Legendary sorceries'],
  },
  THB: {
    tagline: 'Gods and heroes in the Underworld of Theros. Midrange games built on enchantments: Aura-carrying heroes, constellation payoffs, and escape cards that climb back out of the graveyard.',
    about: 'Theros Beyond Death follows Elspeth into the Underworld of Theros, a Greek-myth plane where gods answer their followers\' devotion.',
    playstyle: 'Midrange with long games. Devotion wakes the gods, heroes grow from the spells you target them with, and escape lets graveyard cards return by exiling others.',
    tryIf: 'enchantments, Aura-boosted heroes and graveyard recursion',
    power: 3, speed: 2, complexity: 3,
    themes: ['graveyard', 'counters', 'bigThings'],
    mechanics: ['Escape', 'Devotion', 'Constellation', 'Sagas'],
  },
  IKO: {
    tagline: 'Giant monsters on Ikoria. A fast, swingy format where creatures mutate on top of each other into hulking threats, cycling keeps your draws flowing, and keyword counters stick to your best creature.',
    about: 'Ikoria: Lair of Behemoths is a plane of kaiju and the humans who hunt or bond with them, split across shards of three colours.',
    playstyle: 'Fast and swingy. Mutate stacks creatures into one towering threat (and a juicy removal target), cycling smooths every draw and feeds its payoffs, and three-colour wedges reward fixing.',
    tryIf: 'monster-sized creatures, mutate stacking and multicolour decks',
    power: 4, speed: 4, complexity: 4,
    themes: ['multicolor', 'counters', 'bigThings'],
    mechanics: ['Mutate', 'Cycling', 'Keyword counters', 'Companion'],
  },
  M21: {
    tagline: 'A clean core set with simple, familiar cards and unusually big bombs. Classic two-colour Limited with plenty of removal, where games often swing on who draws their rare.',
    about: 'Core Set 2021 is headlined by Teferi, Basri, Chandra, Liliana and Garruk, with no single plane — just straightforward Magic.',
    playstyle: 'Classic two-colour Limited with plenty of removal. Bombs are big, so games often swing on who draws theirs.',
    tryIf: 'classic two-colour Limited, big dragons and planeswalkers',
    power: 4, speed: 3, complexity: 2,
    themes: ['flyers', 'counters', 'lifegain', 'bigThings'],
    mechanics: ['Evergreen keywords', 'Planeswalkers'],
  },
  ZNR: {
    tagline: 'Adventurers exploring Zendikar\'s ruins. One of the fastest formats here: every land drop pumps your creatures, and double-faced spell/land cards smooth out your draws.',
    about: "Zendikar Rising sends adventurers into Zendikar's wild ruins: Clerics, Rogues, Warriors and Wizards forming parties.",
    playstyle: 'One of the fastest formats here. Landfall rewards every land drop, kicker scales spells, and modal double-faced cards fix flooding and screw.',
    tryIf: 'fast aggressive decks, landfall and kicker',
    power: 3, speed: 5, complexity: 3,
    themes: ['aggro', 'tribal'],
    mechanics: ['Party', 'Landfall', 'Kicker', 'Modal double-faced cards'],
  },
  KHM: {
    tagline: 'The Norse realms of Kaldheim. Midrange games of planning ahead: foretell spells a turn early, reward attacking with boast, and build around snow lands.',
    about: 'Kaldheim spans ten Norse-inspired realms where Elves, Giants, Berserkers and Zombies clash, and double-faced Gods walk among them.',
    playstyle: 'Midrange with a lot of planning. Foretell lets you prepay spells, boast rewards attacking, and snow lands power snow payoffs.',
    tryIf: 'tribal clans, snow mana and planning turns ahead',
    power: 4, speed: 3, complexity: 4,
    themes: ['tribal', 'multicolor'],
    mechanics: ['Foretell', 'Boast', 'Snow', 'Sagas', 'Changeling'],
  },
  STX: {
    tagline: 'Rival colleges at a magic university. Spell-focused decks that reward every instant and sorcery you cast, with Lessons fetched from your sideboard.',
    about: 'Strixhaven: School of Mages is a school of magic where each college is a two-colour pair with its own approach to spells.',
    playstyle: 'Spell-focused drafts. Magecraft rewards every instant and sorcery, and Learn fetches Lessons from your sideboard for flexible card advantage.',
    tryIf: 'instants and sorceries, magecraft and toolbox sideboards',
    power: 3, speed: 3, complexity: 4,
    themes: ['spells', 'multicolor'],
    mechanics: ['Magecraft', 'Learn & Lessons', 'Ward'],
  },
  AFR: {
    tagline: 'Dungeon crawling on a fantasy-RPG plane: roll d20s and venture room by room through dungeons. Steady midrange games where pack tactics rewards wide attacks and dragons rule the skies.',
    about: 'Adventures in the Forgotten Realms brings Dungeons & Dragons to Magic: dungeon rooms, dice rolls, Class enchantments and iconic monsters.',
    playstyle: 'Midrange with steady value. Venturing into dungeons grinds out advantage, pack tactics rewards wide attacks, and dragons dominate the skies.',
    tryIf: 'D&D flavour, dice rolls and dungeon crawling',
    power: 3, speed: 2, complexity: 3,
    themes: ['flyers', 'artifacts'],
    mechanics: ['Venture into the dungeon', 'Roll a d20', 'Pack tactics', 'Class enchantments'],
  },
  MID: {
    tagline: 'Werewolves and witches as day turns to night on Innistrad. Fast, graveyard-focused games where werewolves flip at night and cards come back for a second use.',
    about: 'Innistrad: Midnight Hunt is a harvest-festival horror set: humans hold out against werewolves under an endless night.',
    playstyle: 'Fast and graveyard-centric. Day and night flip werewolves, flashback and disturb give cards second lives, and decayed Zombies swarm early.',
    tryIf: 'werewolves, flashback and aggressive graveyard decks',
    power: 3, speed: 4, complexity: 4,
    themes: ['graveyard', 'tokens', 'aggro'],
    mechanics: ['Day/Night', 'Flashback', 'Disturb', 'Coven', 'Decayed'],
  },
  VOW: {
    tagline: 'A vampire wedding on Innistrad, where creatures train and grow by attacking alongside bigger allies. A bit slower than its sister set, with Blood tokens and sacrifice payoffs.',
    about: 'Innistrad: Crimson Vow is set at the wedding of Sorin and Olivia, with vampire nobility and their uninvited guests.',
    playstyle: 'Slower than Midnight Hunt. Training grows creatures through combat, Blood tokens rummage, and exploit and disturb reward sacrifice and graveyard play.',
    tryIf: 'gothic vampires, counters and sacrifice value',
    power: 3, speed: 3, complexity: 4,
    themes: ['counters', 'sacrifice', 'tribal'],
    mechanics: ['Training', 'Blood tokens', 'Exploit', 'Cleave', 'Disturb'],
  },
  NEO: {
    tagline: 'Neon cyberpunk Kamigawa of ninjas, vehicles and samurai. Fast, synergy-rich games where ninjas sneak in past blockers and enchantments and counters power up your creatures.',
    about: 'Kamigawa: Neon Dynasty revisits Kamigawa a thousand years later, mixing tradition and technology: ninja clans, mechs and kami.',
    playstyle: 'Fast and synergy-rich. Ninjutsu sneaks threats in, vehicles and equipment reconfigure, and modified creatures reward enchantments and counters.',
    tryIf: 'ninjas, vehicles and artifact-enchantment synergies',
    power: 4, speed: 4, complexity: 4,
    themes: ['artifacts', 'counters', 'aggro'],
    mechanics: ['Ninjutsu', 'Channel', 'Modified', 'Reconfigure', 'Vehicles'],
  },
  SNC: {
    tagline: '1920s crime families running a city of demons and angels. Three-colour family decks with good fixing, each with a clear plan: sacrifice, card filtering, or hit-and-run attacks.',
    about: 'Streets of New Capenna is an art-deco city ruled by five three-colour crime families.',
    playstyle: 'Three-colour family decks with good fixing. Casualty, connive and blitz give each family its plan, and shield counters protect key threats.',
    tryIf: 'three-colour decks and crime-family flavour',
    power: 3, speed: 3, complexity: 4,
    themes: ['multicolor', 'sacrifice', 'counters'],
    mechanics: ['Casualty', 'Connive', 'Blitz', 'Shield counters', 'Alliance'],
  },
  DMU: {
    tagline: 'A coalition of legends defends Dominaria, with domain rewarding every basic land type you play. Slow, grindy and removal-rich: patience and extra colours pay off.',
    about: 'Dominaria United returns to Magic\'s home plane on the eve of the Phyrexian invasion, packed with legends and multicolour heroes.',
    playstyle: 'Slow, grindy and removal-rich. Kicker and domain reward patience, and enlist and read ahead give creatures and Sagas flexible uses.',
    tryIf: 'slower multicolour games with lots of removal',
    power: 3, speed: 1, complexity: 3,
    themes: ['multicolor', 'legends'],
    mechanics: ['Kicker', 'Domain', 'Enlist', 'Read ahead', 'Stun counters'],
  },
  BRO: {
    tagline: 'The Brothers\' War retold with artifacts. Midrange games that ramp with Powerstones into big artifact threats, with creatures that come in a small and a large size.',
    about: "The Brothers' War dramatises Urza and Mishra's conflict with giant machines, Powerstones and melding war engines.",
    playstyle: 'Artifact-centred midrange. Powerstones ramp into big artifact threats, prototype gives creatures two sizes, and unearth brings attackers back.',
    tryIf: 'artifacts, giant machines and ramp',
    power: 4, speed: 2, complexity: 4,
    themes: ['artifacts', 'bigThings'],
    mechanics: ['Unearth', 'Prototype', 'Powerstones', 'Meld'],
  },
  J22: {
    tagline: 'Themed half-decks: each pack is built around one colour and idea, so two packs make a deck. Laid-back and easy, with honest, synergy-driven games.',
    about: 'Jumpstart 2022 packs are built around themes like Dragons, Wizards or Tokens rather than a single world.',
    playstyle: 'Laid-back and focused: each theme has a clear plan, so decks come together fast and play honest, synergy-driven games.',
    tryIf: 'quick, clear themes with little deckbuilding',
    power: 3, speed: 3, complexity: 1,
    themes: ['tokens', 'counters', 'lifegain'],
    mechanics: ['Themed packs', 'Evergreen keywords'],
  },
  ONE: {
    tagline: 'Phyrexia conquers Mirrodin. Fast and punishing: poison gives a second way to win, and proliferate grows every kind of counter at once.',
    about: "Phyrexia: All Will Be One is set on New Phyrexia, where the Machine Orthodoxy has conquered the plane once called Mirrodin.",
    playstyle: 'Fast and punishing. Toxic creatures give a second way to win through poison, proliferate grows counters of every kind, and oil counters power up artifacts.',
    tryIf: 'poison counters and aggressive proliferate decks',
    power: 4, speed: 4, complexity: 3,
    themes: ['counters', 'artifacts', 'aggro'],
    mechanics: ['Toxic', 'Proliferate', 'Corrupted', 'Oil counters', 'For Mirrodin!'],
  },
  MOM: {
    tagline: 'The Phyrexian invasion hits every plane. Fast and powerful games where battles reward attacking, transforming cards flip into stronger forms and counters spread across your board.',
    about: 'March of the Machine is the climax of the Phyrexian saga, with battles to defeat and heroes from across the multiverse.',
    playstyle: 'Fast and powerful. Battles reward attacking, incubate makes Phyrexian tokens, and backup spreads counters across your board.',
    tryIf: 'multiverse-spanning action and aggressive battles',
    power: 4, speed: 4, complexity: 4,
    themes: ['counters', 'tokens', 'aggro'],
    mechanics: ['Battles', 'Incubate', 'Backup', 'Convoke', 'Transform'],
  },
  MAT: {
    tagline: 'A small aftermath extension of multicolour legends. Not a standalone draft: mix it into March of the Machine or another set for a sprinkle of strong gold cards.',
    about: "March of the Machine: The Aftermath picks up after the invasion, a 50-card epilogue of multicolour legends.",
    playstyle: 'An extension: play it alongside March of the Machine or another set to add a sprinkle of strong gold legends.',
    tryIf: 'multicolour legends added to another set',
    power: 4, speed: 3, complexity: 3,
    themes: ['multicolor', 'legends'],
    mechanics: ['Gold legends', 'Extension sheet'],
  },
  LTR: {
    tagline: 'The Fellowship against Mordor. Midrange games led by legendary heroes, where the Ring grows your Ring-bearer each time it tempts you and Orc armies amass.',
    about: 'Tales of Middle-earth brings the Fellowship, Sauron and the armies of Mordor to Magic.',
    playstyle: 'Midrange with legendary heroes. The Ring tempting you makes a Ring-bearer stronger each time, amass builds Orc armies and Food gains life.',
    tryIf: 'Tolkien, legendary heroes and Orc armies',
    power: 4, speed: 3, complexity: 3,
    themes: ['legends', 'tokens'],
    mechanics: ['The Ring tempts you', 'Amass Orcs', 'Food', 'Scry'],
  },
  WOE: {
    tagline: 'Fairy tales on Eldraine. Fast and synergistic: adventure cards are two spells in one, and Role enchantments upgrade your creatures.',
    about: 'Wilds of Eldraine returns to the storybook plane of courts, curses and the Wilds.',
    playstyle: 'Fast and synergistic. Adventure cards are two spells in one, Roles enchant creatures for value, and celebration and bargain reward permanents leaving or arriving.',
    tryIf: 'fairy tales, two-for-one adventures and enchantments',
    power: 3, speed: 4, complexity: 4,
    themes: ['tokens', 'sacrifice', 'aggro'],
    mechanics: ['Adventure', 'Roles', 'Celebration', 'Bargain'],
  },
  LCI: {
    tagline: 'Dinosaurs, pirates and vampires exploring Ixalan\'s underground. Midrange exploration where creatures grow as they explore and some spells cast free spells from your library.',
    about: 'The Lost Caverns of Ixalan dives below Ixalan into a vast cavern world of dinosaurs, merfolk, pirates and ancient gods.',
    playstyle: 'Midrange exploration. Explore and Map tokens grow creatures, descend rewards a full graveyard, and discover casts free spells from your library.',
    tryIf: 'dinosaurs and exploring for value',
    power: 3, speed: 3, complexity: 4,
    themes: ['tribal', 'graveyard', 'artifacts'],
    mechanics: ['Explore', 'Discover', 'Descend', 'Craft', 'Map tokens'],
  },
  MKM: {
    tagline: 'A murder mystery in Ravnica. Midrange games of hidden information: creatures hide face down in disguise and Clues and Cases reward investigating.',
    about: 'Murders at Karlov Manor turns Ravnica into a detective story, with every guild a suspect.',
    playstyle: 'Midrange with hidden information. Disguised creatures hide face down, collect evidence eats graveyards, and Clues and Cases reward investigating.',
    tryIf: 'detective stories and face-down bluffing',
    power: 3, speed: 3, complexity: 4,
    themes: ['multicolor', 'graveyard', 'artifacts'],
    mechanics: ['Disguise', 'Cloak', 'Collect evidence', 'Suspect', 'Cases'],
  },
  OTJ: {
    tagline: 'A Wild West heist with outlaws and bounties. Fast and interactive: crimes against your opponent pay off, and plotted spells can be set up now and cast free later.',
    about: 'Outlaws of Thunder Junction is a frontier full of gangs and gunslingers, each outlaw crew committing crimes.',
    playstyle: 'Fast and interactive. Committing crimes by targeting opponents pays off, plot sets up a free spell later, and spree spells pick their modes.',
    tryIf: 'outlaws, crimes and flexible spree spells',
    power: 3, speed: 4, complexity: 4,
    themes: ['aggro', 'tribal'],
    mechanics: ['Crimes', 'Plot', 'Saddle', 'Spree', 'Outlaws'],
  },
  BIG: {
    tagline: 'A small bonus sheet of loot from the Thunder Junction heist. Not a standalone draft: add it to another set for a shot at a powerful artifact in every pack.',
    about: "The Big Score is a bonus sheet of high-value artifacts and treasures that came with Outlaws of Thunder Junction boosters.",
    playstyle: 'An extension: add it to Outlaws of Thunder Junction (or another set) and every pack has a shot at a powerful artifact.',
    tryIf: 'adding big, splashy artifacts to another set',
    power: 4, speed: 3, complexity: 2,
    themes: ['artifacts'],
    mechanics: ['High-value artifacts', 'Extension sheet'],
  },
  MH3: {
    tagline: 'A high-power remix of Eldrazi, energy and planeswalkers. The strongest modern draft here: bombs in every pack and fast games full of haymakers. Best for experienced players.',
    about: 'Modern Horizons 3 is a supplemental set built for Modern, packed with Eldrazi, energy and powerful planeswalker transforms.',
    playstyle: 'The strongest modern draft here. Every pack has bombs, multiple mechanics interact, and games are fast and full of haymakers.',
    tryIf: 'high power, deep synergies and splashy rares',
    power: 5, speed: 4, complexity: 5,
    themes: ['bigThings', 'graveyard', 'artifacts', 'multicolor'],
    mechanics: ['Energy', 'Eldrazi & devoid', 'Double-faced planeswalkers', 'Modal double-faced lands', 'Evoke'],
  },
  BLB: {
    tagline: 'Woodland animals in ten two-colour tribes. Very fast and aggressive: pick an animal family and go all-in on its plan.',
    about: 'Bloomburrow is a storybook plane of animal folk: Rabbits, Bats, Lizards, Otters, Squirrels, Mice, Raccoons, Frogs, Birds and Rats.',
    playstyle: 'Very fast, with each animal tribe its own aggressive plan. Offspring, forage and gift reward going all-in on your creature type.',
    tryIf: 'tribal decks, critters and quick games',
    power: 4, speed: 5, complexity: 3,
    themes: ['tribal', 'aggro', 'tokens'],
    mechanics: ['Offspring', 'Forage', 'Gift', 'Expend', 'Valiant'],
  },
  DSK: {
    tagline: 'A haunted-house horror movie. Midrange games built around enchantments, with two-door Room cards and face-down threats hiding on the board.',
    about: 'Duskmourn: House of Horror traps survivors inside a sentient house, a slasher film turned into a set.',
    playstyle: 'Midrange with enchantment synergy. Rooms are two-door enchantments, eerie rewards casting enchantments, and manifest dread hides threats face down.',
    tryIf: 'horror movies and enchantment-based decks',
    power: 3, speed: 3, complexity: 4,
    themes: ['graveyard', 'sacrifice'],
    mechanics: ['Rooms', 'Eerie', 'Survival', 'Manifest dread', 'Impending', 'Delirium'],
  },
  FDN: {
    tagline: 'The new Magic core set, built to teach modern Magic. Fair, clean two-colour Limited with clear archetypes and little to track, so good attacks and blocks win.',
    about: 'Foundations is an evergreen core set built for new players, with familiar cards, creature types and no complicated set mechanics.',
    playstyle: 'Fair two-colour Limited with clear archetypes. Little to keep track of, so the focus stays on good attacks and blocks.',
    tryIf: 'learning Limited or a classic core-set draft',
    power: 3, speed: 3, complexity: 1,
    themes: ['tribal', 'lifegain', 'tokens'],
    mechanics: ['Evergreen keywords', 'Creature-type payoffs'],
  },
  INR: {
    tagline: 'A greatest-hits tour of gothic Innistrad. Powerful, graveyard-heavy games where madness, flashback and friends reward cards in your graveyard.',
    about: 'Innistrad Remastered collects the best of five Innistrad sets: Spirits, Zombies, Vampires, Werewolves and their mechanics.',
    playstyle: 'Powerful and graveyard-heavy. Madness, flashback, morbid and disturb all reward cards in the graveyard, and the rare sheet is stacked.',
    tryIf: 'horror flavour and a graveyard-heavy draft',
    power: 4, speed: 3, complexity: 4,
    themes: ['graveyard', 'tribal', 'tokens'],
    mechanics: ['Flashback', 'Madness', 'Morbid', 'Disturb', 'Day/Night'],
  },
  DFT: {
    tagline: 'A multiversal race with vehicles. Fast and aggressive: deal damage every turn to reach max speed and unlock one-shot exhaust bursts.',
    about: 'Aetherdrift is a death race across the planes, with racers, crews and souped-up vehicles.',
    playstyle: 'Fast and aggressive. Start your engines rewards dealing damage every turn, exhaust abilities give one-time bursts, and vehicles need crew.',
    tryIf: 'vehicles, racing flavour and aggressive decks',
    power: 3, speed: 4, complexity: 4,
    themes: ['artifacts', 'aggro'],
    mechanics: ['Start your engines!', 'Exhaust', 'Vehicles & crew', 'Cycling'],
  },
  TDM: {
    tagline: 'Dragons return to Tarkir\'s five clans. Two- and three-colour clan decks with plenty of fixing: make attackers with mobilize, chain spells, or reuse your graveyard.',
    about: 'Tarkir: Dragonstorm brings the Dragons back to Tarkir, where the clans now fight alongside them.',
    playstyle: 'Two- and three-colour clan decks with plenty of fixing. Mobilize makes attackers, flurry rewards spell chains, and renew and harmonize use the graveyard.',
    tryIf: 'dragons, clans and three-colour decks',
    power: 4, speed: 3, complexity: 4,
    themes: ['multicolor', 'flyers', 'graveyard'],
    mechanics: ['Mobilize', 'Flurry', 'Renew', 'Endure', 'Harmonize', 'Omens'],
  },
  FIN: {
    tagline: 'Heroes, summons and towns from across the RPG series. Midrange battles where you gear up your party with Equipment and summons strike in chapters.',
    about: 'Final Fantasy brings characters, jobs and summons from every mainline game to Magic.',
    playstyle: 'Midrange RPG battles. Equipment and job-select creatures gear up your party, Summon Sagas strike in chapters, and legendary heroes lead the way.',
    tryIf: 'RPG flavour, Equipment and legendary heroes',
    power: 4, speed: 3, complexity: 3,
    themes: ['artifacts', 'legends'],
    mechanics: ['Job select', 'Equipment', 'Summon Sagas', 'Towns'],
  },
  EOE: {
    tagline: 'Sci-fi Magic among the stars. Slower midrange ramp: charge up huge Spacecraft, crack Landers for mana and warp threats in early for a turn.',
    about: 'Edge of Eternities is set among the stars, with starships, planets and the void between them.',
    playstyle: 'Midrange ramp. Station charges up huge Spacecraft, Landers crack for mana, warp casts threats early for a turn, and void rewards things leaving play.',
    tryIf: 'spaceships, ramp and sacrifice-heavy decks',
    power: 3, speed: 2, complexity: 4,
    themes: ['artifacts', 'bigThings', 'sacrifice'],
    mechanics: ['Station', 'Warp', 'Landers', 'Void', 'Spacecraft'],
  },
  OM1: {
    tagline: 'The Spider-Man draft retold with Magic\'s own heroes and villains. Small, fast and full of legends, with the same easy-to-learn mechanics.',
    about: 'Through the Omenpaths is the same card pool as Marvel\'s Spider-Man, with new names and art set on Magic\'s planes.',
    playstyle: 'Identical to the Spider-Man draft: small, fast and legend-filled. Web-slinging and mayhem keep the pressure on.',
    tryIf: 'the Spider-Man draft without leaving the Multiverse',
    power: 3, speed: 4, complexity: 2,
    themes: ['legends', 'aggro'],
    mechanics: ['Web-slinging', 'Mayhem', 'Villains & heroes'],
  },
  SPM: {
    tagline: 'Web-slinging heroes against New York\'s rogues\' gallery. A small, fast set: swap in heroes cheaply with web-slinging and recast discarded cards with mayhem.',
    about: "Marvel's Spider-Man brings New York's heroes and villains to Magic, from Peter Parker to Green Goblin.",
    playstyle: 'A small, fast set. Web-slinging returns a creature to cast a hero cheaply, and mayhem rewards discarding to recast from the graveyard.',
    tryIf: 'comic-book flavour and legendary characters',
    power: 3, speed: 4, complexity: 2,
    themes: ['legends', 'aggro'],
    mechanics: ['Web-slinging', 'Mayhem', 'Villains & heroes'],
  },
  TLA: {
    tagline: 'The four nations of air, earth, fire and water. Midrange games with an elemental bending trick every turn, and Allies that grow stronger together.',
    about: 'Avatar: The Last Airbender follows Aang and friends across the four nations, from airbenders to the Fire Nation.',
    playstyle: 'Elemental tricks every turn. Each bending style has its own effect, Lessons reward spells, and Allies grow stronger together.',
    tryIf: 'the show, and elemental tricks on every turn',
    power: 3, speed: 3, complexity: 3,
    themes: ['spells', 'tribal'],
    mechanics: ['Airbend', 'Earthbend', 'Firebending', 'Waterbend', 'Lessons', 'Allies'],
  },
  ECL: {
    tagline: 'Lorwyn\'s tribes return under a darker sky. Tribal synergy with flash-speed tricks, where you weaken your own creatures with -1/-1 counters for extra power.',
    about: 'Lorwyn Eclipsed revisits Lorwyn with its old tribes — Kithkin, Merfolk, Faeries, Boggarts, Elves and Changelings — in a darker fairy tale.',
    playstyle: 'Tribal synergy with flash-speed tricks. Behold rewards revealing your tribe, and blight trades -1/-1 counters on your own creatures for power.',
    tryIf: 'tribal synergies and flash-speed tricks',
    power: 4, speed: 3, complexity: 4,
    themes: ['tribal', 'counters'],
    mechanics: ['Behold', 'Blight', 'Changeling', 'Flash'],
  },
  TMT: {
    tagline: 'Four ninja turtle brothers against the Foot Clan. Aggressive and go-wide: flood the board with Mutant tokens and swing in extra combats, or play tempo by blinking your own creatures.',
    about: 'Teenage Mutant Ninja Turtles brings the heroes in a half shell, Splinter and the Foot Clan to Magic.',
    playstyle: 'Aggressive and go-wide. Flood the board with Mutant tokens, swing in extra combats, or play tempo by blinking and bouncing your own creatures.',
    tryIf: 'aggressive go-wide decks and pizza-fuelled flavour',
    power: 3, speed: 4, complexity: 2,
    themes: ['tokens', 'aggro'],
    mechanics: ['Alliance', 'Mutant tokens', 'Extra combats', 'Blink'],
  },
  SOS: {
    tagline: 'Back to Strixhaven, where each college casts spells its own way. Dense, spell-heavy and one of the most complex drafts here — rewarding for experienced players.',
    about: 'Secrets of Strixhaven returns to the mage university, with every college bringing a classic spell mechanic.',
    playstyle: 'Dense and spell-heavy. Casualty, storm, affinity, miracle and cascade each power a college, and prepared permanents let you recast their spells later.',
    tryIf: 'instants and sorceries, and decks built around spells',
    power: 4, speed: 3, complexity: 5,
    themes: ['spells', 'multicolor'],
    mechanics: ['Storm', 'Cascade', 'Miracle', 'Casualty', 'Affinity', 'Prepared'],
  },
  MSH: {
    tagline: 'Superheroes assemble against their villains. Aggressive battles where heroes power up the turn they arrive and teamwork taps your team to supercharge spells.',
    about: "Marvel Super Heroes brings the Avengers, X-Men and their villains to Magic.",
    playstyle: 'Aggressive superhero battles. Power-up gives each hero a one-time boost that is cheaper the turn it arrives, and teamwork taps your creatures to supercharge spells.',
    tryIf: 'comic-book flavour and building a team',
    power: 4, speed: 4, complexity: 3,
    themes: ['legends', 'counters', 'flyers'],
    mechanics: ['Power-up', 'Teamwork', 'Connive', 'Landcycling'],
  },
  HOB: {
    tagline: 'Bilbo\'s journey from the Shire to the Lonely Mountain. Easy-going midrange of tokens and Treasure: build Goblin armies, gather your Dwarven company and ramp toward dragons.',
    about: "The Hobbit follows Bilbo, Thorin's company of Dwarves and Gandalf on the road to the Lonely Mountain and Smaug's hoard.",
    playstyle: 'Midrange with tokens and treasure. Amass builds Goblin armies, recruit gathers your company, and Treasure ramps toward dragons and big legends.',
    tryIf: 'Tolkien, armies of tokens and treasure-fuelled ramp',
    power: 3, speed: 3, complexity: 2,
    themes: ['tokens', 'tribal', 'bigThings'],
    mechanics: ['Amass', 'Recruit', 'Treasure', 'Equipment'],
  },
  FRA: {
    tagline: 'Planeswalkers in training at an academy. Card-selection midrange with ten archetypes: scry and surveil power up your own Jace, and Cadet tokens swarm the board.',
    about: "Reality Fracture sets a planeswalker academy at the heart of a fractured multiverse, with Jace and a class of Cadets.",
    playstyle: 'Card-selection midrange with ten archetypes. Scry and surveil empower your own Jace token, Cadet tokens swarm the board, and prepared permanents recast spells later.',
    tryIf: 'card selection, planeswalkers and lots of archetypes to explore',
    power: 3, speed: 3, complexity: 4,
    themes: ['spells', 'tokens', 'graveyard'],
    mechanics: ['Surveil', 'Empower Jace', 'Prepared', 'Cadet tokens', 'Prowess'],
  },
}

export function setProfile(code: string): SetProfile | undefined {
  return SET_PROFILES[code.toUpperCase()]
}
