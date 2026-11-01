/**
 * The Set guide's hand-written profile of every fully playable set: how its sealed and draft games
 * play, what wins them, what its mechanics do to a game, and three ratings to compare sets by. The
 * world gets a single line — the guide is for choosing what to play, not for lore.
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

/**
 * Theme tags in display order: what a deck in the set is built around, phrased as how it plays.
 * `hint` explains the tag on hover, on the set's tile and on the guide's filter chips.
 */
export const SET_THEMES: readonly { readonly id: SetTheme; readonly label: string; readonly hint: string }[] = [
  { id: 'aggro', label: 'Fast aggro', hint: 'Cheap attackers and curving out win games; slow hands get run over' },
  { id: 'flyers', label: 'Fliers win', hint: 'Ground boards stall, so evasive creatures do most of the damage' },
  { id: 'tribal', label: 'Creature-type synergy', hint: 'Cards reward sticking to one creature type, so draft for your tribe' },
  { id: 'tokens', label: 'Go wide with tokens', hint: 'Fill the board with small creatures, then pump or swarm' },
  { id: 'counters', label: '+1/+1 counters', hint: 'Creatures grow over the game; counters change which trades are good' },
  { id: 'graveyard', label: 'Graveyard value', hint: 'Cards in the graveyard are a resource: recursion, flashback, self-mill' },
  { id: 'spells', label: 'Spells matter', hint: 'Instants and sorceries power your creatures; count your non-creature spells' },
  { id: 'artifacts', label: 'Artifacts matter', hint: 'Artifacts and colourless cards are a deck of their own and a common payoff' },
  { id: 'sacrifice', label: 'Sacrifice engines', hint: 'Creatures are fuel: sacrifice outlets and death triggers grind value' },
  { id: 'multicolor', label: 'Multicolour & splashes', hint: 'Gold cards and fixing reward three-colour decks and splashing bombs' },
  { id: 'lifegain', label: 'Life gain payoffs', hint: 'Gaining life is a plan, not just a buffer: payoffs trigger on it' },
  { id: 'bigThings', label: 'Ramp into bombs', hint: 'Games go long enough for expensive threats; ramp and big rares decide them' },
  { id: 'legends', label: 'Legendary heroes', hint: 'Lots of legendary creatures, and many decks are built around one' },
]

/** A signature mechanic and what it does to a game — how it changes decisions, not its rules text. */
export interface SetMechanic {
  readonly name: string
  readonly inPlay: string
}

export interface SetProfile {
  /**
   * How a game of the set feels and what decides it, in a sentence or two. Shown on its tile, in the
   * lobby's set picker and under the play screen's set choice.
   */
  readonly tagline: string
  /** The world, in one line — context, not the point. */
  readonly setting: string
  /** How a typical sealed or draft game unfolds: pace, board, removal, what decides it. */
  readonly gameplay: string
  /** Three concrete pieces of advice for drafting or playing this set. */
  readonly tips: readonly string[]
  /** Completes "Try it if you like …". */
  readonly tryIf: string
  readonly power: Rating
  readonly speed: Rating
  readonly complexity: Rating
  readonly themes: readonly SetTheme[]
  /** The set's signature mechanics, most important first. */
  readonly mechanics: readonly SetMechanic[]
}

