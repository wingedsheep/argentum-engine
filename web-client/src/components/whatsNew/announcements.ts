/**
 * The landing screen's "What's new" feed — hand-curated, newest first.
 *
 * Only milestones a player would notice belong here: a set becoming fully playable, a new way to
 * play, or a feature big enough to change how someone uses the site. A batch of cards, a bug fix or
 * a UI tweak does not. When in doubt, leave it out — the feed is only worth reading while it stays
 * short.
 *
 * A set entry is also a pitch: `body` says what the set is like to play (its world, its mechanics,
 * how its decks feel) and `tryIf` names the kind of player who will enjoy it, so someone can decide
 * from the feed alone whether it's worth a draft.
 *
 * `id` is what a viewer's read state is keyed on, so never change one after it ships; give a new
 * entry a new id instead. `date` is the day the work landed on `main` (`YYYY-MM-DD`).
 */

export type AnnouncementKind = 'set' | 'mode' | 'feature'

export interface Announcement {
  id: string
  date: string
  kind: AnnouncementKind
  title: string
  body: string
  /** Set code for `kind: 'set'`, rendered as the set's Keyrune symbol. */
  setCode?: string
  /** Who will enjoy it, completing "Try it if you like …". Every set entry has one. */
  tryIf?: string
}

export const ANNOUNCEMENTS: readonly Announcement[] = [
  {
    id: 'feature-avatars',
    date: '2026-10-09',
    kind: 'feature',
    title: 'Avatars',
    body:
      'Give your account a face: choose from 56 painted portraits, or frame any part of any card’s art — any printing — as your own. Click your avatar on your profile to choose; it shows in the account menu, to friends and in messages, and in your life orb at the table.',
  },
  {
    id: 'feature-preferences',
    date: '2026-10-09',
    kind: 'feature',
    title: 'Preferences',
    body:
      'Set the table up your way from the gear on the home screen, or mid-game beside the fullscreen button: the priority mode games start in, standing stops for both turns, when identical lands, creatures and other permanents stack and how big a stack gets, auto-tap, reduced motion and the hover preview. Signed in, they follow you to every device.',
  },
  {
    id: 'feature-messages',
    date: '2026-10-09',
    kind: 'feature',
    title: 'Messages',
    body:
      'Message other players from Messages in the account menu, or from a friend’s row or any player’s profile. Friends can talk freely; a message from someone you haven’t friended arrives as a request you can accept, delete or block.',
  },
  {
    id: 'mode-matchmaking',
    date: '2026-10-08',
    kind: 'mode',
    title: 'Matchmaking',
    body:
      'Find an opponent without an invite. Pick a mode on the home screen — Random deck, Jump In, Momir Basic or a Constructed format — then search; ranked pairs you with someone close to your rating. Queues with players already waiting are shown, so you can join one and start at once.',
  },
  {
    id: 'set-leb',
    date: '2026-10-08',
    kind: 'set',
    setCode: 'LEB',
    title: 'Limited Edition Beta is complete',
    body:
      'The second printing of Magic’s first set, two months after Alpha: the same Power Nine and dual lands, plus the two cards Alpha left out, Circle of Protection: Black and Volcanic Island.',
    tryIf: 'Magic history and the most iconic cards ever printed',
  },
  {
    id: 'set-lea',
    date: '2026-10-07',
    kind: 'set',
    setCode: 'LEA',
    title: 'Limited Edition Alpha is complete',
    body:
      'Where Magic began, in 1993: Black Lotus, the original dual lands, Shivan Dragon, Lord of the Pit and Wrath of God. Five colours of raw, unbalanced power from before anyone knew what a fair card looked like.',
    tryIf: 'Magic history and the most iconic cards ever printed',
  },
  {
    id: 'set-j22',
    date: '2026-10-07',
    kind: 'set',
    setCode: 'J22',
    title: 'Jumpstart 2022 is complete',
    body:
      'Mix and match themes from across Magic: Goblins, snow creatures, blink tricks, giant monsters and more. Combine two themed packs for a ready-to-play deck, or explore the full set in Limited.',
    tryIf: 'surprising theme combinations and quick games without deckbuilding',
  },
  {
    id: 'mode-jump-in',
    date: '2026-10-07',
    kind: 'mode',
    title: 'Jump In / Jumpstart',
    body: 'Choose two themed 20-card packs and play their combined deck, lands included. No deckbuilding needed. Play with friends or AI, starting with Jumpstart 2022 or the original Jumpstart set. Find Jump In under Play → What are you playing with?',
  },
  {
    id: 'set-om1',
    date: '2026-10-05',
    kind: 'set',
    setCode: 'OM1',
    title: 'Through the Omenpaths is complete',
    body:
      'Marvel’s Spider-Man retold on Magic’s own planes: the same cards and draft as Spider-Man, with new names and art for every hero and villain. Pick its printings in the deckbuilder or draft it as its own set.',
    tryIf: 'the Spider-Man draft without leaving the Multiverse',
  },
  {
    id: 'set-mh3',
    date: '2026-10-04',
    kind: 'set',
    setCode: 'MH3',
    title: 'Modern Horizons 3 is complete',
    body:
      'A high-powered set built for Modern: the Eldrazi return, energy is back, and five legendary creatures flip into planeswalkers. Draft is fast, swingy and full of bombs.',
    tryIf: 'big power, splashy rares and deep synergies',
  },
  {
    id: 'feature-replay-files',
    date: '2026-10-02',
    kind: 'feature',
    title: 'Replay files',
    body: 'Export any game as a compact replay file, and upload one to watch it back.',
  },
  {
    id: 'set-one',
    date: '2026-10-02',
    kind: 'set',
    setCode: 'ONE',
    title: 'Phyrexia: All Will Be One is complete',
    body:
      'The Phyrexians’ metal home plane. Toxic creatures poison opponents toward ten counters, oil counters fuel Phyrexian machines, and corrupted cards turn deadly once a foe has three poison.',
    tryIf: 'an alternate way to win and aggressive poison decks',
  },
  {
    id: 'set-s99',
    date: '2026-09-30',
    kind: 'set',
    setCode: 'S99',
    title: 'Starter 1999 is complete',
    body:
      'A 1999 set made to teach the game: honest creatures, simple removal and card draw, and no complicated rules.',
    tryIf: 'learning the basics, or a relaxed old-school game',
  },
  {
    id: 'set-ptk',
    date: '2026-09-29',
    kind: 'set',
    setCode: 'PTK',
    title: 'Portal Three Kingdoms is complete',
    body:
      'Portal’s retelling of China’s Three Kingdoms era. Beginner-friendly cards with almost no instants, and horsemanship: evasion only other horsemen can block.',
    tryIf: 'plain creature combat at an old-school pace',
  },
  {
    id: 'set-mom',
    date: '2026-09-29',
    kind: 'set',
    setCode: 'MOM',
    title: 'March of the Machine is complete',
    body:
      'The Phyrexian invasion of the Multiverse. Battles are a new card type you attack to flip, incubate builds Phyrexian armies, and backup passes abilities across your team.',
    tryIf: 'big multiverse battles and transforming cards',
  },
  {
    id: 'set-p02',
    date: '2026-09-28',
    kind: 'set',
    setCode: 'P02',
    title: 'Portal Second Age is complete',
    body:
      'The 1998 follow-up to Portal: beginner-friendly cards, sorcery-speed tricks and straightforward creature battles.',
    tryIf: 'simple, readable games at a slower pace',
  },
  {
    id: 'set-chk',
    date: '2026-09-27',
    kind: 'set',
    setCode: 'CHK',
    title: 'Champions of Kamigawa is complete',
    body:
      'Mortals at war with the kami of a feudal-Japan-inspired world. Spirits and Arcane spells feed splice and soulshift, Samurai fight with bushido, and legendary heroes flip into stronger forms.',
    tryIf: 'Spirits, legends and Japanese-inspired flavour',
  },
  {
    id: 'set-fra',
    date: '2026-09-24',
    kind: 'set',
    setCode: 'FRA',
    title: 'Reality Fracture is complete',
    body:
      'Planeswalkers at an academy. Scry and surveil to empower your own Jace token, swarm with 2/2 Cadets, and prepare permanents that let you cast a copy of their spell later. Ten two-colour archetypes, from surveil tempo to graveyard control.',
    tryIf: 'card selection, planeswalkers and lots of archetypes to explore',
  },
  {
    id: 'set-lrw',
    date: '2026-09-24',
    kind: 'set',
    setCode: 'LRW',
    title: 'Lorwyn is complete',
    body:
      'A sunlit fairy-tale world where every tribe has its own deck: Kithkin, Merfolk, Faeries, Elves, Goblins, Giants, Elementals and Treefolk. Clash, champion and evoke reward knowing your creature types.',
    tryIf: 'tribal synergies and creature-type deckbuilding',
  },
  {
    id: 'set-rav',
    date: '2026-09-24',
    kind: 'set',
    setCode: 'RAV',
    title: 'Ravnica: City of Guilds is complete',
    body:
      'The city-plane of guilds, where each two-colour guild plays its own way: Boros radiance, Selesnya convoke, Golgari dredge and Dimir transmute. A deep, beloved draft format.',
    tryIf: 'multicolour decks and strong guild identities',
  },
  {
    id: 'set-vow',
    date: '2026-09-01',
    kind: 'set',
    setCode: 'VOW',
    title: 'Innistrad: Crimson Vow is complete',
    body:
      'A vampire wedding on gothic-horror Innistrad. Werewolves flip at nightfall, Blood tokens fuel looting, and training and exploit reward smart attacks and sacrifices.',
    tryIf: 'day/night swings, spooky flavour and grindy combat',
  },
  {
    id: 'feature-learn-to-play',
    date: '2026-08-28',
    kind: 'feature',
    title: 'Learn to Play',
    body: 'Five coached games against the AI that teach Magic and the table from scratch.',
  },
  {
    id: 'set-arn',
    date: '2026-08-24',
    kind: 'set',
    setCode: 'ARN',
    title: 'Arabian Nights is complete',
    body:
      'Magic’s first expansion, from 1993, drawn from the Thousand and One Nights: djinn, efreet, flying carpets and the original City of Brass.',
    tryIf: 'Magic history and classic old-school cards',
  },
  {
    id: 'set-ddq',
    date: '2026-08-24',
    kind: 'set',
    setCode: 'DDQ',
    title: 'Duel Decks: Blessed vs. Cursed is complete',
    body:
      'Two ready-made Innistrad decks built to face each other: Humans and holy Spirits against Zombies and the curses of the night.',
    tryIf: 'an evenly matched duel with no deckbuilding',
  },
  {
    id: 'set-fem',
    date: '2026-08-23',
    kind: 'set',
    setCode: 'FEM',
    title: 'Fallen Empires is complete',
    body:
      'A 1994 tale of dwindling empires: Thallids grow spore counters into Saprolings, Thrulls are sacrificed for value, and Homarids and Orcs fight over the scraps. Low power, high charm.',
    tryIf: 'Magic history and tokens done the old way',
  },
  {
    id: 'set-drk',
    date: '2026-08-23',
    kind: 'set',
    setCode: 'DRK',
    title: 'The Dark is complete',
    body:
      'Magic’s darkest early expansion: a low-power, flavour-first set of cults, curses and strange artifacts from 1994.',
    tryIf: 'Magic history and odd, characterful cards',
  },
  {
    id: 'set-mkm',
    date: '2026-08-21',
    kind: 'set',
    setCode: 'MKM',
    title: 'Murders at Karlov Manor is complete',
    body:
      'A murder mystery on Ravnica. Detectives collect evidence from the graveyard, suspects get menace and can’t block, and disguised creatures hide face down until revealed.',
    tryIf: 'detective flavour, face-down bluffing and graveyard play',
  },
  {
    id: 'set-mrd',
    date: '2026-08-21',
    kind: 'set',
    setCode: 'MRD',
    title: 'Mirrodin is complete',
    body:
      'A plane made of metal, where artifacts are everything: affinity makes spells nearly free, Equipment suits up your team, and Myr ramp you into robots.',
    tryIf: 'artifacts and fast, explosive starts',
  },
  {
    id: 'set-msh',
    date: '2026-08-17',
    kind: 'set',
    setCode: 'MSH',
    title: 'Marvel Super Heroes is complete',
    body:
      'Marvel’s heroes and villains assemble. Power-up gives each hero a one-time boost that’s cheaper the turn it arrives, and teamwork lets you tap your creatures to supercharge a spell.',
    tryIf: 'comic-book flavour and building a team',
  },
  {
    id: 'set-hob',
    date: '2026-08-15',
    kind: 'set',
    setCode: 'HOB',
    title: 'The Hobbit is complete',
    body:
      'Bilbo’s journey to the Lonely Mountain: amass Goblin armies, recruit your company, and hoard Treasure on the way to the dragon.',
    tryIf: 'Tolkien, armies of tokens and treasure-fuelled ramp',
  },
  {
    id: 'set-inr',
    date: '2026-08-08',
    kind: 'set',
    setCode: 'INR',
    title: 'Innistrad Remastered is complete',
    body:
      'A greatest-hits tour of gothic Innistrad: Spirits, Zombies, Vampires and Werewolves, with madness, morbid and self-mill all in one draft.',
    tryIf: 'horror flavour and a graveyard-heavy draft',
  },
  {
    id: 'set-spm',
    date: '2026-08-04',
    kind: 'set',
    setCode: 'SPM',
    title: 'Marvel’s Spider-Man is complete',
    body:
      'Marvel’s Spider-Man: heroes and villains of New York trade blows, with web-slinging and plenty of rogues’-gallery legends.',
    tryIf: 'comic-book flavour and legendary characters',
  },
  {
    id: 'set-dft',
    date: '2026-08-03',
    kind: 'set',
    setCode: 'DFT',
    title: 'Aetherdrift is complete',
    body:
      'A death race across the Multiverse. Start your engines and build speed toward max-speed bonuses, crew Vehicles and saddle Mounts, and cash in exhaust abilities for one big burst.',
    tryIf: 'fast, aggressive decks and Vehicles',
  },
  {
    id: 'set-woe',
    date: '2026-08-02',
    kind: 'set',
    setCode: 'WOE',
    title: 'Wilds of Eldraine is complete',
    body:
      'A fairy-tale plane of storybook courts. Adventures give each card two uses, Roles enchant your creatures, bargain trades a token for a bigger effect, and Faeries and Rats run wild.',
    tryIf: 'two-for-one value and enchantment-based tricks',
  },
  {
    id: 'mode-ai-multiplayer',
    date: '2026-08-01',
    kind: 'mode',
    title: 'AI players at multiplayer tables',
    body: 'Fill Free-for-All and Two-Headed Giant seats with AI, who build and play their own Commander decks.',
  },
  {
    id: 'mode-commander-pods',
    date: '2026-07-31',
    kind: 'mode',
    title: 'Commander pods',
    body: 'Bring a commander to a three- or four-player pod table.',
  },
  {
    id: 'feature-saved-setups',
    date: '2026-07-31',
    kind: 'feature',
    title: 'Saved setups and rematch',
    body: 'Save a lobby setup to launch it again in one click, and start a rematch just as fast.',
  },
  {
    id: 'mode-cube',
    date: '2026-07-30',
    kind: 'mode',
    title: 'Cube Draft',
    body: 'Build and edit your own cubes, then draft them or play straight from a cube pool.',
  },
  {
    id: 'set-fdn',
    date: '2026-07-27',
    kind: 'set',
    setCode: 'FDN',
    title: 'Foundations is complete',
    body:
      'Magic’s long-running core set: clean, evergreen mechanics and familiar staples, designed as the best place to start.',
    tryIf: 'a clean, classic draft, or learning the game',
  },
  {
    id: 'set-atq',
    date: '2026-07-27',
    kind: 'set',
    setCode: 'ATQ',
    title: 'Antiquities is complete',
    body:
      'The 1994 story of the brothers Urza and Mishra: an all-artifact arms race of golems, war machines and the original Urza lands.',
    tryIf: 'artifacts and Magic history',
  },
  {
    id: 'feature-new-menu',
    date: '2026-07-26',
    kind: 'feature',
    title: 'A new home screen and Help',
    body: 'Three questions get you into a game, and the Help page explains modes, priority and shortcuts.',
  },
  {
    id: 'set-lci',
    date: '2026-07-25',
    kind: 'set',
    setCode: 'LCI',
    title: 'The Lost Caverns of Ixalan is complete',
    body:
      'Ixalan’s underground: descend into the caverns, discover free spells, craft artifacts into new forms, and stampede with Dinosaurs.',
    tryIf: 'exploration, big Dinosaurs and graveyard-depth payoffs',
  },
  {
    id: 'set-tla',
    date: '2026-07-06',
    kind: 'set',
    setCode: 'TLA',
    title: 'Avatar: The Last Airbender is complete',
    body:
      'Avatar: The Last Airbender. Airbend, earthbend, firebend and waterbend across the four nations, with Lessons and Allies at the heart of its decks.',
    tryIf: 'the show, and elemental tricks on every turn',
  },
  {
    id: 'set-fin',
    date: '2026-07-05',
    kind: 'set',
    setCode: 'FIN',
    title: 'Final Fantasy is complete',
    body:
      'Final Fantasy: heroes, summons and Towns from the whole series. Equipment, job-select creatures and big spells give it a classic RPG feel.',
    tryIf: 'RPG flavour, Equipment and legendary heroes',
  },
  {
    id: 'mode-ranked',
    date: '2026-06-28',
    kind: 'mode',
    title: 'Ranked play',
    body: 'Signed-in players earn a separate ELO rating for each game mode.',
  },
  {
    id: 'feature-friends',
    date: '2026-06-28',
    kind: 'feature',
    title: 'Friends, profiles and stats',
    body: 'Add friends, see who is online, and browse your game history, decks and head-to-heads.',
  },
  {
    id: 'set-tmt',
    date: '2026-06-28',
    kind: 'set',
    setCode: 'TMT',
    title: 'Teenage Mutant Ninja Turtles is complete',
    body:
      'Teenage Mutant Ninja Turtles: flood the board with Mutant tokens, swing in extra combats with Alliance, or play tempo by blinking and bouncing your own creatures.',
    tryIf: 'aggressive go-wide decks and pizza-fuelled flavour',
  },
  {
    id: 'set-dsk',
    date: '2026-06-28',
    kind: 'set',
    setCode: 'DSK',
    title: 'Duskmourn: House of Horror is complete',
    body:
      'A haunted house that hunts its guests. Rooms are enchantments that unlock door by door, manifest dread hides creatures face down, and eerie and survival reward braving the night.',
    tryIf: 'horror flavour and enchantment-heavy decks',
  },
  {
    id: 'feature-accounts',
    date: '2026-06-27',
    kind: 'feature',
    title: 'Free accounts',
    body: 'Sign in with a magic link — no password — to keep your decks across devices.',
  },
  {
    id: 'set-ltr',
    date: '2026-06-21',
    kind: 'set',
    setCode: 'LTR',
    title: 'The Lord of the Rings is complete',
    body:
      'The Fellowship’s journey through Middle-earth. The Ring tempts your Ring-bearer, Food fuels the hobbits, and Orc armies amass against you.',
    tryIf: 'Tolkien and legendary characters everywhere',
  },
  {
    id: 'set-sos',
    date: '2026-06-20',
    kind: 'set',
    setCode: 'SOS',
    title: 'Secrets of Strixhaven is complete',
    body:
      'A return to the mage school of Strixhaven, where each college casts its spells differently: Silverquill casualty, Prismari storm, Witherbloom affinity, Lorehold miracles and Quandrix cascade.',
    tryIf: 'instants and sorceries, and decks built around spells',
  },
  {
    id: 'set-big',
    date: '2026-06-18',
    kind: 'set',
    setCode: 'BIG',
    title: 'The Big Score is complete',
    body:
      'Thunder Junction’s bonus sheet of heist loot: powerful artifacts and legendary treasures that turn up in Outlaws boosters.',
    tryIf: 'flashy artifacts in your Thunder Junction drafts',
  },
  {
    id: 'set-otj',
    date: '2026-06-15',
    kind: 'set',
    setCode: 'OTJ',
    title: 'Outlaws of Thunder Junction is complete',
    body:
      'A wild-west heist. Commit crimes by targeting opponents, plot cards to cast them free later, saddle Mounts, and hire Mercenaries.',
    tryIf: 'outlaws, heists and flexible timing',
  },
  {
    id: 'mode-two-headed-giant',
    date: '2026-06-15',
    kind: 'mode',
    title: 'Two-Headed Giant and Team vs. Team',
    body: 'Team up: share a life total and a turn, in casual games or as a tournament format.',
  },
  {
    id: 'mode-momir',
    date: '2026-06-14',
    kind: 'mode',
    title: 'Momir Basic',
    body: 'Sixty basic lands and an avatar that turns mana into a random creature.',
  },
  {
    id: 'mode-free-for-all',
    date: '2026-06-13',
    kind: 'mode',
    title: 'Free-for-All multiplayer',
    body: 'Three or four players at one table, with spectators welcome.',
  },
  {
    id: 'feature-set-completion',
    date: '2026-06-13',
    kind: 'feature',
    title: 'Set Completion',
    body: 'See exactly which cards of every set are playable, set by set, before you pick one to draft.',
  },
  {
    id: 'feature-draft-assist',
    date: '2026-06-09',
    kind: 'feature',
    title: 'Draft and deckbuilding assistance',
    body: 'Optional pick ratings while you draft, and an auto-builder that proposes a deck from your pool.',
  },
  {
    id: 'set-eoe',
    date: '2026-06-08',
    kind: 'set',
    setCode: 'EOE',
    title: 'Edge of Eternities is complete',
    body:
      'Sci-fi Magic: station your creatures into huge Spacecraft, crack Landers for mana, and warp threats in early before they return for good.',
    tryIf: 'spaceships, ramp and sacrifice-heavy decks',
  },
  {
    id: 'set-tdm',
    date: '2026-06-05',
    kind: 'set',
    setCode: 'TDM',
    title: 'Tarkir: Dragonstorm is complete',
    body:
      'Dragons return to Tarkir, and the five three-colour clans — Abzan, Jeskai, Sultai, Mardu and Temur — each fight their own way. Mana fixing is generous, so go big on colours.',
    tryIf: 'three-colour decks and dragons',
  },
  {
    id: 'feature-deck-share-links',
    date: '2026-06-05',
    kind: 'feature',
    title: 'Shareable deck links',
    body: 'Send a deck to a friend as a single link that opens straight in the deckbuilder.',
  },
  {
    id: 'set-inv',
    date: '2026-05-31',
    kind: 'set',
    setCode: 'INV',
    title: 'Invasion is complete',
    body:
      'The classic multicolour block: the Phyrexian invasion of Dominaria, with kicker, domain and gold cards in every pack.',
    tryIf: 'multicolour decks and old-school power',
  },
  {
    id: 'set-dom',
    date: '2026-05-26',
    kind: 'set',
    setCode: 'DOM',
    title: 'Dominaria is complete',
    body:
      'A love letter to Magic’s history. Sagas tell stories over three turns, kicker gives every spell options, and historic rewards legends, artifacts and Sagas.',
    tryIf: 'flexible spells and a well-loved draft format',
  },
  {
    id: 'mode-commander-draft',
    date: '2026-05-16',
    kind: 'mode',
    title: 'Commander Draft and Sealed',
    body: 'Draft or open a pool, pick a commander from it, and play limited Commander.',
  },
  {
    id: 'set-ecl',
    date: '2026-05-08',
    kind: 'set',
    setCode: 'ECL',
    title: 'Lorwyn Eclipsed is complete',
    body:
      'Lorwyn returns with its old tribes — Kithkin, Merfolk, Faeries, Boggarts, Elves and Changelings — plus behold and blight, a darker take on the fairy-tale.',
    tryIf: 'tribal synergies and flash-speed tricks',
  },
  {
    id: 'mode-commander',
    date: '2026-05-08',
    kind: 'mode',
    title: 'Commander',
    body: 'Build a 100-card singleton deck around your commander and play it, with deck-list import and export.',
  },
  {
    id: 'mode-premade-tournaments',
    date: '2026-05-08',
    kind: 'mode',
    title: 'Premade Decks tournaments',
    body: 'Run a tournament where everyone brings their own constructed deck instead of opening boosters.',
  },
  {
    id: 'feature-deckbuilder',
    date: '2026-05-01',
    kind: 'feature',
    title: 'Deckbuilder',
    body: 'Build and save constructed decks from every playable card, with filters for keywords, power and toughness, and sets.',
  },
  {
    id: 'set-blb',
    date: '2026-04-04',
    kind: 'set',
    setCode: 'BLB',
    title: 'Bloomburrow is complete',
    body:
      'A woodland of animal folk. Every two-colour pair is a creature type — Bats, Otters, Frogs, Lizards, Squirrels, Rabbits, Birds, Mice and Raccoons — with forage, offspring and gift.',
    tryIf: 'cute creatures and clear, synergy-driven archetypes',
  },
  {
    id: 'feature-ai-drafters',
    date: '2026-04-02',
    kind: 'feature',
    title: 'AI drafters',
    body: 'Fill empty seats at a draft with AI players, in Booster, Winston and Grid Draft.',
  },
  {
    id: 'mode-vs-ai',
    date: '2026-03-20',
    kind: 'mode',
    title: 'Play against the AI',
    body: 'Nobody online? A built-in AI opponent plays full games against you, any time.',
  },
  {
    id: 'set-ktk',
    date: '2026-03-11',
    kind: 'set',
    setCode: 'KTK',
    title: 'Khans of Tarkir is complete',
    body:
      'Five warring clans, each in three colours: Abzan outlast, Jeskai prowess, Sultai delve, Mardu raid and Temur ferocious, with morph creatures hiding everywhere.',
    tryIf: 'three-colour decks and a classic, beloved draft',
  },
  {
    id: 'set-lgn',
    date: '2026-03-04',
    kind: 'set',
    setCode: 'LGN',
    title: 'Legions is complete',
    body:
      'The only set made entirely of creatures: Slivers share their abilities with each other, morph hides every threat face down, and provoke forces blocks.',
    tryIf: 'creature-heavy tribal battles',
  },
  {
    id: 'set-scg',
    date: '2026-03-03',
    kind: 'set',
    setCode: 'SCG',
    title: 'Scourge is complete',
    body:
      'The Onslaught block’s finale: Dragons rise, storm copies spells for each one before them, and landcycling turns big creatures into lands when you need them.',
    tryIf: 'tribal battles and big creatures',
  },
  {
    id: 'mode-winston-grid',
    date: '2026-02-28',
    kind: 'mode',
    title: 'Winston Draft and Grid Draft',
    body: 'Two drafting formats built for two players.',
  },
  {
    id: 'feature-replays',
    date: '2026-02-18',
    kind: 'feature',
    title: 'Game replays',
    body: 'Rewatch your games turn by turn with a timeline scrubber.',
  },
  {
    id: 'set-ons',
    date: '2026-02-16',
    kind: 'set',
    setCode: 'ONS',
    title: 'Onslaught is complete',
    body:
      'A tribal war on Otaria: Goblins, Elves, Clerics, Beasts and Soldiers each have a deck, and morph hides every creature face down until the right moment.',
    tryIf: 'tribal decks and bluffing with face-down creatures',
  },
  {
    id: 'mode-booster-draft',
    date: '2026-01-31',
    kind: 'mode',
    title: 'Booster Draft',
    body: 'Draft with friends, build from your picks, and play it out as a tournament.',
  },
  {
    id: 'feature-spectating',
    date: '2026-01-30',
    kind: 'feature',
    title: 'Spectate live games',
    body: 'Watch any game in progress, targeting arrows and all.',
  },
  {
    id: 'mode-sealed',
    date: '2026-01-27',
    kind: 'mode',
    title: 'Sealed tournaments',
    body: 'Open boosters, build a deck, and play rounds with standings — spectators welcome.',
  },
  {
    id: 'set-por',
    date: '2026-01-24',
    kind: 'set',
    setCode: 'POR',
    title: 'Portal is complete',
    body:
      'Magic’s original starter set: simple creatures, sorcery-speed spells and no complex rules. The first fully playable set here.',
    tryIf: 'learning Magic, or a calm game of creature combat',
  },
]