export const SET_PROFILES: Readonly<Record<string, SetProfile>> = {
  LEA: {
    tagline: 'The original 1993 game: plain creatures, walls and burn, with a handful of rares strong enough to decide a match on their own. Removal and fliers win; the rest is a slow ground war.',
    setting: 'The first Magic set ever printed: no plane, no story, just the five colours as Richard Garfield first drew them.',
    gameplay: 'Games are slow and lopsided. The ground clogs with walls, regenerating Skeletons and vanilla 2/2s, so fliers and X-spells do most of the winning. Red and black hold the deepest common removal (Lightning Bolt, Fireball, Terror, Pestilence), while many commons, like the Circles of Protection or Lifetap, do nothing outside a specific match-up. Whoever opens Shivan Dragon, Wrath of God or Control Magic plays a different game from everyone else.',
    tips: [
      'Take removal over creatures: Lightning Bolt, Terror, Fireball and Disintegrate are commons, and Swords to Plowshares is an uncommon worth taking first.',
      'Expect walls on every board; plan to win with fliers like Serra Angel or Sengir Vampire, or with a big X-spell late in the game.',
      'Skip the colour-hosers and Circles unless you know your opponent\'s colours; they are blank cards most of the time.',
    ],
    tryIf: 'history, raw power and seeing where every evergreen card began',
    power: 5, speed: 2, complexity: 2,
    themes: ['bigThings', 'flyers'],
    mechanics: [
      { name: 'X spells', inPlay: 'Fireball, Disintegrate and Drain Life are removal early and a finishing blow late, so holding one back is often right.' },
      { name: 'Walls', inPlay: 'Cheap defenders with huge toughness stall the ground for turns, which pushes games toward fliers and direct damage.' },
      { name: 'Regeneration', inPlay: 'Drudge Skeletons and Uthden Troll block every turn; save Terror or Disintegrate, which stop regeneration, for them.' },
      { name: 'Banding', inPlay: 'Banded attackers let you choose how blockers\' damage is split, so a band with a big creature makes blocks costly.' },
      { name: 'Power Nine', inPlay: 'Moxen, Time Walk and Ancestral Recall are tempo and cards in Limited; nice to open, but bombs like Shivan Dragon matter more.' },
    ],
  },
  LEB: {
    tagline: 'Alpha\'s second print run, and the same 1993 game: slow ground stalls of walls and 2/2s, broken by fliers, burn and a few game-winning rares like Shivan Dragon and Wrath of God.',
    setting: 'Beta reprinted Alpha with its misprints fixed and two missing cards restored: Circle of Protection: Black and Volcanic Island.',
    gameplay: 'Exactly Alpha\'s card pool, so it plays the same way. Walls and regenerators hold the ground, fliers and X-spells end games, and red and black have the best common removal in Lightning Bolt, Fireball, Terror and Pestilence. Power is wildly uneven: a pack can hold a Mox, a dual land or a Circle that never matters, and the player who opens Serra Angel or Shivan Dragon has a real edge.',
    tips: [
      'Removal first: Lightning Bolt, Terror and Fireball are commons, and Swords to Plowshares and Psionic Blast are uncommons worth taking early.',
      'Pestilence is a common board wipe on a stick; with high-toughness creatures or walls it can clear your opponent\'s board every turn.',
      'Open a dual land and a splash becomes cheap; use it to add a bomb or Fireball from a second or third colour.',
    ],
    tryIf: 'history, raw power and seeing where every evergreen card began',
    power: 5, speed: 2, complexity: 2,
    themes: ['bigThings', 'flyers'],
    mechanics: [
      { name: 'X spells', inPlay: 'Fireball, Disintegrate and Drain Life scale with the game, removing a creature early or ending the game from a stalled board.' },
      { name: 'Walls', inPlay: 'Defenders with huge toughness stall the ground for turns, so evasion and direct damage decide most games.' },
      { name: 'Regeneration', inPlay: 'Regenerating blockers shrug off combat and damage removal; Terror and Disintegrate are the clean answers.' },
      { name: 'Banding', inPlay: 'A band lets the attacker split blockers\' damage, so banding a big creature with small ones makes blocking painful.' },
      { name: 'Power Nine', inPlay: 'Moxen and Time Walk are tempo in Limited; welcome, but bombs like Shivan Dragon or Wrath of God swing more games.' },
    ],
  },
  ARN: {
    tagline: 'A small, odd pool of cheap aggressive creatures, desert-guarded combat and rare Djinn and Efreet that dominate when they land. Removal is scarce, so evasion and early pressure carry games.',
    setting: 'Magic\'s first expansion, drawn from the Thousand and One Nights: genies, sultans, flying carpets and lamps.',
    gameplay: 'Packs are only eight cards and the commons are thin, so decks are built from whatever comes. Cheap beaters like Kird Ape, Erg Raiders and Hasran Ogress start the race, and Desert, the most common card on the sheet, punishes attacks with x/1s. Real removal is rare: Oubliette and Cuombajj Witches are the best at common. The Djinn and Efreet, big efficient fliers and 5/5s, sit on the uncommon sheet and win the games they show up in.',
    tips: [
      'Desert pings attackers after combat; count it before swinging with 1-toughness creatures, and play it yourself to blunt their offence.',
      'Oubliette and Cuombajj Witches are your best common answers; take them over another vanilla body.',
      'Kird Ape needs a Forest to be a 2/3 for one; pair red with green to make it one of the best commons.',
    ],
    tryIf: 'oddball old-school cards and a small, scrappy draft',
    power: 3, speed: 3, complexity: 2,
    themes: ['flyers', 'aggro'],
    mechanics: [
      { name: 'Djinn & Efreet', inPlay: 'Serendib Efreet, Juzám Djinn and friends are huge for their cost but carry upkeep costs or drawbacks you have to race through.' },
      { name: 'Lands with abilities', inPlay: 'Desert and Oasis fight in combat from the land slot, so a board can hold more tricks than its creatures show.' },
      { name: 'Landwalk', inPlay: 'Desertwalk, islandwalk auras and colour-dependent creatures like Dandân make your opponent\'s lands matter to every attack.' },
      { name: 'Coin flips', inPlay: 'Mijae Djinn, Ydwen Efreet and Bottle of Suleiman gamble big stats on a flip; play them knowing they fail half the time.' },
    ],
  },
  ATQ: {
    tagline: 'An artifact-heavy pool of small colourless creatures and Urza lands. Games are slow board stalls, decided by whoever assembles big artifacts or the right answer to them.',
    setting: 'Antiquities tells Urza and Mishra\'s war: almost every card is an artifact, a land or a colour\'s way of fighting them.',
    gameplay: 'Packs are eight cards and only a few dozen cards in the set are coloured, so most playables fit any deck. Common creatures are small and defensive (Yotian Soldier, Dragon Engine, Ornithopter, Clay Statue), and real creature removal hardly exists at common, so boards stall. The Urza lands are all over the common sheet: three different ones in play tap for seven mana and power out the big artifacts. Colours mostly add artifact hate and artifact-fed engines like Atog and Orcish Mechanics.',
    tips: [
      'Draft the Urza lands: Mine, Power Plant and Tower are common, and having all three turns a stalled board into a race to your biggest threat.',
      'Crumble and Artifact Blast are live in every match-up here; most of your opponent\'s creatures are artifacts, so take them.',
      'Sacrifice outlets like Atog, Orcish Mechanics and Sage of Lat-Nam turn spare artifacts into damage or cards when the board stalls.',
    ],
    tryIf: 'artifacts, colourless decks and long, puzzle-like games',
    power: 3, speed: 1, complexity: 3,
    themes: ['artifacts', 'bigThings'],
    mechanics: [
      { name: 'Artifact creatures', inPlay: 'Colourless bodies fit every deck, so picks stay open late, but they all die to the colours\' artifact removal.' },
      { name: 'Urza\'s lands', inPlay: 'Each piece taps for one alone; with all three you get seven mana, so long games favour whoever has the set.' },
      { name: 'Artifact hate', inPlay: 'Removal here mostly hits artifacts; holding Crumble for their best creature matters more than ever.' },
      { name: 'Artifact sacrifice', inPlay: 'Atog, Orcish Mechanics and Priest of Yawgmoth cash in artifacts for damage, cards or mana once trading stops.' },
    ],
  },
  LEG: {
    tagline: 'Big gold legends, walls and old-school combat tricks. Slow, grindy games where the ground stays locked until a multicolour legend or a flier breaks it open.',
    setting: 'Legends brought the first legendary creatures and gold cards to Dominaria: Elder Dragons, wandering heroes and lords.',
    gameplay: 'Games are slow. Commons are full of walls, Fog effects and odd combat tricks, and the cleanest common removal is red: Chain Lightning and Pyrotechnics. Legendary creatures fill much of the uncommon and rare sheets, nearly all of them two or three colours, and they usually decide the game once they land. Rampage and banding make blocks a puzzle, so every attack needs some arithmetic.',
    tips: [
      'Take Chain Lightning and Pyrotechnics highly; they are the format\'s best common removal and Pyrotechnics can kill two small creatures.',
      'Plan your colours around the legends you open; most are two or three colours, so a strong one is worth bending your deck for.',
      'Expect walls and Fog effects like Holy Day; fliers and landwalkers such as Lost Soul or Cat Warriors are how the ground gets broken.',
    ],
    tryIf: 'legendary creatures, gold cards and slow, thoughtful games',
    power: 3, speed: 1, complexity: 4,
    themes: ['legends', 'multicolor', 'bigThings'],
    mechanics: [
      { name: 'Legendary creatures', inPlay: 'Big multicolour threats, often in three colours, that pull drafts toward gold decks and decide games once resolved.' },
      { name: 'Gold cards', inPlay: 'The first multicolour cards, so mana fixing and staying in two or three colours matters more than in older sets.' },
      { name: 'Rampage', inPlay: 'Rampage creatures grow for each extra blocker, so double-blocking them is a trap; chump or trade one-for-one instead.' },
      { name: 'Walls', inPlay: 'Many common walls, plus Glyphs that reward blocking with them, keep the ground locked and games long.' },
      { name: 'Banding', inPlay: 'Banding and \'bands with other\' let attackers group up and control how damage is split, punishing big blockers.' },
    ],
  },
  DRK: {
    tagline: 'A low-powered pool where weak creatures, Goblins and cards that hurt their caster fight over the board. Games grind on until evasion or a rare two-for-one tips them.',
    setting: 'The Dark is set in Dominaria\'s dark age after the Brothers\' War: witch hunts, swamp folk and scrappy Goblin tribes.',
    gameplay: 'Power is low throughout, so ordinary creatures matter. Commons offer small Goblins, fliers like Bog Imp and Ghost Ship, and defensive pieces like Carnivorous Plant and Marsh Gas. Removal is scarce but strong: Ashes to Ashes exiles two creatures at the cost of 5 life, and Fissure kills anything. Several commons do little in Limited, such as Erosion, Riptide or Deep Water, so a deck with fifteen real creatures is ahead before the game starts.',
    tips: [
      'Ashes to Ashes is a common two-for-one; take it early and keep your life total high enough to cast it.',
      'Count playables, not power: plenty of commons are blanks here, so a solid, evasive creature is a high pick.',
      'Goblins of the Flarg and Marsh Goblins have mountainwalk and swampwalk; check your opponent\'s lands before choosing your blockers.',
    ],
    tryIf: 'low-power creature battles and old-school flavour',
    power: 2, speed: 3, complexity: 2,
    themes: ['tribal'],
    mechanics: [
      { name: 'Self-damaging spells', inPlay: 'Ashes to Ashes, Brothers of Fire and Elves of Deep Shadow trade your life for power, so racing decks punish them.' },
      { name: 'Goblins', inPlay: 'Cheap red Goblins and Goblin-only cards like Goblin Caves reward taking several, though the tribe\'s payoffs are modest.' },
      { name: 'Landwalk', inPlay: 'Swampwalk, mountainwalk and forestwalk creatures turn your opponent\'s land types into evasion.' },
      { name: 'Enemy-colour pairs', inPlay: 'Gold commons like Marsh Goblins and Scarwood Goblins reward unusual pairs such as black-red and red-green.' },
    ],
  },
  FEM: {
    tagline: 'Every colour has its own small tribe, from Thallids to Thrulls. Low-powered, grindy games of weenies, slow token engines and sacrifice tricks, won by whoever gets more out of their tribe.',
    setting: 'Fallen Empires follows the collapse of Sarpadia, where each colour\'s civilisation is torn apart by the creatures it bred or ruled.',
    gameplay: 'Creatures are small and removal is close to absent: Goblin Grenade needs a Goblin and the white Javelineers only ping. Boards fill with 1/1s and 2/2s, and each colour adds a slow engine on top. Thallids grow spores to make Saprolings, Thrulls sacrifice themselves for counters or mana, and Icatian soldiers lend each other first strike. Order of Leitbur and Order of the Ebon Hand, with protection from each other\'s colours, can hold a whole board on their own.',
    tips: [
      'Thallids need three upkeeps for each Saproling; play them early, as on turn eight they are just small bodies.',
      'Removal is very rare, so a protection creature like Order of the Ebon Hand can hold a lane all game; check colours before blocking.',
      'Hymn to Tourach is a common two-for-one; it is one of the few ways to get ahead in cards, so take it early.',
    ],
    tryIf: 'tribal tokens, sacrifice engines and slow little boards',
    power: 2, speed: 3, complexity: 2,
    themes: ['tribal', 'tokens', 'sacrifice'],
    mechanics: [
      { name: 'Spore counters', inPlay: 'Thallids tick up each upkeep and cash three spores for a 1/1 Saproling; slow, but they win long games.' },
      { name: 'Sacrifice outlets', inPlay: 'Thrulls, Goblin Grenade and the sac lands turn your own creatures into value, so dying in combat isn\'t always a loss.' },
      { name: 'Creature-type tribes', inPlay: 'Each colour\'s tribe has small lords or payoffs, rewarding a deck built mostly of one type.' },
      { name: 'Protection', inPlay: 'The Orders have protection from black or white, so they block and attack freely through the opposite colour.' },
    ],
  },
  POR: {
    tagline: 'Magic built to teach: big vanilla creatures, sorcery removal and almost no instants. Every trick is cast on your own turn, so games are clean, readable creature fights.',
    setting: 'Portal was Wizards\' 1997 introductory set, written in plain English for players who had never seen Magic.',
    gameplay: 'Almost everything is a sorcery, so combat has very few surprises: what you see on the board is what you fight. Creatures are mostly vanilla or nearly so, many red and black ones can\'t block, and haste is common in red. Removal is clean and sorcery-speed: Hand of Death, Path of Peace, Volcanic Hammer and Soul Shred are commons. Games are races of honest creatures, with fliers and pump sorceries like Monstrous Growth deciding them.',
    tips: [
      'Pump spells are sorceries, so cast them before attacking to make blocks bad, not to win a combat that is already declared.',
      'A handful of uncommon instants (Defiant Stand, Deep Wood, Command of Unsummoning) only work when you\'re attacked; watch for open mana.',
      'Creatures that can\'t block, like Craven Giant or Hulking Goblin, belong in an attacking deck; don\'t count them as defence.',
    ],
    tryIf: 'learning the game or a relaxed, straightforward match',
    power: 1, speed: 3, complexity: 1,
    themes: ['aggro', 'flyers'],
    mechanics: [
      { name: 'Sorcery-speed tricks', inPlay: 'Pump and removal happen on your own turn, so attacks are planned with full information and bluffing barely exists.' },
      { name: 'Attacked-only instants', inPlay: 'A few uncommon instants work only when you\'re attacked, so open mana on defence is a real but rare threat.' },
      { name: 'Can\'t block', inPlay: 'Cheap aggressive bodies that can\'t block push decks to race, so pick a plan and build for it.' },
      { name: 'Simple removal', inPlay: 'Hand of Death and Path of Peace kill anything relevant, so bombs are rarely safe and the best deck is usually the most solid.' },
    ],
  },
  P02: {
    tagline: 'Portal\'s sequel: simple creature combat with a little more bite, real tribes like Goblins and Nightstalkers, and common counterspells. Clean, quick games won on the board.',
    setting: 'Portal Second Age is set on Caliman, where Alaborn soldiers, Talas pirates, Norwood elves and Dakmor\'s undead clash.',
    gameplay: 'Like Portal, it\'s mostly sorceries, but blue now has common instant counters for creatures and sorceries, and several tribes have small payoffs. Removal is clean: Hand of Death, Cruel Edict, Path of Peace and Volcanic Hammer are all commons. Creatures are honest and slightly better than Portal\'s, with many that can\'t block in red, so games are fast races with fliers and pump sorceries deciding the close ones.',
    tips: [
      'Your opponent picks what Cruel Edict kills; cast it when they have one creature, or after trading off their small ones.',
      'Against open blue mana, False Summoning or Extinguish may be waiting; bait it with a lesser spell before the one you care about.',
      'Goblin and Rat decks scale: Goblin War Strike and Swarm of Rats grow with each copy, so take extras of the tribe once you\'re in.',
    ],
    tryIf: 'learning the game, or simple tribal drafts with some bite',
    power: 2, speed: 3, complexity: 1,
    themes: ['aggro', 'tribal'],
    mechanics: [
      { name: 'Sorcery-speed tricks', inPlay: 'Pump and removal happen on your turn, so combat is mostly open information and planning ahead pays off.' },
      { name: 'Common counterspells', inPlay: 'Extinguish and False Summoning are common instants, so blue can punish a key creature or sorcery.' },
      { name: 'Creature-type payoffs', inPlay: 'Goblins, Rats, Nightstalkers and Pirates have cards that count or reward their kind, worth drafting deeper.' },
      { name: 'Island-only attackers', inPlay: 'Steam Frigate and Armored Galleon only attack if you control an Island, so they are walls in other match-ups.' },
    ],
  },
  PTK: {
    tagline: 'Simple Portal rules plus horsemanship: cavalry that only other cavalry can block. Fast, aggressive races where evasive soldiers and clean sorcery removal decide the game.',
    setting: 'Portal Three Kingdoms retells the Romance of the Three Kingdoms: the armies, generals and strategists of Wei, Shu and Wu.',
    gameplay: 'Horsemanship creatures can only be blocked by other horsemanship creatures, so in practice they are unblockable, and four colours get them at common. Games become races of cavalry while ground soldiers trade or stall. Removal is plentiful at common, with Ghostly Visit, Imperial Edict, Misfortune\'s Gain and Fire Ambush, and the rare legendary generals are the bombs. Packs are small, so playable cards come thinner than in a modern set.',
    tips: [
      'Horsemanship is the evasion that matters; draft it highly, and keep a horsemanship creature home if you need to block theirs.',
      'Green has no horsemanship at common; it relies on big bodies, landwalking Zodiac creatures and Trip Wire to fight cavalry.',
      'Spend removal on cavalry first; ground creatures can be blocked, but a 2/2 horsemanship attacker hits you every turn.',
    ],
    tryIf: 'simple rules and fast, evasive aggro',
    power: 2, speed: 4, complexity: 2,
    themes: ['aggro', 'legends'],
    mechanics: [
      { name: 'Horsemanship', inPlay: 'Like flying that nothing else can block; races become cavalry versus cavalry and blocking turns are rare.' },
      { name: 'Legendary generals', inPlay: 'Rare legends such as Sun Quan, who gives your whole team horsemanship, end games quickly.' },
      { name: 'Zodiac landwalkers', inPlay: 'The Zodiac creatures carry landwalk, so your opponent\'s land types decide whether they can be blocked.' },
      { name: 'Sorcery-speed tricks', inPlay: 'Pump and removal are mostly sorceries, so combat is open information; a few attacked-only instants exist.' },
    ],
  },
  S99: {
    tagline: 'A starter set of honest creatures and simple spells, built as the step from Portal to full Magic. Fair, creature-focused games where fliers, clean removal and a few big rares decide most of them.',
    setting: 'Starter 1999 gathered reprints from Portal and other sets, plus a few new cards, to teach full Magic to new players.',
    gameplay: 'Creatures are mostly vanilla or close to it, and nearly every spell is a sorcery, so combat is straightforward. Fliers like Wind Drake and Wild Griffin carry many games, and removal is clean: Hand of Death, Path of Peace and Volcanic Hammer at common, with Jagged Lightning and Dark Offering at uncommon. Rares swing hard. Thorn Elemental, Wicked Pact or Fire Tempest can decide a match, and blue\'s counterspells are almost the only interaction on the opponent\'s turn.',
    tips: [
      'Fliers are the main evasion; draft enough of them, or reach creatures like Norwood Archers, to win or stop the air race.',
      'Sorcery removal can\'t save you mid-combat, so kill the threat on your turn before it attacks, not after.',
      'Against open blue mana, Remove Soul may be waiting; lead with a lesser creature before your best one.',
    ],
    tryIf: 'learning the game or a quick, simple draft',
    power: 2, speed: 3, complexity: 1,
    themes: ['aggro', 'flyers'],
    mechanics: [
      { name: 'Evergreen keywords', inPlay: 'Flying, haste, vigilance and landwalk are the only keywords, so creature size and evasion decide combat.' },
      { name: 'Sorcery-speed spells', inPlay: 'Almost all spells are sorceries, so the board is open information and combat rarely surprises.' },
      { name: 'Simple removal', inPlay: 'Hand of Death and Path of Peace answer anything, so even bombs are rarely safe.' },
    ],
  },
  INV: {
    tagline: 'Slow, removal-rich games where creatures are modest and spells do the heavy lifting. Two-for-ones, gold cards and how well you fix your mana decide who wins.',
    setting: 'The Phyrexian invasion of Dominaria, with allied colours banding together against it.',
    gameplay: 'Games develop at a measured pace: the creatures are mostly ordinary, while the spells are strong and removal is easy to find at common. A large share of cards give you two-for-ones, so the long game is a war of attrition decided by card advantage and a few powerful gold cards. Mana fixing is scarcer than the multicolour theme suggests, and stumbling on colours is the most common way to lose.',
    tips: [
      'Take fixing early. Dream Thrush, the allied tap lands and green\'s land fetchers are harder to find than playables.',
      'Almost any creature is playable here, so spend early picks on removal, card advantage and gold cards.',
      'Kicker spells are cheap early plays and big late ones. Keep a little mana back in the late game to pay the kicker.',
    ],
    tryIf: 'removal-heavy attrition and stretching your mana for strong gold cards',
    power: 4, speed: 2, complexity: 3,
    themes: ['multicolor', 'spells'],
    mechanics: [
      { name: 'Gold cards', inPlay: 'Powerful two-colour cards pull you toward an allied pair, and the best ones are worth stretching your mana for a third colour.' },
      { name: 'Kicker', inPlay: 'Every kicker card has a cheap and an expensive mode, so few draws are dead and long games reward flexible hands.' },
      { name: 'Domain', inPlay: 'Rewards each basic land type you control, pushing greedy multicolour decks to splash further than they normally would.' },
      { name: 'Split cards', inPlay: 'Two spells on one card, like Fire // Ice, give you a choice of effect every time you draw them.' },
    ],
  },
  ONS: {
    tagline: 'A board full of face-down 2/2s where every attack is a guess. Morph bluffs, scarce removal and cycling make for tense, skill-testing combat.',
    setting: 'Otaria, a plane where Goblins, Elves, Clerics, Soldiers, Wizards and Zombies care about their own kind.',
    gameplay: 'Most turns from three onward put another morph on the table, so the board quickly fills with identical 2/2s that might be anything. Combat is a bluffing game about how much mana your opponent has open. Removal is scarce and fiercely drafted, which makes red and black contested; cheap answers that kill a face-down creature, such as Sparksmith and Shock, are at their best here.',
    tips: [
      'Sparksmith is the format\'s defining common. Take it, and cheap burn that kills a 2/2 morph, very highly.',
      'Count open mana before blocking a morph. Five mana up means it could flip into almost anything.',
      'Draft colours and good cards first; creature types usually follow, and Clerics are the closest thing to a true tribal deck.',
    ],
    tryIf: 'face-down bluffing and reading your opponent\'s open mana',
    power: 3, speed: 2, complexity: 3,
    themes: ['tribal'],
    mechanics: [
      { name: 'Morph', inPlay: 'Three mana buys a mystery 2/2 that can flip mid-combat, so attacks and blocks turn into reads on the opponent\'s open mana.' },
      { name: 'Cycling', inPlay: 'Cards trade themselves for a fresh draw, so floods and dead cards are rarer and some cycle for a small effect.' },
      { name: 'Creature types', inPlay: 'Lords and type-counting cards like Sparksmith and Timberwatch Elf get stronger the more of one type you and your opponent have.' },
    ],
  },
  LGN: {
    tagline: 'Nothing but creatures: no spells, no removal sorceries. Games are decided by who builds the better board, and which morphs and cycled Gempalms clear the way.',
    setting: 'Onslaught block\'s second set on Otaria, made entirely of creatures: tribal bodies, Slivers and morphs.',
    gameplay: 'With no instants or sorceries, every card is a creature, so games are pure board development and combat. The interaction comes from creatures: morphs that kill or bounce when they flip, Gempalms that cycle for an effect, and provoke creatures that drag a blocker into a bad fight. Most of this removal needs setting up, so boards clog and evasive fliers and tribal lords tend to break the stall.',
    tips: [
      'Prize the few creatures that kill things: Skirk Marauder, Gempalm Incinerator and Gempalm Polluter are removal in a set with almost none.',
      'With no combat tricks, the only surprises are morph flips and cycled Gempalms. Track your opponent\'s open mana.',
      'Fliers win stalled boards. Cheap Soldiers and Birds are the most reliable way to close games.',
    ],
    tryIf: 'pure creature combat with no spells to hide behind',
    power: 2, speed: 2, complexity: 3,
    themes: ['tribal', 'flyers'],
    mechanics: [
      { name: 'Morph', inPlay: 'Many morphs do something when turned face up, so a flip is the closest thing the set has to a removal spell or trick.' },
      { name: 'Provoke', inPlay: 'Attacking forces a chosen creature to block, letting you pick off utility creatures or trade your way through a board.' },
      { name: 'Gempalm cycling', inPlay: 'The Gempalm cycle cycles for an effect scaled by your creature type, an instant-speed answer hidden in a creature.' },
      { name: 'Slivers', inPlay: 'Every Sliver shares its ability with the rest, so each new one strengthens the team, for your opponent too.' },
      { name: 'Amplify', inPlay: 'Reveal creatures of the same type from hand to make it bigger, a trade of card information for size.' },
    ],
  },
  SCG: {
    tagline: 'Big mana, big creatures and Dragons. Landcycling keeps you hitting drops, games run long, and a single huge flier or board wipe often decides them.',
    setting: 'Onslaught block\'s finale on Otaria, where the Dragons return.',
    gameplay: 'Scourge pushes toward expensive cards: Dragons, big green creatures and cards that reward your highest mana value. Landcycling creatures turn early draws into lands and late draws into large bodies, so decks hit their land drops and reach the top of the curve. Removal is better than in Legions, with cards like Carbonize, Torrent of Fire and Wing Shards, and the long games tend to end with whoever lands the bigger flier or a Decree.',
    tips: [
      'Landcyclers like Twisted Abomination are both lands and finishers. Play them as part of your land count.',
      'Wing Shards and other storm spells get much stronger when cast after a cheap spell or two. Sequence for it.',
      'Expect long games. Prioritise removal that handles a big flier over cheap aggressive creatures.',
    ],
    tryIf: 'dragons, big spells and long games decided by the top of the curve',
    power: 3, speed: 2, complexity: 3,
    themes: ['bigThings', 'tribal', 'flyers'],
    mechanics: [
      { name: 'Landcycling', inPlay: 'Creatures cycle for a basic land early, so you rarely miss a drop and still have a big threat in the late game.' },
      { name: 'Storm', inPlay: 'Copies for each spell cast earlier in the turn; in Limited it mostly turns Wing Shards and friends into two-for-ones.' },
      { name: 'Mana value matters', inPlay: 'Cards count your biggest mana value or care about high costs, rewarding decks built around expensive threats.' },
      { name: 'Morph', inPlay: 'Still around from Onslaught, so face-down 2/2s and flip tricks remain part of every board.' },
    ],
  },
  MRD: {
    tagline: 'An artifact-heavy format where tempo rules. Cheap Equipment like Bonesplitter turns small creatures into threats, and decks that waste early turns fall behind fast.',
    setting: 'Mirrodin, an artificial plane of metal where artifacts fill every colour.',
    gameplay: 'Games reward using your mana every turn from the start. Equipment survives creature trades and moves on to the next body, so aggressive decks keep their pressure even through removal. Every colour plays artifacts, which makes artifact removal like Shatter maindeckable and turns affinity creatures into cheap threats. Red is widely considered the best colour, and decks that drift past turn eight or ten without a plan tend to lose to bombs.',
    tips: [
      'Bonesplitter is the format\'s best common. Take it, and other cheap Equipment, over most creatures.',
      'Artifact removal is maindeckable here. Shatter and Deconstruct hit Equipment, Myr and affinity threats alike.',
      'Count artifacts, not just lands, when building affinity: Frogmite and Myr Enforcer need a board of them to be cheap.',
    ],
    tryIf: 'artifacts and equipment-fuelled aggro',
    power: 4, speed: 4, complexity: 3,
    themes: ['artifacts', 'aggro'],
    mechanics: [
      { name: 'Equipment', inPlay: 'Equipment outlives the creature wearing it, so trades favour the side with Equipment and evasive fliers become fast clocks.' },
      { name: 'Affinity', inPlay: 'Each artifact you control makes these cards cheaper, letting artifact-dense decks empty their hands early.' },
      { name: 'Spellbombs', inPlay: 'Cheap artifacts with a small effect or a cycle, counting toward affinity and artifact payoffs.' },
      { name: 'Imprint', inPlay: 'Exile a card under an artifact to set what it does, turning spare cards into lasting effects.' },
      { name: 'Entwine', inPlay: 'Pay extra to get both modes of a spell, a choice between an early play and a late two-for-one.' },
    ],
  },
  CHK: {
    tagline: 'Careful, combat-focused games where bushido samurai punish blocks and Spirits recycle themselves. White is deep, removal is uneven, and big green creatures rule long games.',
    setting: 'Kamigawa, a Japanese-inspired plane where mortals fight a war against the angry spirits.',
    gameplay: 'Games are deliberate. Creatures are mostly fair-sized, bushido makes attacking and blocking samurai costly to read, and soulshift Spirits return smaller Spirits when they die, so trades often leave one player ahead. Removal is uneven: white\'s Cage of Hands, red\'s Glacial Ray and Yamabushi\'s Flame and blue\'s Mystic Restraints are good, while black\'s commons are expensive or conditional. Green\'s big Spirits and ramp tend to take over if the game drags on.',
    tips: [
      'White is the deepest colour. Kitsune Blademaster, Cage of Hands and Kabuto Moth are all strong commons.',
      'Treat bushido creatures as bigger than they look in combat: they gain the bonus whether they block or are blocked.',
      'Rend Spirit and Rend Flesh only hit one side of the Spirit line. Check what your opponent is playing before relying on them.',
    ],
    tryIf: 'samurai, spirits and careful combat',
    power: 3, speed: 2, complexity: 3,
    themes: ['tribal', 'spells'],
    mechanics: [
      { name: 'Bushido', inPlay: 'Samurai grow when they block or are blocked, so they attack well and make double blocks and trades hard to judge.' },
      { name: 'Soulshift', inPlay: 'A dying Spirit returns a cheaper Spirit from your graveyard to hand, so Spirit decks win long, grindy trading games.' },
      { name: 'Splice onto Arcane', inPlay: 'Add a spliceable card\'s effect onto another Arcane spell while keeping it in hand, turning cheap spells into repeatable value.' },
      { name: 'Spirit vs non-Spirit', inPlay: 'Many cards care whether a creature is a Spirit, so removal like Rend Spirit and Rend Flesh depends on the matchup.' },
      { name: 'Flip cards', inPlay: 'Legends that transform into a stronger form when their condition is met, a slow build-around threat.' },
    ],
  },
  RAV: {
    tagline: 'Two-colour guild decks with deep, interchangeable commons. Boros races, Selesnya goes wide, Golgari and Dimir grind, and Signets and karoos make a splash easy.',
    setting: 'Ravnica, a plane-wide city ruled by guilds; this set introduces Boros, Selesnya, Golgari and Dimir.',
    gameplay: 'You draft around one of four guilds, and each plays very differently: Boros curves out with fliers, double strikers and burn, Selesnya swarms and convokes out big spells, Golgari dredges value from its graveyard and Dimir tutors for answers. Commons are deep in every colour, and Signets and the common bounce lands make a light third colour easy. Removal is good but not abundant, so games are interactive and usually decided on the board.',
    tips: [
      'Take premium removal early: Lightning Helix, Faith\'s Fetters, Galvanic Arc and Last Gasp are the cards that swing games.',
      'Bounce lands are good but cost tempo when they enter. Don\'t let one pull you into a weaker guild.',
      'Hybrid and gold guild cards signal what\'s open. Take your guild\'s early so neighbours don\'t cut you.',
    ],
    tryIf: 'guild decks and two-colour synergies',
    power: 4, speed: 3, complexity: 3,
    themes: ['multicolor', 'graveyard', 'tokens'],
    mechanics: [
      { name: 'Convoke', inPlay: 'Tap your creatures to help pay, so a wide board casts big spells early, at the cost of attacking that turn.' },
      { name: 'Dredge', inPlay: 'Replace a draw with a card back from your graveyard, so Golgari decks keep their best creatures coming back.' },
      { name: 'Radiance', inPlay: 'Effects spread to every creature sharing a colour with the target, which can hit both sides of the board.' },
      { name: 'Transmute', inPlay: 'Discard a card to tutor one of the same mana value, letting Dimir find its best removal or bomb.' },
      { name: 'Hybrid mana', inPlay: 'Hybrid cards can be cast with either colour, so they fit more decks and are easy to pick up early.' },
    ],
  },
  LRW: {
    tagline: 'One of the most intricate drafts: eight tribes, changelings and instant-speed Faeries mean many decisions every turn. Staying in a tribe matters more than raw card quality.',
    setting: 'Lorwyn, a sunlit fairy-tale plane of Kithkin, Merfolk, Faeries, Elves, Giants, Elementals, Goblins and Treefolk.',
    gameplay: 'Decks are built around one or two tribes, and the tribal cards build on each other, so a deck that ignores types ends up weak. Kithkin rush with cheap creatures, Faeries flash in fliers at instant speed, Giants and Treefolk take over the ground late, and Elementals trade evoke effects for card advantage. Removal like Nameless Inversion, Eyeblight\'s Ending and Oblivion Ring is solid, and many cards ask you to track types, reveals and clashes.',
    tips: [
      'Pick a tribe or two and stay in them; a deck of on-type cards beats a pile of stronger cards that share no types.',
      'Mulldrifter is a bomb common. Evoke it early for cards or hardcast it for a flier.',
      'Faerie players hold mana open for flash creatures. Attacking into an untapped blue deck is risky.',
    ],
    tryIf: 'tribal synergy and puzzling over every turn',
    power: 4, speed: 3, complexity: 5,
    themes: ['tribal'],
    mechanics: [
      { name: 'Tribal types', inPlay: 'Lords and payoffs count or reveal your creature types, so your draft picks are judged by type as much as by power.' },
      { name: 'Changeling', inPlay: 'Changelings have every creature type, so they fill any tribe\'s gaps and turn on any lord.' },
      { name: 'Evoke', inPlay: 'Cast an Elemental cheaply for its enter effect and let it die, choosing between a fast answer and a body.' },
      { name: 'Champion', inPlay: 'A strong creature exiles one of yours until it leaves, a cheap powerhouse that risks a two-for-one.' },
      { name: 'Clash', inPlay: 'Both players reveal their top card for a bonus, which also lets you send bad draws to the bottom.' },
    ],
  },
  KTK: {
    tagline: 'Three-colour clan decks and boards full of morphs. Turns three to five decide most games, outlast and big flips stall the board, and evasion or reach breaks it.',
    setting: 'Tarkir, a dragonless plane ruled by five warring three-colour clans.',
    gameplay: 'Nearly every game sees each player drop a morph on turn three, and the middle turns are a tense guessing game about what each 2/2 will become. Generous fixing from gain lands and banners makes three-colour decks the norm, and greedier decks splash further at the cost of speed. Once big morphs flip and outlast creatures grow, boards stall, so fliers, burn and cards like Arrow Storm that push damage through tend to decide games.',
    tips: [
      'Don\'t overvalue morphing. Playing a creature face up on curve is often better tempo than casting it face down.',
      'Bounce is strong: hitting a morph your opponent just paid to flip costs them their whole turn.',
      'Pack evasion or reach. Outlast creatures and big morphs stall the ground, and fliers or Arrow Storm finish games.',
    ],
    tryIf: 'three-colour decks and face-down morph mind games',
    power: 3, speed: 3, complexity: 4,
    themes: ['multicolor', 'graveyard', 'counters'],
    mechanics: [
      { name: 'Morph', inPlay: 'Any card can be a three-mana 2/2, so turn three is always a play and every attack is a read on open mana.' },
      { name: 'Raid', inPlay: 'Bonuses if you attacked this turn, rewarding Mardu for pushing in even with imperfect attacks.' },
      { name: 'Delve', inPlay: 'Exile cards from your graveyard to pay, so Sultai cast big spells cheaply after a few trades.' },
      { name: 'Prowess', inPlay: 'Creatures grow with each noncreature spell, so Jeskai\'s cheap spells double as combat tricks.' },
      { name: 'Outlast', inPlay: 'Sorcery-speed +1/+1 counters give Abzan a mana sink that slowly out-grows the board.' },
    ],
  },
  DOM: {
    tagline: 'A slow, value-driven format with weak two-drops and plenty of common removal. Sagas, kicker and strong uncommons reward patient, card-advantage decks.',
    setting: 'A return to Dominaria, Magic\'s home plane, with Sagas retelling its history.',
    gameplay: 'Two-drops are weaker than usual, so aggro struggles and games build toward the mid and late game. Removal is plentiful at common, with Shivan Fire, Eviscerate, Vicious Offering and Blessed Light, which means most bombs can be answered. Boards stall readily, so fliers like Cloudreader Sphinx, kicked spells and Sagas that grind out value over three turns tend to decide who comes out ahead.',
    tips: [
      'Run eighteen lands in most decks. Kicker gives every extra land a use, and the format is slow enough to reward it.',
      'Shivan Fire, Eviscerate and Blessed Light are the commons to take early; most bombs can be answered with them.',
      'Count your historic cards before taking payoffs. Around eight artifacts, legends and Sagas makes them reliable.',
    ],
    tryIf: 'legends, Sagas and slower value-driven games',
    power: 3, speed: 2, complexity: 3,
    themes: ['legends', 'artifacts', 'flyers'],
    mechanics: [
      { name: 'Kicker', inPlay: 'Cards work early and scale late, so flooding is rarely a problem and extra lands are welcome.' },
      { name: 'Sagas', inPlay: 'Enchantments that deliver a chapter each turn, scheduling value your opponent can see coming but rarely stop.' },
      { name: 'Historic', inPlay: 'Payoffs trigger on artifacts, legends and Sagas, rewarding decks that pick up enough of them.' },
      { name: 'Legendary sorceries', inPlay: 'Powerful but only castable with a legendary creature or planeswalker out, so they need a deck built for them.' },
    ],
  },
  THB: {
    tagline: 'A slow, grindy enchantment format where Aura removal and escape recursion reward the deck that still has something to do on turn ten.',
    setting: 'Greek-myth Theros, where Elspeth escapes the Underworld past gods who grow stronger with their followers\' devotion.',
    gameplay: 'Games are slow and full of value. Two-drops are weak, so boards build up through the middle turns and the player with more card advantage pulls ahead. Much of the best common removal is an Aura, such as Mire\'s Grasp and Dreadful Apathy, which makes enchantment answers like Return to Nature live. Escape cards come back late to break stalls, so both players\' graveyards matter.',
    tips: [
      'Take Mire\'s Grasp and Dreadful Apathy early: black and white Aura removal is the format\'s best common interaction, and black runs deepest in it.',
      'Value escape cards and self-mill like Funeral Rites; a threat escaped on turn eight often wins a stalled board.',
      'Heroic is the aggressive plan, but stacking Auras and tricks on one creature into open black mana hands your opponent a two-for-one.',
    ],
    tryIf: 'grindy games, Aura removal and graveyard recursion',
    power: 3, speed: 2, complexity: 3,
    themes: ['graveyard', 'counters', 'bigThings'],
    mechanics: [
      { name: 'Escape', inPlay: 'Cards climb back out of the graveyard by exiling others, so long games favour whoever fuelled their yard, and grave hate gains value.' },
      { name: 'Constellation', inPlay: 'Rewards each enchantment you land; Omens and enchantment creatures double as triggers, so count enablers before taking payoffs.' },
      { name: 'Devotion', inPlay: 'Heavy commitment to one colour wakes the gods and powers devotion payoffs, which makes splashing costlier than usual.' },
      { name: 'Heroic', inPlay: 'Targeting your own creature with Auras and tricks grows it, rewarding aggression but leaving you exposed to removal in response.' },
    ],
  },
  WAR: {
    tagline: 'A planeswalker in every pack: boards fill with walkers to attack and defend, Zombie Armies grow with amass, and proliferate pushes counters and loyalty higher every turn.',
    setting: 'Ravnica under invasion, as Nicol Bolas and his Dreadhorde of Eternals turn the guild city into a battlefield for the Gatewatch and their allies.',
    gameplay: 'Every pack holds a planeswalker, so most games feature several: attacking them down is often worth more than hitting the player, and holding back blockers to protect your own matters. Blue-black and black-red amass a single Zombie Army that keeps growing, green-white and green-blue proliferate +1/+1 counters and loyalty, white-black drains whenever creatures and walkers die, and red-white rewards targeting its own creatures. Cheap removal like Despark and Tyrant\'s Scorn keeps the rare walkers in check.',
    tips: [
      'Uncommon planeswalkers are strong picks: their static ability works the moment they land, and the minus ability is usually a spell in its own right.',
      'Attack planeswalkers early; a walker left alone for two turns usually pays for itself several times over.',
      'Amass cards all grow the same Army, so a deck full of them turns several small effects into one big threat.',
    ],
    tryIf: 'planeswalker-heavy Limited, counters and proliferate',
    power: 4, speed: 3, complexity: 4,
    themes: ['counters', 'tokens', 'multicolor'],
    mechanics: [
      { name: 'Planeswalkers', inPlay: 'One in every pack; combat becomes a question of whether to hit the player or the walker, and protecting your own walker shapes blocks.' },
      { name: 'Amass', inPlay: 'Puts +1/+1 counters on your one Zombie Army, creating it first if needed, so every amass card stacks into the same growing creature.' },
      { name: 'Proliferate', inPlay: 'Adds one more of each kind of counter you choose, growing +1/+1 counters, Armies and planeswalker loyalty at once.' },
    ],
  },
  M20: {
    tagline: 'A classic core-set format with an Elemental twist: solid two-colour decks, cheap removal and a handful of rare bombs, with Elementals tying blue, red and green together.',
    setting: 'A core set with no plane, headlined by Chandra, the five Cavaliers and an Elemental subtheme.',
    gameplay: 'Most games are decided on the board: creatures trade, fliers race and the deck with more removal pulls ahead. Elementals give red-green an aggressive tribal plan and green-blue a ramp plan around Risen Reef, while white-blue goes to the air, black-white drains with every creature that enters and black-green turns creatures dying into cards. A common land slot of gain lands and Evolving Wilds makes splashing cheap.',
    tips: [
      'Pick removal early: Disfigure, Shock, Murder and Pacifism are what break board stalls.',
      'Elemental decks want the tribe: Creeping Trailblazer and Risen Reef get much better with every other Elemental you draft.',
      'Gain lands and Evolving Wilds come in the land slot, so a light splash for a bomb rare costs little.',
    ],
    tryIf: 'clean, classic Limited and Elemental tribal decks',
    power: 3, speed: 3, complexity: 2,
    themes: ['tribal', 'flyers', 'lifegain'],
    mechanics: [
      { name: 'Elementals', inPlay: 'Blue, red and green Elementals reward each other, from Risen Reef digging lands to Creeping Trailblazer pumping the team.' },
      { name: 'Evergreen keywords', inPlay: 'Flying, deathtouch, lifelink and friends do most of the work; reading combat well matters more than any set mechanic.' },
      { name: 'Planeswalkers', inPlay: 'Rare walkers take over an unattended board; attack them early or they generate value every turn.' },
    ],
  },
  ELD: {
    tagline: 'A fast, two-for-one format: Adventure cards give two spells for one card, Food buys time, and aggressive Knight decks punish slow starts.',
    setting: 'Eldraine, a fairy-tale plane of five courtly castles and the wild, enchanted realm beyond them.',
    gameplay: 'Adventure creatures cast their spell half first and come back from exile later as a creature, so card advantage is everywhere and trading one-for-one rarely keeps up. Knight decks in red, white and black curve out and race, while Food decks gain life and grind with Bake into a Pie and Giant Opportunity. Blue-black mills both players for Drown in the Loch, and mono-coloured payoffs reward heavy commitment to one colour.',
    tips: [
      'Value Adventure cards highly: a removal spell or trick that later becomes a creature is two cards in one.',
      'Respect the Knight aggro decks; a curve of cheap creatures backed by Searing Barrage or Scorching Dragonfire ends games by turn six.',
      'Lean into one colour: adamant cards get their bonus when three mana of one colour is spent, so they shine in a deck heavy in that colour.',
    ],
    tryIf: 'Adventure value, Knight aggro and fairy-tale flavour',
    power: 3, speed: 4, complexity: 3,
    themes: ['aggro', 'tribal', 'graveyard'],
    mechanics: [
      { name: 'Adventure', inPlay: 'Cast the spell half from your hand, then the creature from exile later, so one card does two jobs and games turn into card-advantage races.' },
      { name: 'Food', inPlay: 'Artifact tokens you sacrifice for 3 life; they buy time against aggro and fuel payoffs that want Food on the board.' },
      { name: 'Adamant', inPlay: 'Spells and creatures get a bonus when three mana of one colour paid for them, rewarding near mono-coloured decks.' },
      { name: 'Mill', inPlay: 'Blue-black fills both graveyards to power Drown in the Loch and graveyard-counting threats; milling out is a real way to win.' },
    ],
  },
  IKO: {
    tagline: 'A high-power format packed with cheap removal: mutate stacks and big monsters meet plenty of answers, cheap creatures pull their weight, and cycling keeps draws useful.',
    setting: 'Ikoria, a plane of kaiju-sized monsters and the humans who hunt or bond with them.',
    gameplay: 'Every colour but green has good one- or two-mana common removal, such as Blood Curdle, Dead Weight and Pacifism, so big creatures die often and games run longer than the monsters suggest. Mutate builds one huge threat that either takes over or becomes a two-for-one for the opponent. Cycling smooths draws and feeds its payoffs, and cheap creatures that trade with expensive ones are worth more than their size.',
    tips: [
      'Prioritise cheap creatures: with removal this good, a two-drop that trades beats a six-drop that dies to Dead Weight.',
      'Mutate onto non-Humans that survive removal, or hold protection like Keep Safe; losing a mutated stack to one removal spell costs two cards.',
      'Cycling is real card quality here; decks with several cyclers often run 16 lands, and some go to 15.',
    ],
    tryIf: 'monster-sized creatures, mutate stacking and removal-heavy games',
    power: 4, speed: 3, complexity: 4,
    themes: ['multicolor', 'counters', 'bigThings'],
    mechanics: [
      { name: 'Mutate', inPlay: 'Merges a creature on top of a non-Human for one big threat with every ability; it dominates combat but loses two cards to one removal spell.' },
      { name: 'Cycling', inPlay: 'Trades dead cards for new ones, so flooding and screw bite less, and cycling payoffs turn each cycle into damage or value.' },
      { name: 'Keyword counters', inPlay: 'Flying, lifelink or deathtouch counters stack on one creature, making it a must-answer threat and a removal magnet.' },
      { name: 'Companion', inPlay: 'A rare companion is a free extra card if your deck meets its condition; weigh that against what the restriction costs you.' },
    ],
  },
  M21: {
    tagline: 'Back-to-basics Limited where cheap creatures and combat tricks set the pace; the deck with the better curve and removal usually wins, unless a bomb lands first.',
    setting: 'A core set with no plane, headlined by Teferi, Basri, Chandra, Liliana and Garruk.',
    gameplay: 'Games are quicker than the plain cards suggest: two- and three-drops are the premium commons, and decks that curve out pressure opponents before slow plans come online. Red and black have the cleanest common removal in Scorching Dragonfire, Shock and Grasp of Darkness. Creatures are small, so cheap tricks swing combat. Big rares still decide games when they land, but control decks struggle to survive long enough to cast them.',
    tips: [
      'Draft a deep curve of two- and three-drops; clunky hands get run over, so mulligan with an aggressive opponent in mind.',
      'Combat tricks matter more than usual: creatures are small, and Feat of Resistance, Titanic Growth or Sure Strike regularly wins a trade.',
      'Blue and black commons are thinner, so pair them with red, white or green rather than counting on them to carry a deck alone.',
    ],
    tryIf: 'classic two-colour Limited, tight curves and big dragons',
    power: 4, speed: 4, complexity: 2,
    themes: ['flyers', 'counters', 'lifegain', 'bigThings'],
    mechanics: [
      { name: 'Evergreen keywords', inPlay: 'Flying, first strike, deathtouch and friends do the work; reading combat well matters more than any set mechanic.' },
      { name: '+1/+1 counters', inPlay: 'Basri\'s cards and Conclave Mentor grow creatures over time, rewarding decks that hold the board and keep attacking.' },
      { name: 'Planeswalkers', inPlay: 'Rare walkers take over an unattended board; attack them early or they generate value every turn.' },
    ],
  },
  ZNR: {
    tagline: 'One of the fastest formats here: curve out, attack every turn, and use landfall, kicker and party to finish before removal or bombs can catch up.',
    setting: 'Zendikar\'s ruins, explored by adventuring parties of Clerics, Rogues, Warriors and Wizards.',
    gameplay: 'Games are short and aggressive. Cheap creatures attack from turn two, landfall turns each land drop into extra damage, and kicker lets a spell be cheap early or bigger later. Modal double-faced spell/lands let you run fewer basics and still hit drops. Common removal is modest, led by Nahiri\'s Binding, Practiced Tactics, Feed the Swarm and Roil Eruption, so a fast start often goes unanswered.',
    tips: [
      'Count spell/land cards toward your land total; they let you trim a basic or two without risking mana screw.',
      'Party payoffs want one each of Cleric, Rogue, Warrior and Wizard; a balanced mix of types beats a pile of one.',
      'Green was the weakest colour on its own; take it only when it is clearly flowing, and lean on black, red and white aggression.',
    ],
    tryIf: 'fast aggressive decks, landfall and kicker',
    power: 3, speed: 5, complexity: 3,
    themes: ['aggro', 'tribal'],
    mechanics: [
      { name: 'Party', inPlay: 'Payoffs scale with how many of Cleric, Rogue, Warrior and Wizard you control, so drafting means balancing creature types.' },
      { name: 'Landfall', inPlay: 'Every land drop pumps or triggers your creatures, so holding a land until combat is a real play.' },
      { name: 'Kicker', inPlay: 'Spells are cheap on curve or stronger late, so they stay useful whether you\'re racing or flooding.' },
      { name: 'Spell/land MDFCs', inPlay: 'Each one is a spell or a tapped land, cutting both flood and screw and letting decks play fewer basics.' },
    ],
  },
  KHM: {
    tagline: 'Aggressive decks race while snow decks stall for their payoffs; drafting snow-covered lands is as important as drafting spells, and foretell makes every turn a plan.',
    setting: 'The Norse-inspired realms of Kaldheim, where clans of Elves, Giants, Berserkers and Zombies war.',
    gameplay: 'Aggro decks push early damage with boast creatures, tricks and equipment, while snow decks stall the board until their payoffs take over. Foretell lets you spend spare mana early and cast spells cheaply later, hiding information and making turns lumpy. Common removal is decent: Demon Bolt, Feed the Serpent, Frost Bite and Squash. Snow-covered lands come in packs and must be drafted, so snow payoffs live or die by how many you picked.',
    tips: [
      'Take snow lands over marginal playables, especially in packs two and three; snow cards like Icehide Troll are weak without them.',
      'A face-down foretold card is information: an opponent who foretold on turn two may be holding Demon Bolt for your best creature.',
      'Black leans on snow and synergy more than the other colours; draft it as part of a snow deck rather than on its own.',
    ],
    tryIf: 'snow mana, tribal clans and planning turns ahead',
    power: 4, speed: 3, complexity: 4,
    themes: ['tribal', 'multicolor'],
    mechanics: [
      { name: 'Snow', inPlay: 'Snow lands are drafted like spells and power payoffs; the more you take, the stronger your snow cards and the weaker theirs.' },
      { name: 'Foretell', inPlay: 'Pay two early to exile a card face down, then cast it cheaply later; it smooths the curve and keeps opponents guessing.' },
      { name: 'Boast', inPlay: 'Attacking creatures get a once-per-turn activated bonus, which pushes aggressive decks to attack into awkward blocks.' },
      { name: 'Changeling', inPlay: 'Shapeshifters count as every creature type, filling gaps in tribal decks and dodging type-based effects.' },
      { name: 'Sagas', inPlay: 'Chapter-by-chapter enchantments that deliver value over three turns; the rare ones are often game-changing.' },
    ],
  },
  STX: {
    tagline: 'A spells-matter format where card advantage and two-colour college decks rule, Lessons in your sideboard add options, and Mystical Archive classics raise the power.',
    setting: 'Strixhaven, a magic university where five two-colour colleges compete.',
    gameplay: 'Games lean toward card advantage: blue-based decks with strong late games do well, while aggressive Silverquill-style decks try to punish greedy draws. Decks run fewer creatures than usual, padding the count with token-making Summoning spells and Learn. Removal is spread across the colours, and each pack carries a Mystical Archive card, so classic spells like Lightning Bolt show up often.',
    tips: [
      'Keep a real creature count: Summoning spells and Learn cards that fetch creatures help, but a deck of only spells loses to an early clock.',
      'Put a few Lessons in your sideboard; Learn turns each one into the right answer or extra card at the right moment.',
      'Fixing is mostly each college\'s own dual land, so commit to a college and keep splashes light; three-colour decks are rare.',
    ],
    tryIf: 'instants and sorceries, magecraft and toolbox sideboards',
    power: 3, speed: 3, complexity: 4,
    themes: ['spells', 'multicolor'],
    mechanics: [
      { name: 'Magecraft', inPlay: 'Triggers whenever you cast or copy an instant or sorcery, so cheap spells double as creature pumps, drain or damage.' },
      { name: 'Learn & Lessons', inPlay: 'Learn fetches a Lesson from your sideboard or rummages, so sideboard building becomes part of deckbuilding.' },
      { name: 'Ward', inPlay: 'Ward taxes targeted removal; plan the extra mana or reach for non-targeted answers.' },
      { name: 'Mystical Archive', inPlay: 'A classic instant or sorcery in every pack raises the pool\'s power and can pull you into a colour.' },
    ],
  },
  AFR: {
    tagline: 'Value-driven midrange with plenty of removal: dungeons and Treasure grind out extra cards, pack tactics rewards big attacks, and dragons close out the long games.',
    setting: 'Dungeons & Dragons\' Forgotten Realms, with dungeon crawls, dice rolls and dragons.',
    gameplay: 'Most games are midrange: boards develop through the middle turns, and value from venturing, Treasure and two-for-ones tips the balance. Red and black were widely seen as the strongest colours, with black commons like Grim Bounty, Precipitous Drop and Deadly Dispute providing removal and card advantage. Pack tactics rewards attacking with a large total power, and flyers and dragons finish games once the ground clogs up.',
    tips: [
      'Take black and red removal early; Grim Bounty and Precipitous Drop are commons, and the colours\' depth made them the ones to be in.',
      'Choose a dungeon for your plan: Lost Mine of Phandelver for steady value, Tomb of Annihilation to finish quickly at a cost in life.',
      'Pack tactics needs six total attacking power, so draft enough three- and four-power attackers rather than a deck of 2/2s.',
    ],
    tryIf: 'D&D flavour, dungeon crawling and grindy midrange',
    power: 3, speed: 2, complexity: 3,
    themes: ['flyers', 'artifacts'],
    mechanics: [
      { name: 'Venture', inPlay: 'Each venture moves you a room through a dungeon for a small bonus; completing one adds value and powers some cards.' },
      { name: 'Pack tactics', inPlay: 'Triggers when you attack with six or more total power, rewarding wide attacks and punishing timid blocks.' },
      { name: 'Roll a d20', inPlay: 'Dice add variance on top of a reliable floor; plan for the low roll and enjoy the high one.' },
      { name: 'Class enchantments', inPlay: 'Level up with spare mana, giving flooded turns something useful to do.' },
    ],
  },
  MID: {
    tagline: 'A tempo-driven graveyard format: flyers and flashback keep pressure on, Zombies swarm early, and day/night punishes the player who casts too little.',
    setting: 'Innistrad at harvest time, as the nights lengthen and werewolves grow bolder.',
    gameplay: 'Games are quick and decided by tempo and second uses. Blue-white flyers and black Zombie decks led the format, filling the graveyard with Consider and Shipwreck Sifters so disturb and flashback cards get cast twice. Day and night turn werewolves into bigger threats when a player casts no spells, so passing with mana up has a cost. Red was considered the weakest colour, and green\'s common werewolves underperformed.',
    tips: [
      'Fill your graveyard on purpose: Consider and Shipwreck Sifters turn disturb and flashback cards into two-for-ones.',
      'At night, casting two spells in a turn makes it day next turn; plan double-spell turns to shrink their werewolves.',
      'Decayed Zombies can\'t block and die after attacking; use them for early damage or sacrifice fodder, not defence.',
    ],
    tryIf: 'flyers, flashback and aggressive graveyard decks',
    power: 3, speed: 4, complexity: 4,
    themes: ['graveyard', 'tokens', 'aggro'],
    mechanics: [
      { name: 'Day/Night', inPlay: 'A turn with no spells cast makes it night and flips werewolves bigger; two spells in a turn turns it back to day.' },
      { name: 'Flashback', inPlay: 'Spells can be cast again from the graveyard, so self-mill turns into card advantage.' },
      { name: 'Disturb', inPlay: 'Creatures come back from the graveyard as a Spirit, so trading them off early is often a good deal.' },
      { name: 'Coven', inPlay: 'Rewards three creatures with different powers, pushing go-wide decks to vary their curve.' },
      { name: 'Decayed', inPlay: 'Free Zombie tokens that can\'t block and die after attacking: early damage and sacrifice fodder.' },
    ],
  },
  VOW: {
    tagline: 'A rare-driven, removal-hungry format where must-answer threats decide games more than aggression does, and Blood tokens smooth every draw.',
    setting: 'Innistrad\'s vampire wedding of Sorin and Olivia, crashed by ghosts, werewolves and worse.',
    gameplay: 'Rares and uncommons define more games than usual, and there are many must-answer threats, so decks without several removal spells struggle. Aggressive decks have a harder time than in Midnight Hunt; midrange decks built on removal and two-for-ones tend to win. Blood tokens let you discard lands or dead cards to draw, easing mana problems and adding decisions every turn. Disturb and exploit give creatures a second job.',
    tips: [
      'Take removal over most creatures; the format is full of rares and uncommons you must answer or lose to.',
      'Spend Blood tokens to pitch extra lands late and dead cards early; black-red commons like Voldaren Epicure make them freely.',
      'Training is slow and fragile; don\'t build around it without strong payoffs, and expect removal to cut your growing creature short.',
    ],
    tryIf: 'gothic vampires, removal-heavy midrange and Blood token value',
    power: 3, speed: 2, complexity: 4,
    themes: ['counters', 'sacrifice', 'tribal'],
    mechanics: [
      { name: 'Blood tokens', inPlay: 'Pay, discard and sacrifice to draw a card: built-in rummaging that eases flood and screw and feeds Blood payoffs.' },
      { name: 'Training', inPlay: 'Creatures grow when attacking beside a stronger ally, pushing attacks but rarely outpacing removal.' },
      { name: 'Exploit', inPlay: 'Sacrifice a creature on entry for a bonus; pair it with tokens or disturb creatures you\'re happy to lose.' },
      { name: 'Disturb', inPlay: 'Creatures return from the graveyard as Spirits, so trades are cheaper and graveyard hate like Diregraf Scavenger matters.' },
      { name: 'Cleave', inPlay: 'Pay more to strike the bracketed text, turning a narrow spell into a broader one later in the game.' },
    ],
  },
  NEO: {
    tagline: 'A wordy, synergy-rich format where ninjas, vehicles and artifact or enchantment themes reward a coherent deck, and thin removal lets threats stick around.',
    setting: 'Kamigawa a thousand years on: a neon city of ninja clans, mechs and kami.',
    gameplay: 'Interaction is lighter than usual, so aggressive starts with ninjutsu and vehicles can run away with games, while the best midrange decks splash bombs using common dual lands and fixing artifacts. Artifacts appear in almost every deck, and many cards care about artifacts, enchantments or modified creatures. Common removal is narrow, such as Intercessor\'s Arrest and Kami\'s Flare, so threats stick and boards grow complicated.',
    tips: [
      'Removal is scarce, so value common answers like Kami\'s Flare and Planar Incision above most creatures.',
      'Keep a cheap evasive creature for ninjutsu; once it connects, swap it for a ninja like Moon-Circuit Hacker and replay it later.',
      'Splashing is cheap: common dual lands and Network Terminal make a third-colour bomb or removal spell easy to support.',
    ],
    tryIf: 'ninjas, vehicles and artifact-enchantment synergies',
    power: 4, speed: 4, complexity: 4,
    themes: ['artifacts', 'counters', 'aggro'],
    mechanics: [
      { name: 'Ninjutsu', inPlay: 'Swap an unblocked attacker for a ninja from hand, so any open lane becomes a threat and blocking decisions get harder.' },
      { name: 'Modified', inPlay: 'Rewards creatures carrying counters, Auras or Equipment, tying creatures to your enchantments and artifacts.' },
      { name: 'Channel', inPlay: 'Discard a land-creature or creature for a spell effect, so many cards are both a threat and an instant.' },
      { name: 'Vehicles', inPlay: 'Strong bodies that need crew; count creatures that can crew before taking another one.' },
      { name: 'Reconfigure', inPlay: 'Equipment creatures that attach or walk on their own, flexible against both removal and board stalls.' },
    ],
  },
  SNC: {
    tagline: 'A tempo format where cheap creatures matter, then three-colour family decks take over. Shield counters, conniving fliers and Treasure fixing decide who wins the long game.',
    setting: 'An art-deco city of demons and angels run by five three-colour crime families.',
    gameplay: 'Games start like a normal two-colour format: cheap creatures trade early and a good two-drop pays off. Most decks then add a third colour through tri-lands, Treasures and fixing creatures, and the late game turns on card quality, such as fliers that connive or creatures with shield counters that shrug off a removal spell. Common removal is clunky and conditional rather than cheap, so a sticky threat often stays on the table.',
    tips: [
      'Start in two colours and let the draft show you which family is open; committing to three colours too early gets punished.',
      'Take fixing seriously: tri-lands and Treasure makers matter more than in a normal set, and they make your best gold cards castable.',
      'Shield counters stop the next damage or destruction, so attack or burn the shield off first, then spend your real removal.',
    ],
    tryIf: 'three-colour decks that grind out value from a good early curve',
    power: 3, speed: 3, complexity: 4,
    themes: ['multicolor', 'sacrifice', 'counters'],
    mechanics: [
      { name: 'Shield counters', inPlay: 'A shielded creature survives one removal spell or lethal hit, so you need two answers or a way to strip the counter first.' },
      { name: 'Connive', inPlay: 'Draw then discard; discarding a spell grows the creature. Smooths your draws and turns extra lands into bigger bodies.' },
      { name: 'Casualty', inPlay: 'Sacrifice a creature to copy the spell. Usually a free bonus late on, when a spent token or blitzed creature is ready to go.' },
      { name: 'Blitz', inPlay: 'Pay extra for a hasty attack that draws a card when the creature is sacrificed at end of turn. Late-game reach that never costs a card.' },
      { name: 'Alliance', inPlay: 'Triggers when another creature enters, so turns with two creatures or a token maker get far more out of these cards.' },
    ],
  },
  DMU: {
    tagline: 'A board-focused, removal-rich format where creatures block well and games become sieges. Kicker, domain and a deep stock of removal reward patient, flexible decks.',
    setting: 'Magic\'s home plane, Dominaria, packed with legends on the eve of the Phyrexian invasion.',
    gameplay: 'Creature stats favour blocking, so games are often a mutual siege rather than a race: boards build up, and removal and card advantage decide who breaks through. Common removal is plentiful, from Lightning Strike to black\'s kill spells, and kicker lets many cards act as a cheap play early or a bigger one late. Domain decks splash freely using the dual-land cycle, while blue\'s fliers and Tolarian Terror give spell decks a faster route.',
    tips: [
      'Blue and black have the deepest commons; draft removal and card advantage highly and let your fliers close it out.',
      'Count basic land types in your deck before taking domain cards; the common dual lands make a three- to five-colour pile workable.',
      'Defenders like Coral Colony are real cards here; a wall deck with the right payoff can blank an aggro opponent.',
    ],
    tryIf: 'removal-rich, patient games and splashing every colour you can',
    power: 3, speed: 1, complexity: 3,
    themes: ['multicolor', 'legends', 'spells'],
    mechanics: [
      { name: 'Kicker', inPlay: 'Most kicker cards are fine early and great late, so your hand is rarely dead and splashing a kicker cost is easy.' },
      { name: 'Domain', inPlay: 'Scales with your basic land types, rewarding greedy mana bases and drafting the dual lands over marginal playables.' },
      { name: 'Enlist', inPlay: 'Tap a creature that isn\'t attacking to add its power to an attacker, turning summoning-sick or defensive bodies into extra damage.' },
      { name: 'Read ahead', inPlay: 'Start a Saga on a later chapter, so you choose between the slow value plan or the payoff right now.' },
      { name: 'Stun counters', inPlay: 'Keep a creature tapped through its next untap; a soft removal spell that buys an attack step or two.' },
    ],
  },
  BRO: {
    tagline: 'Faster than it looks: Powerstones ramp into big artifacts, but aggressive decks with unearth and cheap creatures often win before the giant machines arrive.',
    setting: 'Urza and Mishra\'s war, fought with giant machines, Powerstones and melding war engines.',
    gameplay: 'Powerstone tokens can only pay for artifacts and abilities, yet they let a deck jump from a three-drop to a six-drop artifact a turn early. Even so, the format plays faster than its ramp suggests: red and white aggro decks curve out, unearth gives every dead attacker one more swing, and slow cards get punished. Removal like Disfigure gains value because the threats are cheap creatures, and unearth means a creature killed once may attack again.',
    tips: [
      'Prioritise proactive cheap creatures over clunky defensive cards; slow artifacts and reactive blue spells underperformed.',
      'Powerstones pay for prototype creatures and artifacts only, so count how many artifact spells you\'ll actually ramp into.',
      'Exile effects beat unearth decks; save them for creatures that would come back for another attack.',
    ],
    tryIf: 'artifacts, ramp and aggressive decks that keep coming back',
    power: 4, speed: 3, complexity: 4,
    themes: ['artifacts', 'bigThings', 'aggro'],
    mechanics: [
      { name: 'Powerstones', inPlay: 'Mana only for artifacts and abilities, so a deck full of artifacts ramps a turn ahead while others get little from them.' },
      { name: 'Unearth', inPlay: 'A dead creature returns for one hasty attack, so trades are never clean and aggro decks keep a late burst of damage.' },
      { name: 'Prototype', inPlay: 'A cheap small version early or the full-size artifact creature later, so these cards fit any curve.' },
      { name: 'Meld', inPlay: 'Rare pairs that combine into one huge threat. A build-around, not something to rely on in a normal deck.' },
    ],
  },
  J22: {
    tagline: 'Pick two themed 20-card packs, shuffle them together and play. Each theme has a clear plan, so games are honest, synergy-driven and quick to learn.',
    setting: 'No single world: 46 themes from across Magic, from Dragons and Merfolk to Cats and Zombies.',
    gameplay: 'Each pack is one colour with a single idea and its own lands, so two packs make a ready-made two-colour deck with no deckbuilding. Games play like a fair midrange format: some themes are aggressive, others make tokens or gain life, and the fun comes from how two themes meet. Removal depends on which packs you picked, so some pairings play a straight creature race while others grind with kill spells and card draw.',
    tips: [
      'Pair a creature-heavy theme with one that brings removal or card draw; two themes that both lack answers can lose to a single big threat.',
      'Look for overlaps between your two packs, such as a token theme with an anthem or a sacrifice theme with death payoffs.',
      'Learn each theme\'s plan from its first few plays; knowing whether an opponent is Speedy or Boneyard tells you whether to race or block.',
    ],
    tryIf: 'quick, clear themes and mixing them with no deckbuilding',
    power: 3, speed: 3, complexity: 1,
    themes: ['tokens', 'counters', 'lifegain'],
    mechanics: [
      { name: 'Two-pack decks', inPlay: 'Your deck is two fixed halves, so the choice is which themes to combine, not which cards to play.' },
      { name: 'Theme synergy', inPlay: 'Each pack repeats one idea, so its payoffs show up often; disrupting the key creature can stall the whole plan.' },
    ],
  },
  ONE: {
    tagline: 'One of the fastest modern formats. Toxic creatures race on poison and life together, removal is cheap, and a slow hand often dies with its best card stuck in it.',
    setting: 'New Phyrexia, the conquered remains of Mirrodin under the Machine Orthodoxy.',
    gameplay: 'Games are short and aggressive. Toxic creatures give poison with every hit, so you\'re racing two clocks at once, and life gain does nothing against them. Red is the deepest colour, with cheap removal like Hexgold Slash, and oil-counter creatures reward curving out. Corrupted cards get stronger once the opponent has three poison, so falling behind on the board snowballs.',
    tips: [
      'Play a low curve and prioritise two-drops; expensive cards often rot in hand in this format.',
      'Track both clocks: block toxic creatures early, because three poison counters switch on all of the opponent\'s corrupted cards.',
      'Value cheap removal very highly; red\'s burn and black\'s Anoint with Affliction are worth splashing.',
    ],
    tryIf: 'fast, aggressive games and racing on poison',
    power: 4, speed: 4, complexity: 3,
    themes: ['counters', 'artifacts', 'aggro'],
    mechanics: [
      { name: 'Toxic', inPlay: 'Damage also gives poison, so even small attackers threaten a win and life gain stops being a defence.' },
      { name: 'Corrupted', inPlay: 'Cards that improve once the opponent has three poison, so early toxic hits snowball into a much stronger turn later.' },
      { name: 'Oil counters', inPlay: 'Counters that fuel artifacts and creatures; the oil decks curve out fast and grow threats every turn.' },
      { name: 'Proliferate', inPlay: 'Adds one of each counter already there: poison, oil, +1/+1. At common it\'s a light bonus rather than a plan.' },
      { name: 'For Mirrodin!', inPlay: 'Equipment that makes a Rebel to carry it, so each one is a creature and a pump that keeps value if the body dies.' },
    ],
  },
  MOM: {
    tagline: 'A powerful, bomb-heavy multiverse invasion. Value-rich games where battles flip into strong cards, transforming creatures pay off later and good removal keeps boards in check.',
    setting: 'The Phyrexian invasion strikes every plane at once.',
    gameplay: 'A traditional two-colour format with plenty of power: almost every sealed pool has a bomb, and many commons are two-for-ones that transform or leave Phyrexian tokens behind. Games tend to be decided by card quality rather than speed, with blue and black value decks among the best early on, and Gruul battle decks pressing the attack. Battles reward diverting damage away from the opponent\'s face, so each attack is a real choice.',
    tips: [
      'Judge battles by how easy the defence is to beat and whether the front face already affected the board; low-defence battles are the good ones.',
      'Incubate tokens are free bodies; build around them with sacrifice outlets or Phyrexian payoffs rather than ignoring them.',
      'Convoke spells need a board of tokens or cheap creatures, so only take them if your deck goes wide.',
    ],
    tryIf: 'powerful bombs, two-for-ones and attacking battles',
    power: 4, speed: 3, complexity: 4,
    themes: ['counters', 'tokens', 'multicolor'],
    mechanics: [
      { name: 'Battles', inPlay: 'Attack a battle instead of the opponent to flip it into a strong card; the opponent blocks to protect it, splitting combat.' },
      { name: 'Incubate', inPlay: 'Makes an Incubator you can pay to turn into a Phyrexian creature, so cheap spells leave a mana sink for later turns.' },
      { name: 'Backup', inPlay: 'Puts counters on another creature that gains the backup creature\'s ability this turn, giving surprise hasty attacks.' },
      { name: 'Transform', inPlay: 'Double-faced cards with a playable front and a stronger back once a condition is met, rewarding long games.' },
      { name: 'Convoke', inPlay: 'Tap creatures to help pay, so a wide board casts big spells early and tokens double as mana.' },
    ],
  },
  MAT: {
    tagline: 'A 50-card epilogue with no commons, only gold uncommons and rare legends. It can\'t stand alone; mix it into another set for a pool with more multicolour power.',
    setting: 'The aftermath of the Phyrexian invasion, as fallen planeswalkers rebuild without their sparks.',
    gameplay: 'Aftermath has no commons and no draft structure of its own, so in this app it is only offered alongside a regular set. Its cards are almost all multicolour, many of them legendary creatures, which raises the power level of whatever it joins and pushes players towards two-colour gold pairs. The four archetypes it supports lean on legends, death triggers, recursion, and artifacts and enchantments rather than a new mechanic.',
    tips: [
      'Pair it with March of the Machine for the closest thing to the original draft, where each pack carried one Aftermath card.',
      'Its uncommons are two-colour and strong, so a gold card can point you into a colour pair early.',
      'Legend-matters cards such as Gold-Forged Thopteryx want a deck with several legendary creatures; count yours before taking them.',
    ],
    tryIf: 'adding powerful multicolour legends to another set',
    power: 4, speed: 3, complexity: 3,
    themes: ['multicolor', 'legends'],
    mechanics: [
      { name: 'Gold legends', inPlay: 'Most cards are multicolour legendary creatures, often former planeswalkers, so pools get strong two-colour threats.' },
      { name: 'Extension set', inPlay: 'No commons and only 50 cards, so it adds power to another set\'s pool instead of forming one on its own.' },
    ],
  },
  LTR: {
    tagline: 'Imbalanced but fun: Rakdos Orc armies and fast Boros humans set the pace, while the Ring tempts you into stronger legends and loots away extra lands.',
    setting: 'Middle-earth: the Fellowship\'s journey against Sauron and the armies of Mordor.',
    gameplay: 'Games are midrange to fast, with black and red the strongest colours: amass builds an Orc Army that grows with every trigger, and Boros goes wide with humans and pump. Black has the best common removal, including Murder, though much of the format\'s removal is sorcery speed. The Ring tempting you keeps turning a creature into a legendary Ring-bearer that gets harder to block and loots away excess lands, so even flooded draws stay live.',
    tips: [
      'Black and red run deep; Orc Army and sacrifice cards come at common, so prioritise them if the lane is open.',
      'Pick a Ring-bearer that\'s evasive or hard to kill; the bonuses stack on one creature, so tempting it often pays off.',
      'Green is the thinnest colour here; take it only if the strong rares or gold cards are flowing to you.',
    ],
    tryIf: 'Orc armies, legendary heroes and an aggressive black-red deck',
    power: 4, speed: 3, complexity: 3,
    themes: ['legends', 'tokens', 'sacrifice'],
    mechanics: [
      { name: 'The Ring tempts you', inPlay: 'Grows a chosen Ring-bearer with evasion and looting each time; every tempt is extra value and protects you from flood.' },
      { name: 'Amass Orcs', inPlay: 'Grows a single Orc Army token, so many small triggers stack into one big threat that removal can answer at once.' },
      { name: 'Food', inPlay: 'Tokens that gain life or fuel payoffs, giving slower decks a way to outlast aggression.' },
      { name: 'Legendary heroes', inPlay: 'Many cards are legendary, so a second copy of the same one is a dead draw; favour variety over duplicates.' },
    ],
  },
  WOE: {
    tagline: 'Fast but balanced. Red aggro and black removal set the pace, adventures give two spells per card, and bargain turns spare tokens and Food into extra power.',
    setting: 'Eldraine\'s storybook courts and the Wilds, full of fairy-tale curses and enchantments.',
    gameplay: 'Games are quick and interactive. Red has the best common removal and aggressive decks curve out, while Golgari Food decks grind them out with Food tokens fuelling bargain and life gain. Many cards are two in one: adventures cast a spell first and the creature later, and bargain lets you sacrifice an artifact, enchantment or token for a bigger effect. Role tokens and celebration reward having extra permanents, so cheap token makers punch above their size.',
    tips: [
      'Red and black are the deepest colours; cheap removal like Candy Grapple and Torch the Tower is worth taking early.',
      'Draft bargain enablers alongside the payoffs: Food, Roles and other tokens turn bargain cards from fair to strong.',
      'Blue\'s common adventures are slower than they look; make sure your blue deck still contests the board early.',
    ],
    tryIf: 'fast, interactive games and two-for-one adventures',
    power: 3, speed: 4, complexity: 4,
    themes: ['tokens', 'sacrifice', 'aggro'],
    mechanics: [
      { name: 'Adventure', inPlay: 'Cast the cheap spell side first, then the creature later from exile, so one card does two jobs and floods less.' },
      { name: 'Bargain', inPlay: 'Sacrifice an artifact, enchantment or token for a stronger spell. Food and Roles turn spare permanents into upgrades.' },
      { name: 'Roles', inPlay: 'Aura tokens that buff your creatures or curse the opponent\'s, and double as bargain fodder once their job is done.' },
      { name: 'Celebration', inPlay: 'Bonus if two nonland permanents entered this turn, rewarding token makers and two-spell turns in aggressive decks.' },
      { name: 'Food', inPlay: 'Tokens that gain life and fuel Golgari payoffs and bargain, letting midrange decks outlast the aggro ones.' },
    ],
  },
  LCI: {
    tagline: 'A tempo format that punishes falling behind. Cheap creatures, fliers and pirates set the pace, while discover and craft add value without slowing you down.',
    setting: 'The vast caverns beneath Ixalan, home to dinosaurs, pirates, merfolk and ancient gods.',
    gameplay: 'Games aren\'t the shortest, but the player who falls behind early rarely recovers, so one- and two-drops matter. White is the deepest colour, with cheap creatures and good common removal, and Azorius fliers, Izzet pirates and Gruul dinosaurs are the strongest plans. Explore and Map tokens grow creatures and smooth draws, and discover casts a free spell from the library. Descend graveyard decks underperformed: their payoffs come online slowly and die to cheap removal.',
    tips: [
      'Prioritise cheap creatures and early plays; most commons that explore or make Maps are good value.',
      'Be wary of descend: the payoffs are fragile and the enablers mediocre, so the graveyard decks are weaker than they look.',
      'Craft from your own graveyard when you can; a dead artifact or creature makes the transformed side cost almost nothing.',
    ],
    tryIf: 'tempo games, dinosaurs and value that comes without slowing you down',
    power: 3, speed: 3, complexity: 4,
    themes: ['tribal', 'flyers', 'artifacts'],
    mechanics: [
      { name: 'Explore', inPlay: 'Reveal the top card: a land goes to hand, anything else grows the creature, so you either hit a land drop or a bigger body.' },
      { name: 'Discover', inPlay: 'Cast a free spell from your library up to a mana value; a reliable two-for-one when your deck has enough cheap cards.' },
      { name: 'Craft', inPlay: 'Exile cards from your graveyard or board to transform an artifact, a late-game mana sink that uses dead cards.' },
      { name: 'Descend', inPlay: 'Rewards permanent cards in your graveyard; slow to come online and fragile once it does.' },
      { name: 'Map tokens', inPlay: 'Spend mana to have a creature explore, a small mana sink that grows threats or finds lands on quiet turns.' },
    ],
  },
  MKM: {
    tagline: 'Tempo-minded midrange where turn three is often a face-down creature. Bluffing disguises, cracking Clues for cards and white\'s strong commons decide most games.',
    setting: 'Ravnica as a murder mystery, with every guild a suspect and detectives on the case.',
    gameplay: 'Games play at a medium pace, faster in draft than sealed. The classic opening is a face-down disguise creature on turn three; ward 2 makes those hard to remove cheaply, so boards build up and blocking gets awkward as players flip creatures mid-combat. Clues are the format\'s main mana sink and keep card flow going late. White is the deepest colour, with Makeshift Binding and Novice Inspector at common, and aggressive red-white is the deck to beat.',
    tips: [
      'Draft with tempo in mind: cheap creatures and efficient removal beat slow value plans, especially in draft.',
      'Leave mana up to flip your disguise creature; the player who can unmask first usually wins the face-down fight.',
      'Pick up a few investigate cards. Clues are the set\'s mana sink, and a deck without any runs out of gas.',
    ],
    tryIf: 'face-down bluffing and tempo-driven midrange',
    power: 3, speed: 3, complexity: 4,
    themes: ['aggro', 'multicolor', 'artifacts'],
    mechanics: [
      { name: 'Disguise', inPlay: 'Face-down 2/2s with ward 2. Hard to kill early, and every flip is a combat trick, so read untapped mana before blocking.' },
      { name: 'Investigate', inPlay: 'Clue tokens turn spare mana into cards. Long games favour the player who made more of them.' },
      { name: 'Collect evidence', inPlay: 'Exile graveyard cards for a bonus. Best as a light extra in any deck; only green decks lean on it heavily.' },
      { name: 'Cloak', inPlay: 'Puts a card face down with ward 2. Watch how eagerly your opponent trades one: a land under there is just a 2/2.' },
      { name: 'Cases', inPlay: 'Enchantments that solve into a stronger effect once a condition is met. A few are excellent, most are filler.' },
    ],
  },
  OTJ: {
    tagline: 'A value-rich, interactive format where three-for-ones are common and removal is back. Bombs, solid green creatures and well-timed crimes decide games more than early rushes.',
    setting: 'A Wild West frontier of outlaw gangs, bounties and heists on the plane of Thunder Junction.',
    gameplay: 'Games run at a middling pace with plenty of time to cast spells, and low-curve aggro struggles to punish greedy decks. Removal is plentiful, black\'s especially, and many cards are two- or three-for-ones, so card advantage piles up. Green is the deepest colour and makes splashing easy; green-white Mounts and black-green value decks are strong homes. Crimes trigger off almost any targeted spell at your opponent, so they happen naturally rather than needing a build-around.',
    tips: [
      'Prioritise green when it\'s open: it\'s the deepest colour, has the best creatures and fixing, and lets you splash removal or a bomb.',
      'Value the desert lands that ping. They\'re free crimes and removal, and three or more turn on desert payoffs.',
      'Save hard removal for rares and fliers; the format is full of value creatures that will outgrind a deck that spends it early.',
    ],
    tryIf: 'value-heavy midrange, removal-rich games and splashing bombs',
    power: 3, speed: 3, complexity: 4,
    themes: ['multicolor', 'bigThings', 'tribal'],
    mechanics: [
      { name: 'Crimes', inPlay: 'Targeting an opponent or their stuff pays off. Removal and burn trigger it for free, so timing a cheap crime adds value to a turn.' },
      { name: 'Plot', inPlay: 'Exile a card now and cast it free on a later turn. Smooths mana use and enables two-spell turns.' },
      { name: 'Saddle', inPlay: 'Tap creatures to make a Mount attack with a bonus. Less all-or-nothing than crewing; green-white builds around it.' },
      { name: 'Spree', inPlay: 'Spells that add modes for extra mana. The best ones are flexible removal or card advantage you\'ll always play.' },
      { name: 'Outlaws', inPlay: 'Assassins, Mercenaries, Pirates, Rogues and Warlocks. Payoffs count them, mostly in black and red.' },
    ],
  },
  BIG: {
    tagline: 'Thirty high-impact artifacts that join a regular set\'s packs. Each one is a cheap, splashable card that bends a game around it, from mana rocks to equipment.',
    setting: 'The vault loot from Thunder Junction\'s great heist, opened alongside Outlaws of Thunder Junction.',
    gameplay: 'The Big Score isn\'t drafted on its own: it rides along with another set, adding a chance at a powerful artifact to your pool. Many of its cards are colourless, so almost any deck can play what it opens. Equipment like Lost Jitte and Sword of Wealth and Power, mana rocks like Lotus Ring, and value engines like Esoteric Duplicator and Legion Extruder tend to take over a fair board, so expect games to swing on whoever finds theirs.',
    tips: [
      'Take the colourless equipment and value engines early. They fit any deck and often outclass the host set\'s rares.',
      'Keep artifact removal in mind when building; a Lost Jitte or Sword left unanswered decides the game.',
      'Don\'t pick a narrow combo piece like Torpor Orb or Rest in Peace over a playable; most do little in a typical Limited deck.',
    ],
    tryIf: 'adding big, splashy artifacts to another set',
    power: 4, speed: 3, complexity: 2,
    themes: ['artifacts'],
    mechanics: [
      { name: 'Splashable artifacts', inPlay: 'Mostly colourless cards that slot into any deck, so a single pick can add a game-winning engine regardless of colours.' },
      { name: 'Extension sheet', inPlay: 'Mixed into another set\'s packs; it raises that format\'s power without changing its archetypes.' },
    ],
  },
  MH3: {
    tagline: 'A high-powered, synergy-dense draft where Eldrazi ramp and energy aggro clash. Games swing hard on bombs and strong commons, and fixing your mana is half the battle.',
    setting: 'A supplemental set for Modern, remixing Eldrazi, energy and Magic\'s multiverse into one pack.',
    gameplay: 'Power is high at every rarity, and games are decided by efficient threats and the removal to answer them. One pole is Eldrazi: ramp with Spawn tokens and Landscapes into huge colourless threats. The other is energy, concentrated in white, blue and red, where aggressive decks earn and spend it on creatures and scaling removal like Galvanic Discharge. Modal double-faced lands and Landscapes let decks stretch colours, so the best sealed pools splash freely.',
    tips: [
      'Take Landscapes and the modal spell-lands early; without fixing you won\'t cast the format\'s colour-hungry and colourless threats.',
      'In energy decks, aim for nearly every card to make or spend energy; half-in energy decks run dry.',
      'Respect the Eldrazi deck\'s ramp: pressure it early, and hold removal for the big devoid creatures rather than its Spawn makers.',
    ],
    tryIf: 'high power, deep synergies and splashy rares',
    power: 5, speed: 4, complexity: 5,
    themes: ['bigThings', 'artifacts', 'multicolor', 'graveyard'],
    mechanics: [
      { name: 'Energy', inPlay: 'A resource you bank and spend. Easy to get and easy to use, it powers scaling removal and aggressive creatures in Jeskai colours.' },
      { name: 'Eldrazi & devoid', inPlay: 'Spawn tokens ramp into huge colourless threats. Landscapes make the colourless costs easier than they look.' },
      { name: 'Modal lands', inPlay: 'Cards that are either a spell or a land. Cut flood and screw, and help splash, so you rarely regret playing them.' },
      { name: 'Evoke Elementals', inPlay: 'Cast cheap for the enter effect alone, or full price for a body. Flexible removal or value at every stage of the game.' },
      { name: 'Flip planeswalkers', inPlay: 'Mythic creatures that transform into planeswalkers. Bombs that must be killed before they flip.' },
    ],
  },
  BLB: {
    tagline: 'Fast, creature-first animal tribes where curving out with two-drops wins. Removal is thin, so the deck that commits to its creature type and hits its lands usually wins.',
    setting: 'A storybook plane of woodland animal folk, from Rabbits and Mice to Frogs and Bats.',
    gameplay: 'Games are quick and creature-centric, decided by who builds the better board in the first few turns. Removal is scarce and mostly in black (Savor, Nocturnal Hunger), so creatures stick and synergy snowballs. Fixing is weak, which keeps decks in two colours and makes mana stumbles a real cause of lost games. Green is the deepest colour, and green-white Rabbits and black-green Squirrels are the strongest homes; blue is shallow.',
    tips: [
      'Prioritise cheap creatures that fit your animal; a curve of on-type two- and three-drops beats a pile of good-stuff.',
      'Value Carrot Cake and other card-filtering commons: mana problems lose more games here than most sets.',
      'Take black\'s removal highly. It\'s the colour with real answers at common, and other colours have little.',
    ],
    tryIf: 'tribal decks, critters and quick games',
    power: 4, speed: 5, complexity: 3,
    themes: ['tribal', 'aggro', 'tokens'],
    mechanics: [
      { name: 'Creature types', inPlay: 'Each colour pair is one animal. Lords and payoffs reward stacking one type, so draft for your animal, not raw power.' },
      { name: 'Forage', inPlay: 'Exile three graveyard cards or sacrifice a Food for a bonus. Excellent in Squirrels; elsewhere it\'s an occasional delve.' },
      { name: 'Gift', inPlay: 'Give your opponent a small present for a bigger effect. Choosing whether to pay it is a real decision each time.' },
      { name: 'Offspring', inPlay: 'Pay extra to also make a 1/1 copy. Uneven: a few are great, many are filler.' },
      { name: 'Valiant', inPlay: 'Rewards targeting your own creature. Powerful in red-white Mice, rarely relevant elsewhere.' },
    ],
  },
  DSK: {
    tagline: 'Enchantment-fuelled midrange with lots of hidden information. Rooms, cheap enchantment removal and face-down manifests make every game a puzzle of what\'s coming next.',
    setting: 'A sentient haunted house that traps survivors inside a slasher movie.',
    gameplay: 'Pace is average but varies with the archetype: white-blue enchantments and red-white aggro are punishing, while green decks grind with manifests and delirium. Two-drops often trade off with face-down 2/2s, so games are rarely one-sided early. Removal is spread across colours and much of it is enchantment-based (Trapped in the Screen, Unable to Scream), which also fuels eerie. Rooms act as removal, value and mana sinks, and white is the deepest colour.',
    tips: [
      'Track your opponent\'s face-down cards: a manifest that has sat unflipped for turns is probably just a 2/2.',
      'Take the enchantment removal at common; it answers threats and triggers eerie in the same card.',
      'Play survival as a subtheme only. It rarely triggers often enough to build around.',
    ],
    tryIf: 'enchantment synergies and reading face-down threats',
    power: 3, speed: 3, complexity: 4,
    themes: ['graveyard', 'sacrifice'],
    mechanics: [
      { name: 'Rooms', inPlay: 'Two-door enchantments: unlock one now, the other later. They\'re removal, eerie triggers and the format\'s main mana sink.' },
      { name: 'Eerie', inPlay: 'Triggers whenever an enchantment enters or a Room door unlocks. Easy to turn on, so the payoffs are reliably strong.' },
      { name: 'Manifest dread', inPlay: 'Look at two, put one face down as a 2/2 and bin the other. Bluffs and graveyard setup in one; green-blue builds on it.' },
      { name: 'Delirium', inPlay: 'Rewards four card types in your graveyard. Happens naturally over a game in green decks that mill and sacrifice.' },
      { name: 'Impending', inPlay: 'Cast an Overlord cheaply as an enchantment that becomes a creature later. Among the set\'s strongest rares.' },
    ],
  },
  FDN: {
    tagline: 'A clean, fair core set with no new mechanics. Curve, card quality and answering big creatures win games, and white\'s fliers and removal set the pace.',
    setting: 'Magic\'s evergreen core set, built from familiar keywords and classic creature types.',
    gameplay: 'Games are straightforward creature combat with evergreen keywords. Archetypes are soft, so card quality and mana curve matter more than synergy. Removal is decent in every colour but blue (Burst Lightning, Banishing Light, Stab), and white stands out with fliers like Dazzling Angel and Vanguard Seraph at common. Many games come down to answering a big creature or a bomb, and grindy black decks win through card advantage.',
    tips: [
      'Draft card quality and a clean curve first; most synergy cards are traps even in their own archetype.',
      'Keep an answer ready for big creatures and bombs. Plenty of large threats need dealing with at uncommon and rare.',
      'Be wary of green unless Llanowar Elves and its strong uncommons are flowing: it\'s the shallowest colour.',
    ],
    tryIf: 'learning Limited or a classic core-set draft',
    power: 3, speed: 3, complexity: 1,
    themes: ['flyers', 'lifegain', 'tokens'],
    mechanics: [
      { name: 'Evergreen keywords', inPlay: 'Flying, lifelink, deathtouch and friends, with nothing new to learn. Combat math decides most games.' },
      { name: 'Light payoffs', inPlay: 'Lifegain, threshold, morbid and counters themes reward a few cards, but none demands a full build-around.' },
    ],
  },
  INR: {
    tagline: 'A dense, archetype-driven greatest-hits draft. Graveyard value, madness discard and a stacked rare sheet make games powerful and full of two-for-ones.',
    setting: 'Gothic horror Innistrad, gathered from five sets of Spirits, Zombies, Vampires and Werewolves.',
    gameplay: 'Archetypes are prominent, so finding your lane early matters more than in most sets. Flashback appears in every colour as a mana sink, so cards keep giving value from the graveyard and long games are full of two-for-ones. Black-red Vampires with madness and Blood tokens is the best-supported deck, while werewolves have historically lagged. Common removal like Bound by Moonsilver, Abrade and Murderous Compulsion is solid, and rares such as Archangel Avacyn or Olivia Voldaren can take over.',
    tips: [
      'Pick a lane early and draft for it: each colour pair has a clear plan, and the synergy cards are much worse outside it.',
      'Count flashback cards as two spells when planning removal and blockers; opponents get to use them again.',
      'Value flexible cards like Lingering Souls that fit several decks, and read late Abrades or Tragic Slips as signals.',
    ],
    tryIf: 'horror flavour and a graveyard-heavy draft',
    power: 4, speed: 3, complexity: 4,
    themes: ['graveyard', 'tribal', 'tokens'],
    mechanics: [
      { name: 'Flashback', inPlay: 'Cast spells again from the graveyard. A built-in mana sink that turns long games into card-advantage races.' },
      { name: 'Madness', inPlay: 'Cast a discarded card cheaply. With Blood tokens and other discard outlets it turns discarding into gaining ground.' },
      { name: 'Disturb', inPlay: 'Creatures that return from the graveyard as Spirits. Extra value for white-blue fliers decks.' },
      { name: 'Day/Night', inPlay: 'Night falls if a player casts no spells on their turn, flipping werewolves bigger. Two spells in a turn brings day back.' },
      { name: 'Morbid', inPlay: 'Bonus if a creature died this turn. A small theme that rewards trading or sacrificing before you cast it.' },
    ],
  },
  DFT: {
    tagline: 'A slower, board-stall-prone race where green creatures and exhaust mana sinks grind games out. Despite the racing theme, careful removal timing usually decides who wins.',
    setting: 'A death race across the planes, with souped-up vehicles, racing crews and engines.',
    gameplay: 'Despite the theme, games run long and stall often, with boards clogged by sturdy creatures and vehicles. Green is the deepest colour, and its exhaust creatures double as late-game mana sinks; black-green and green-blue midrange are the strongest decks. Removal such as Spin Out and Run Over is efficient but limited, so saving it for the threat that breaks a stall matters. Flooding is a real risk, and vehicles are weaker than they look.',
    tips: [
      'Commit to speed or cut it: max-speed payoffs need a deck full of ways to hit your opponent each turn.',
      'Cast your start-your-engines card before combat: speed only rises if you already have one out when the damage lands.',
      'Don\'t overload on vehicles. Each extra one needs crew your board can\'t spare; one or two good ones is plenty.',
    ],
    tryIf: 'grindy midrange with exhaust mana sinks and racing flavour',
    power: 3, speed: 2, complexity: 4,
    themes: ['artifacts', 'bigThings'],
    mechanics: [
      { name: 'Start your engines!', inPlay: 'Speed rises once per turn when you deal damage; max speed unlocks bonuses. Only worth it if most of your deck plays into it.' },
      { name: 'Exhaust', inPlay: 'One-shot activated abilities. Green\'s best ones are mana sinks that pull you ahead in stalled boards.' },
      { name: 'Vehicles & crew', inPlay: 'Strong bodies that need crew. Diminishing returns are steep, so a deck of them falls over.' },
      { name: 'Cycling', inPlay: 'Trade a dead card for a fresh one. Useful against flood, but doesn\'t let you cut lands.' },
    ],
  },
  TDM: {
    tagline: 'Slow, grindy three-colour and five-colour games where Dragons rule. Cheap behold removal kills early creatures, so card advantage, fixing and bombs decide games.',
    setting: 'Tarkir\'s five clans fighting alongside the returned Dragons.',
    gameplay: 'The format is sluggish: common Exhale removal kills most early creatures and the creatures favour toughness, so games run long enough to double-spell. Many players draft five-colour Dragon piles on tri-lands, dual lands and Monuments; the clans are close in strength. Bombs like Ugin and the big Dragons decide a lot of games, so expect to need answers to them and to win the long game with card advantage rather than pressure.',
    tips: [
      'Prioritise bombs, then removal, then card advantage, then fixing; curve filler comes last in this slow format.',
      'If you\'re going five-colour, take tri-lands and dual lands early and keep your early drops in two colours.',
      'Hold a Dragon or two in hand: they turn on behold removal like Caustic Exhale and Molten Exhale.',
    ],
    tryIf: 'dragons, slow grindy games and five-colour piles',
    power: 4, speed: 2, complexity: 4,
    themes: ['multicolor', 'flyers', 'bigThings'],
    mechanics: [
      { name: 'Omens', inPlay: 'Dragons with a cheap spell half that shuffles back into your library. Flexible, and several are among the best cards.' },
      { name: 'Behold', inPlay: 'Reveal or control a Dragon to empower a spell. The Exhale removal cycle makes early creatures hard to keep alive.' },
      { name: 'Harmonize', inPlay: 'Cast a spell again from the graveyard, tapping a creature to help pay. Late-game value and two-for-ones.' },
      { name: 'Mobilize', inPlay: 'Attacks bring tapped Warrior tokens that leave at end of turn. Mardu\'s aggressive engine, but many payoffs underperform.' },
      { name: 'Flurry', inPlay: 'Triggers on your second spell each turn. Easier to reach than expected in a slow format.' },
    ],
  },
  FIN: {
    tagline: 'Sturdy midrange battles where Equipment arrives already attached to a body and Summons trade blows for a few turns. Games run long, and the deep pile of uncommon bombs often decides them.',
    setting: 'Heroes, summons, jobs and towns drawn from every mainline Final Fantasy game.',
    gameplay: 'Boards fill up early: job-select Equipment and four-chapter Summons are effectively creatures, and many commons have high toughness, so cheap two-drops struggle to attack. Removal is decent, led by scaling spells like Thunder Magic and Vayne\'s Treachery, and games tend to stretch until a legendary rare or a flashback-fuelled late game breaks the stall. Red-white pump and Equipment decks are the main way to go under it.',
    tips: [
      'Count job-select Equipment as creatures: each one brings a Hero token, so White Mage\'s Staff is a two-drop body with a weapon attached.',
      'Expect long games. Value cheap two-drops less, and favour removal that scales, like Thunder Magic, plus flashback cards for the late game.',
      'Pick up Town dual lands and fixing; splashing a third colour for an uncommon or rare bomb is normal and usually worth it.',
    ],
    tryIf: 'Equipment, legendary heroes and long, value-driven games',
    power: 4, speed: 3, complexity: 3,
    themes: ['artifacts', 'legends', 'multicolor'],
    mechanics: [
      { name: 'Job select', inPlay: 'Equipment that enters attached to a fresh 1/1 Hero, so it is a creature first; once the Hero dies the gear is still there to re-equip.' },
      { name: 'Summons', inPlay: 'Saga creatures that do something each chapter and then leave. Three-chapter ones are spells with a body; four-chapter ones play as real creatures.' },
      { name: 'Equipment', inPlay: 'Everywhere, and it rewards going tall: suit up one evasive or double-striking creature and make every block a bad one.' },
      { name: 'Flashback', inPlay: 'Spells come back for a second use from the graveyard, so long games reward the player who has kept a few cards in reserve.' },
      { name: 'Towns', inPlay: 'Nonbasic lands with value abilities that also fix colours. They make three-colour decks easy and feed land-count payoffs.' },
    ],
  },
  EOE: {
    tagline: 'A slow format of board stalls broken by fliers, huge Spacecraft and late-game bombs. Plentiful removal and Lander ramp mean the player with more card advantage usually wins.',
    setting: 'Science-fiction Magic among stars, planets and the void between them.',
    gameplay: 'Few early creatures hit hard, so boards stall and games go long; reaching seven mana for something like Pinnacle Kill-Ship is realistic. Common removal is abundant and good (Gravkill, Orbital Plunge, Banishing Light, Plasma Bolt), which makes evasive threats, card advantage and big Spacecraft the way through. Lander tokens fix and ramp, so three-colour decks splashing bombs are common.',
    tips: [
      'Play no more than two or three Spacecraft: they need creatures to station them and sit uselessly in hand without a board.',
      'Value Lander makers highly. They ramp, fix a splash and feed landfall or void, and the slow pace gives them time to pay off.',
      'Warp a creature early only when its enter effect matters; it leaves at end of turn and can\'t block until you recast it.',
    ],
    tryIf: 'slow games, ramp and big spaceships',
    power: 3, speed: 2, complexity: 4,
    themes: ['artifacts', 'bigThings', 'flyers'],
    mechanics: [
      { name: 'Station', inPlay: 'Tap creatures instead of attacking to charge a Spacecraft until it becomes a large threat. Rewards stalled boards and wide teams.' },
      { name: 'Landers', inPlay: 'Artifact tokens you sacrifice for a basic land. Ramp, fixing and sacrifice fodder in one, so splashing is cheap.' },
      { name: 'Warp', inPlay: 'Cast a permanent cheaply for a turn to grab its enter trigger, then recast it later. Turns one card into two plays.' },
      { name: 'Void', inPlay: 'Bonuses if a nonland permanent left play or you warped this turn. Removal, trades and sacrifice all switch it on.' },
      { name: 'Spacecraft', inPlay: 'Artifacts that become creatures at a station threshold, often with flying. Hard to answer once charged, so save removal.' },
    ],
  },
  OM1: {
    tagline: 'The Spider-Man card pool under Magic\'s own names and art. Small and simple: cheap Heroes race, Villain decks grind with connive and mayhem, and the black and blue rares decide many games.',
    setting: 'Through the Omenpaths retells the Spider-Man set with characters from Magic\'s own planes.',
    gameplay: 'A small format with only five two-colour archetypes, so colours are tightly shared and fixing comes easily. White decks attack early with cheap Heroes and web-slinging, while blue and black villain decks grind with connive and mayhem card advantage. Common removal exists (Web Up, Scorpion\'s Sting, Electro\'s Bolt), but games often turn on the strong black and blue rares, and red is widely considered the weakest colour.',
    tips: [
      'Treat red as a support colour at best; reviewers found its archetypes too slow or too costly, so lean on white, blue and black.',
      'Web-slinging needs a tapped creature to return, so fliers and creatures with enter triggers like City Pigeon make the best partners.',
      'Pair mayhem cards with free discard outlets: discarding one costs you nothing when you can cast it from the graveyard that turn.',
    ],
    tryIf: 'the Spider-Man draft without leaving the Multiverse',
    power: 3, speed: 4, complexity: 2,
    themes: ['legends', 'aggro'],
    mechanics: [
      { name: 'Web-slinging', inPlay: 'Cast a Hero cheaply by returning a tapped creature you control, ideally after it attacked, and replay its enter effect later.' },
      { name: 'Mayhem', inPlay: 'Cast a card from your graveyard the turn you discarded it, so looting and discard costs become free card advantage.' },
      { name: 'Connive', inPlay: 'Draw then discard, growing the creature if you pitch a nonland card. Smooths draws and feeds mayhem and discard payoffs.' },
      { name: 'Villains & Heroes', inPlay: 'Creature-type payoffs split the colours: Heroes reward suiting up and attacking, Villains reward each other entering.' },
    ],
  },
  SPM: {
    tagline: 'A small, simple format with five archetypes. Cheap Heroes race while Villain decks grind with connive and mayhem, and the strong black and blue rares decide many games.',
    setting: 'Marvel\'s Spider-Man: New York\'s web-slinging heroes against their rogues\' gallery.',
    gameplay: 'A small format with only five two-colour archetypes, so colours are tightly shared and fixing comes easily. White decks attack early with cheap Heroes and web-slinging, while blue and black villain decks grind with connive and mayhem card advantage. Common removal exists (Web Up, Scorpion\'s Sting, Electro\'s Bolt), but games often turn on the strong black and blue rares, and red is widely considered the weakest colour.',
    tips: [
      'Treat red as a support colour at best; reviewers found its archetypes too slow or too costly, so lean on white, blue and black.',
      'Web-slinging needs a tapped creature to return, so fliers and creatures with enter triggers like City Pigeon make the best partners.',
      'Pair mayhem cards with free discard outlets: discarding one costs you nothing when you can cast it from the graveyard that turn.',
    ],
    tryIf: 'comic-book flavour and a small, quick-to-learn draft',
    power: 3, speed: 4, complexity: 2,
    themes: ['legends', 'aggro'],
    mechanics: [
      { name: 'Web-slinging', inPlay: 'Cast a Hero cheaply by returning a tapped creature you control, ideally after it attacked, and replay its enter effect later.' },
      { name: 'Mayhem', inPlay: 'Cast a card from your graveyard the turn you discarded it, so looting and discard costs become free card advantage.' },
      { name: 'Connive', inPlay: 'Draw then discard, growing the creature if you pitch a nonland card. Smooths draws and feeds mayhem and discard payoffs.' },
      { name: 'Villains & Heroes', inPlay: 'Creature-type payoffs split the colours: Heroes reward suiting up and attacking, Villains reward each other entering.' },
    ],
  },
  TLA: {
    tagline: 'A bomb-heavy format where one rare can end a game on its own. Bending tricks every turn, plentiful removal, and boards that stall until a bomb or an earthbent land breaks through.',
    setting: 'Avatar: The Last Airbender, with Aang and friends travelling the four nations.',
    gameplay: 'White Ally decks and red firebenders can start fast, but boards often stall and many games come down to resolving and answering rares. Removal is plentiful, with Firebending Lesson, Lightning Strike, Sold Out and Watery Grasp at common, yet the best bombs can leave you behind even after you kill them. Waterbending blockers, Clues and hasty earthbent lands are how the commons win a stalled board.',
    tips: [
      'Take removal and bombs over synergy: the gap between the many busted rares and the modest commons is the format\'s defining feature.',
      'Airbending is flexible tempo: bounce a blocker, save your creature from Watery Grasp, or reset your own enter effects.',
      'Earthbent lands are creatures, so bounce and removal can hit them; reuse the same land rather than risk a fresh one.',
    ],
    tryIf: 'bombs, flexible tricks and the show\'s four bending styles',
    power: 4, speed: 3, complexity: 3,
    themes: ['spells', 'tribal', 'bigThings'],
    mechanics: [
      { name: 'Firebending', inPlay: 'Attacking gives you combat-only mana, so aggressive decks want instants, Clues or activated abilities to spend it on.' },
      { name: 'Earthbend', inPlay: 'Turn a land into a hasty X/X creature that keeps tapping for mana. Strong late, but it exposes your lands to removal.' },
      { name: 'Waterbend', inPlay: 'Tap your artifacts and creatures to help pay costs. Clues and Ally tokens pull double duty, enabling flash blockers.' },
      { name: 'Lessons', inPlay: 'An instant/sorcery subtype that payoffs count and some cards fetch. Blue has the deepest lesson core.' },
      { name: 'Allies', inPlay: 'Most white commons are Allies, so creature-count payoffs come together easily in base-white aggro decks.' },
    ],
  },
  ECL: {
    tagline: 'A creature-type draft where committing to a tribe pays off. Flash creatures, convoke and blight make combat tricky, and the deep, synergy-rich decks tend to win longer games.',
    setting: 'Lorwyn\'s fairy-tale tribes of Kithkin, Merfolk, Elves, Goblins and Elementals return under a darker sky.',
    gameplay: 'Games are creature-heavy and reward a critical mass of one creature type, so drafting is about finding the deep tribe for your seat more than an open colour. Flash creatures and convoke turn combat into a guessing game, and -1/-1 counters from blight and removal like Blight Rot shrink boards permanently. Removal is decent but black is thinner than usual, and reviewers found the pace slower than expected, favouring deep decks like Elves.',
    tips: [
      'Count creature types as you draft: payoffs like behold and the champion cycle need a real density of your tribe to work.',
      'Blight is a cost; only pay it on creatures that shrug off counters, like high-toughness bodies, or when the payoff wins the game.',
      'Hold blocks until flash creatures are accounted for, and keep removal for the creature that sneaks in after blockers.',
    ],
    tryIf: 'tribal synergies, flash creatures and tricky combat',
    power: 4, speed: 3, complexity: 4,
    themes: ['tribal', 'counters', 'multicolor'],
    mechanics: [
      { name: 'Blight', inPlay: 'Put -1/-1 counters on your own creature to pay for a stronger effect. Great on 0/5s, painful on your best attacker.' },
      { name: 'Changeling', inPlay: 'Counts as every creature type, so it fits any tribal deck and turns on several tribe payoffs at once.' },
      { name: 'Behold', inPlay: 'Reveal a creature of the named type from hand or play as a cost. Rewards decks that stay deep in one tribe.' },
      { name: 'Convoke', inPlay: 'Tap creatures to help pay, so a wide board casts big spells early or at instant speed mid-combat.' },
      { name: 'Vivid', inPlay: 'Bonuses scale with the colours among your permanents, rewarding multicolour decks that splash freely.' },
    ],
  },
  TMT: {
    tagline: 'A small, simple five-archetype draft built around sneak: swap an unblocked attacker for a ninja mid-combat. Games run slower than you\'d expect until a sneak turn swings the race.',
    setting: 'Teenage Mutant Ninja Turtles, with the four brothers, Splinter and the Foot Clan.',
    gameplay: 'Boards develop at a fairly measured pace, with red-white alliance the one deck that wants to race from turn two. The swings come from sneak: cheap evasive creatures get through, then return to hand so a ninja or spell lands for less, so every unblocked attacker is a threat. Black has the best common removal, including Anchovy & Banana Pizza, while green has the biggest creatures but little interaction.',
    tips: [
      'Draft cheap evasive creatures and reusable enter effects: sneak enablers are scarcer than sneak cards, so they\'re worth taking early.',
      'Block more than usual against white-black. Every unblocked attacker can become a sneak cast, so chump or trade to deny the swap.',
      'Remember sneaking casts the spell: counterspells can stop it, and \'whenever you cast\' triggers fire when you sneak.',
    ],
    tryIf: 'ninjutsu-style combat tricks and a quick-to-learn draft',
    power: 3, speed: 3, complexity: 2,
    themes: ['aggro', 'tokens', 'counters'],
    mechanics: [
      { name: 'Sneak', inPlay: 'Return an unblocked attacker to cast a ninja or spell cheaply mid-combat. Rewards evasive one- and two-drops and punishes loose blocks.' },
      { name: 'Alliance', inPlay: 'Triggers when another creature enters under your control, so go-wide decks and token makers snowball damage.' },
      { name: 'Mutagen tokens', inPlay: 'Artifacts you cash in for a +1/+1 counter, letting you grow a creature at the right moment or feed artifact payoffs.' },
      { name: 'Disappear', inPlay: 'Bonuses if a permanent you controlled left play this turn. Eating Food or sacrificing turns it on.' },
      { name: 'Classes', inPlay: 'Enchantments you level up at sorcery speed, turning spare late-game mana into a growing engine.' },
    ],
  },
  SOS: {
    tagline: 'A spells-matter format with small boards: aggressive Silverquill and Lorehold decks race slower multicolour converge piles. Removal is good and the rares are very strong.',
    setting: 'A return to the Strixhaven mage university, each college casting spells its own way.',
    gameplay: 'Few creatures stay alive for long, because removal and creatures with spells attached keep trading off the board. White-based aggro decks can end games quickly with cheap removal and tricks that trigger repartee, while green-based converge decks stumble early and then take over with five-colour payoffs. You are largely confined to the five college colour pairs plus a splash, and powerful rares often decide the game once they land.',
    tips: [
      'Prepared creatures are two-for-ones, a body plus a copy of a spell, so value them like removal or card advantage, not vanilla bodies.',
      'Decide early whether you\'re the fast deck or the converge deck: the middle ground gets run over or out-valued.',
      'Lean towards 18 lands in converge and opus decks; they need to hit land drops and colours for their big turns.',
    ],
    tryIf: 'instants and sorceries, and choosing between racing and going big',
    power: 4, speed: 3, complexity: 4,
    themes: ['spells', 'multicolor', 'lifegain'],
    mechanics: [
      { name: 'Prepared', inPlay: 'Creatures that enter with a spell you can cast a copy of later. Each one is a body plus a spell, so trades rarely lose card advantage.' },
      { name: 'Repartee', inPlay: 'Silverquill bonuses when you target a creature with an instant or sorcery. Cheap removal and tricks trigger it easily.' },
      { name: 'Converge', inPlay: 'Spells that scale with colours spent, the engine of slow four- and five-colour decks built on fixing.' },
      { name: 'Opus', inPlay: 'Prismari rewards for instants and sorceries, with a bigger bonus for five-mana spells. Builds towards one explosive turn.' },
      { name: 'Increment & Infusion', inPlay: 'Quandrix creatures grow when you spend lots of mana on a spell; Witherbloom spells get stronger if you gained life this turn.' },
    ],
  },
  MSH: {
    tagline: 'A creature-heavy superhero format where ground stalls are common and fliers break them. Teamwork and power-up turn spare creatures and mana into swingy plays, and Heroes outclass Villains.',
    setting: 'Marvel Super Heroes: the Avengers, X-Men and friends against their greatest villains.',
    gameplay: 'Boards fill with creatures and the ground often locks up, so fliers and well-timed races decide many games and reading combat math matters. Power-up gives every creature a late-game mana sink, and teamwork spells get stronger by tapping your own creatures, which trades attacking for value. White and blue have the deepest commons; black has good removal like Cruel Alliance but weak creatures, and red was widely judged the weakest colour.',
    tips: [
      'Prioritise white and blue commons like Hero in Training; their depth makes those colours the most consistent drafts.',
      'Value fliers highly: ground boards stall and evasive Heroes are how most games end.',
      'Teamwork 1–2 is nearly free, but teamwork 4 means skipping attacks; count your untapped creatures before you rely on a big one.',
    ],
    tryIf: 'comic-book flavour, fliers and combat-math-heavy games',
    power: 4, speed: 3, complexity: 3,
    themes: ['flyers', 'legends', 'counters'],
    mechanics: [
      { name: 'Power-up', inPlay: 'A one-shot activated boost, cheaper the turn the creature arrives. Makes cheap creatures good late and keeps flooding from hurting.' },
      { name: 'Teamwork', inPlay: 'Tap your creatures as an extra cost to supercharge a spell, trading attacks or blocks for a stronger effect.' },
      { name: 'Plans', inPlay: 'Enchantments that gain counters as you meet a condition, then pay off. Some, like Political Triumph, overperform; others never fire.' },
      { name: 'Connive', inPlay: 'Draw then discard, growing the creature if you pitch a nonland. Smooths draws, but there\'s little graveyard payoff.' },
      { name: 'Heroes & Villains', inPlay: 'Creature-type payoffs; Hero commons are notably stronger, so Villain-themed decks need their rares.' },
    ],
  },
  HOB: {
    tagline: 'A small, fast five-archetype draft where black is the strongest colour and being on the play matters. Goblin armies, Dwarven legends and Adventure two-for-ones fight over a crowded board.',
    setting: 'The Hobbit: Bilbo, Thorin\'s company and Gandalf on the road to Smaug\'s Lonely Mountain.',
    gameplay: 'Games are quick and tempo-driven, and going first is a real edge, so cheap creatures and efficient removal matter more than late-game value. Black has the best removal (Bilbo\'s Deadly Slice, Gnashing of Teeth) and creature quality, which is why black-green and black-red were the strongest decks. The mechanics of each archetype barely overlap, so drafts run on rails: pick a lane early and stay in it.',
    tips: [
      'Take black early; its removal and creatures are the best in the set, and both black pairs outperformed the rest.',
      'Pick your archetype early and commit: amass, ferocious, landfall and storied rarely support each other, so off-plan cards underperform.',
      'Curve out and choose to play first; the format is fast enough that tempo beats card advantage in most games.',
    ],
    tryIf: 'Tolkien, fast tempo games and tightly defined archetypes',
    power: 3, speed: 4, complexity: 2,
    themes: ['tokens', 'tribal', 'legends'],
    mechanics: [
      { name: 'Amass Goblins', inPlay: 'Build up one Goblin Army token with +1/+1 counters. Grow a single threat, or sacrifice it for value in black-red.' },
      { name: 'Adventures', inPlay: 'Cast the spell half first, then the creature from exile later. Easy two-for-ones that make any deck grind better.' },
      { name: 'Ferocious', inPlay: 'Bonuses while you control a creature with power 4 or more, rewarding black-green decks that land one big body early.' },
      { name: 'Storied', inPlay: 'Once you control three artifacts, legends and/or Sagas, Dwarves gain a permanent bonus. Equipment and legends make it easy.' },
      { name: 'Recruit', inPlay: 'Draw then discard; pitching a nonland gives you a 1/1 token. Smooths draws while adding to the board.' },
    ],
  },
  FRA: {
    tagline: 'One of the slowest recent formats: long games where Jace tokens soak up attacks and value piles up. Fixing is plentiful, splashing is easy, and expensive cards are fully playable.',
    setting: 'Reality Fracture: Jace\'s Echoverse collides with the Multiverse around a school for planeswalkers.',
    gameplay: 'Games regularly run past turn ten. Empower Jace puts a planeswalker token into almost every game, and each one forces the choice between attacking it and attacking the player, which slows aggressive decks down. Common removal is good in white and black (Memory Trap, Extended Absence, Last Gasp) but blue has none, and fixing from the dual lands and Heartwood tokens makes splashing a third colour easy and rewarding.',
    tips: [
      'Don\'t overprotect Jace tokens: losing one usually costs about a card. Use the −3 to draw when in doubt and the −1 only when racing.',
      'Aggro must decide when to hit Jace: attack it early, but ignore it once you can push lethal damage at the player.',
      'Draft fixing and splash bombs freely; the format is slow enough that five- and six-drops are real cards.',
    ],
    tryIf: 'slow, grindy games, planeswalkers and splashing bombs',
    power: 3, speed: 2, complexity: 4,
    themes: ['spells', 'tokens', 'multicolor'],
    mechanics: [
      { name: 'Empower Jace', inPlay: 'Adds loyalty to your Jace token, creating one if needed. It draws or surveils for you and absorbs attacks, slowing every game.' },
      { name: 'Prepared', inPlay: 'Creatures that enter with a spell you can cast a copy of later, so each one is a body plus a spell\'s worth of value.' },
      { name: 'Surveil', inPlay: 'Constant card selection that blue-white turns into growth and payoffs, and that fuels flashback in graveyard decks.' },
      { name: 'Echoed pairs', inPlay: 'Each pack has a Multiverse character and its Echoverse twin; the two often belong to different decks.' },
      { name: 'Flashback', inPlay: 'Spells recast from the graveyard; in a format this slow, cards like Bestial Incursion often get both halves.' },
    ],
  },
}

export function setProfile(code: string): SetProfile | undefined {
  return SET_PROFILES[code.toUpperCase()]
}
