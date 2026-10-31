/**
 * The one content source behind both help surfaces: the `/help` page and the inline
 * {@link HelpTip} popovers.
 *
 * Everything explained anywhere in the client should live here, and the call site should
 * reference a topic id rather than hold its own string. That single constraint is what stops the
 * drift that made ~40 scattered `title=` tooltips near-useless: each explanation now has exactly
 * one home, and the popover and the page can never disagree.
 *
 * **Audience: knows Magic, new to Argentum.** No rules teaching — nothing here explains what a
 * phase, the stack or a mulligan *is*. It explains what *this app* does with them.
 *
 * Typed TS rather than markdown on purpose: there is no markdown pipeline in the client, `public/`
 * ships no docs, and the Dockerfile copies only `dist/` + `nginx.conf` — the repo's `docs/` is not
 * reachable from the browser and never will be without new build machinery.
 *
 * `body` is a small block union rather than `ReactNode` so this stays a plain data module that
 * both surfaces can render (and that a lint/test can walk).
 */

import { SHORTCUTS } from './shortcuts'

export type HelpSection =
  | 'getting-started'
  | 'modes'
  | 'limited'
  | 'playing'
  | 'decks'
  | 'community'
  | 'cards'
  | 'advanced'

export type HelpBlock =
  | { kind: 'p'; text: string }
  | { kind: 'ul'; items: readonly string[] }
  /** Numbered steps — for "how to" sequences where the order is the point. */
  | { kind: 'ol'; items: readonly string[] }
  /** A sub-heading inside a long topic. */
  | { kind: 'h'; text: string }
  /** A highlighted aside: the one practical thing worth knowing. */
  | { kind: 'tip'; text: string }
  /** A small fact table — modes side by side, limits, defaults. Cells take `code` spans. */
  | { kind: 'table'; head: readonly string[]; rows: readonly (readonly string[])[] }
  /** Renders the full `shortcuts.ts` table. */
  | { kind: 'shortcuts' }

/** An off-site reference. Only for things we deliberately track rather than define ourselves. */
export interface HelpLink {
  label: string
  href: string
}

export interface HelpTopic {
  id: string
  section: HelpSection
  title: string
  /** One or two sentences — this is what the inline popover shows. */
  summary: string
  /** Longer prose, only rendered on `/help`. */
  body?: readonly HelpBlock[]
  /** Other topic ids, rendered as links. */
  related?: readonly string[]
  /** External references, rendered as outbound links below `related`. */
  links?: readonly HelpLink[]
  /** Ids from `shortcuts.ts`, rendered as chips under the topic. */
  shortcuts?: readonly string[]
}

export const HELP_SECTIONS: readonly { id: HelpSection; title: string; blurb: string }[] = [
  {
    id: 'getting-started',
    title: 'Getting started',
    blurb: 'Your first five minutes: the home screen, a name, a first game, and the words this wiki uses.',
  },
  {
    id: 'modes',
    title: 'Game modes',
    blurb: 'Every way to play — the mode catalogue, the four lobby axes, tables, events and the lobby itself.',
  },
  {
    id: 'limited',
    title: 'Draft & Sealed',
    blurb: 'Picking from packs, the draft formats, AI pick help, archetypes, building 40 cards and playing the bracket.',
  },
  {
    id: 'playing',
    title: 'Playing a game',
    blurb: 'The table, the pass button, priority and stops, decisions, combat, yields, and how a game ends.',
  },
  {
    id: 'decks',
    title: 'Decks',
    blurb: 'The deckbuilder, search, printings, formats and legality, Commander decks, import, export and sharing.',
  },
  {
    id: 'community',
    title: 'Accounts & community',
    blurb: 'Profiles, stats, friends, ranked ratings, spectating and replays.',
  },
  {
    id: 'cards',
    title: 'Cards & sets',
    blurb: 'The card browser, which cards and sets are playable, and Argentum Assay — the engine’s reader for Oracle text.',
  },
  {
    id: 'advanced',
    title: 'Advanced & Lab',
    blurb: 'Shortcuts, the multiplayer camera, troubleshooting, and the Lab tools for testing and debugging.',
  },
]

/**
 * The wiki front page's "Start here" row — the handful of topics a newcomer most often needs.
 * Checked by `topics.test.ts` like every other cross-reference.
 */
export const START_HERE_TOPIC_IDS: readonly string[] = [
  'first-game',
  'learn-to-play',
  'pass-button',
  'priority-modes',
  'cards-draft',
  'deckbuilder',
  'guest-vs-account',
  'troubleshooting',
]

export const HELP_TOPICS: readonly HelpTopic[] = [
  // ── Getting started ────────────────────────────────────────────────────
  {
    id: 'home-screen',
    section: 'getting-started',
    title: 'The home screen',
    summary:
      'The landing page is where every game starts: a catalogue of named modes under “Pick a game”, a Join box for invite codes, your saved setups, and lists of public lobbies and live games.',
    body: [
      { kind: 'h', text: 'Top bar' },
      { kind: 'ul', items: [
        'Decks & Cubes — everything you have built, on one shelf with a Decks tab and a Cubes tab. Show them as art tiles or as a compact list (the toggle beside the sort). Open a deck or cube in the builder, start a new one, or play a cube straight from the shelf.',
        'Cards — the card browser: every card you can play, to look through without building anything.',
        'Replays — every finished game you played, ready to rewatch (shown once you are connected).',
        'Sets — Set Completion: which sets and cards are playable.',
        'Learn — the Learn to Play course. Help — this wiki.',
        'The bell is What’s new; the corner button goes fullscreen; the account widget logs you in or opens your Profile, Stats and Friends.',
      ] },
      { kind: 'h', text: 'Main column' },
      { kind: 'ul', items: [
        '“Lobby still open” — if you reloaded with a lobby open, Rejoin takes you back; × forgets it.',
        '“Jump back in” — your saved setups and your last setup, one click each.',
        '“Pick a game” — the mode catalogue, grouped into Play right now, Build from packs and Multiplayer tables. Next to it, “Have a code?” joins any lobby.',
      ] },
      { kind: 'h', text: 'Side column' },
      { kind: 'ul', items: [
        'Open lobbies — public lobbies anyone may join, newest first, with an online-player count. Each shows the table (1v1, Free-for-All, tournament…), what is being played, who is hosting and who has joined, and how many seats are left. Seats taken by AI players are marked apart from people — square pips and a robot badge — so you can tell a table of humans from one padded with AI. With many lobbies, filter by 1v1 / Multiplayer / Tournaments or search by host, set or format. Refreshes every few seconds.',
        'Live Games — games in progress, each with a Spectate button.',
        'A Learn to Play row, and for guests a reminder of what a free account adds.',
      ] },
      { kind: 'p', text: 'The footer links Discord, GitHub and “Help build it”, and — in development builds only — the Lab tools.' },
    ],
    related: ['first-game', 'saved-setups', 'whats-new', 'spectating', 'invite-codes', 'lab-tools'],
  },
  {
    id: 'learn-to-play',
    section: 'getting-started',
    title: 'New to Magic itself? Learn to Play',
    summary:
      'This wiki assumes you know the game. If you do not, the Learn to Play course teaches it by playing: five short games against an AI called Tutor, each from a board set up to teach one thing, with a coach saying what to do next and pointing at the part of the table to do it on.',
    body: [
      { kind: 'p', text: 'The course lives at /learn (the Learn link in the top bar) and needs no name or account. Progress is kept in this browser, and the landing page keeps a row pointing at it — including after you have finished, so any mission can be played again. The whole course takes about half an hour.' },
      { kind: 'table', head: ['Mission', 'Teaches', 'Time'], rows: [
        ['1 · First steps', 'Lands and mana, casting a creature, summoning sickness, the pass button', '4 min'],
        ['2 · Holding the line', 'Blocking, power against toughness, flash, the game log', '5 min'],
        ['3 · In response', 'Instants and the stack, the Auto priority mode', '5 min'],
        ['4 · Answers', 'Removal, Auras, the graveyard', '5 min'],
        ['5 · A real game', 'A full game at 20 life with two-colour decks, plus Undo and the land-tap toggle', '10 min'],
      ] },
      { kind: 'p', text: 'Each mission page shows what you will do and your opening cards before you sit down. In the game, the coach lists the mission’s objectives, explains each keyword the first time it appears, and finishes with a “What you now know” recap. You can tuck the coach away at any time.' },
      { kind: 'tip', text: '“Skip to the table” on the Learn page goes straight to the home screen once you feel ready.' },
    ],
    links: [{ label: 'Learn to play', href: '/learn' }],
    related: ['first-game', 'roster-solo'],
  },
  {
    id: 'pick-a-name',
    section: 'getting-started',
    title: 'Picking a name',
    summary:
      'Type any name to start playing straight away — no sign-up. The name is remembered in this browser and is what opponents see.',
    body: [
      { kind: 'p', text: 'Nothing is gated behind an account. A name is enough to create a lobby, join one with a code, or play the AI.' },
    ],
    related: ['guest-vs-account'],
  },
  {
    id: 'guest-vs-account',
    section: 'getting-started',
    title: 'Guest vs. account',
    summary:
      'Guests can play everything. An account (one magic link, no password) adds decks that follow you between devices, friends, messages, ranked play, stats and saved replays.',
    body: [
      { kind: 'p', text: 'Signing in later keeps the decks you built as a guest — you are offered a one-click migration.' },
      { kind: 'ul', items: [
        'Decks saved to the account instead of this browser',
        'Friends list and online presence',
        'Private messages with other players',
        'Preferences (stops, stacking, motion) that follow you between devices',
        'Ranked games (every player in the game must be signed in, otherwise it silently plays unranked)',
        'Full stats dashboard and permanent replays',
      ] },
    ],
    related: ['ranked', 'replays', 'messages', 'preferences'],
  },
  {
    id: 'first-game',
    section: 'getting-started',
    title: 'Starting your first game',
    summary:
      'Pick a mode from the “Pick a game” catalogue on the home screen, choose who to play with — AI, friends or anyone — and press the button. Against the AI you are seated straight away; with people you land in a lobby to share.',
    body: [
      { kind: 'ol', items: [
        'Type a name if you have not yet (no sign-up needed).',
        'Pick a mode tile. “Constructed” plays a saved deck; “Random deck” and “Jump In” need no deck at all.',
        'Choose “Play with”: AI (starts right away), Friends (an invite link) or Anyone (a public lobby).',
        'Adjust the few options the panel shows — AI opponents, set, your deck — and press Play.',
      ] },
      { kind: 'p', text: 'The panel’s footer spells out what happens next, step by step — for a draft, “Draft → Build 40 → Everyone plays everyone → Standings” — so you know before clicking whether you are about to play one game or start an event.' },
      { kind: 'p', text: 'Everything you start with people ends in a lobby, and every lobby shows the same settings. Nothing is a dead end: a lobby you opened as a 1v1 can become a four-player game without going back to the menu.' },
      { kind: 'tip', text: 'Fastest possible game: “Random deck” → AI → Play. The server deals you a ready-made deck and the game starts.' },
    ],
    related: ['play-wizard', 'axes', 'invite-codes', 'lobby-screen'],
  },
  {
    id: 'invite-codes',
    section: 'getting-started',
    title: 'Invite codes and links',
    summary:
      'Every lobby has a short code. Paste it into the Join field on the home screen, scan its QR code, or open the share link — all three land in the same lobby.',
    body: [
      { kind: 'p', text: 'The Join field does not care what kind of lobby a code belongs to; it routes for you.' },
    ],
  },
  {
    id: 'matchmaking',
    section: 'getting-started',
    title: 'Finding an opponent',
    summary:
      'No one to invite? Pick casual or ranked and a format on the home screen and press Search — the server pairs you with another player searching for the same thing.',
    body: [
      { kind: 'p', text: 'Limited gives each of you a random sealed pool; every other format plays one of your saved decks. Casual and ranked are separate queues, and ranked needs both players signed in.' },
      { kind: 'p', text: 'Casual pairs the two players who have waited longest. Ranked looks for an opponent within 100 rating points, widens that range the longer you wait, and after two minutes takes anyone in the queue.' },
      { kind: 'p', text: 'When a match is found, both players get a one-minute accept prompt. If the other player declines or does not answer, you go back into the queue at your old place. Your search keeps running while you browse other pages, such as the deckbuilder.' },
      { kind: 'tip', text: 'Queues that already have someone waiting are listed above the search line; press Play on one to be paired at once.' },
      { kind: 'p', text: 'A matched lobby has no host, invite code, AI seats or settings to change, and it closes if either player leaves.' },
      { kind: 'tip', text: 'Bored of waiting? Press Play the AI while you wait under the search line. You stay in the queue; when a match is found you are asked first, and accepting ends the AI game (it does not count in your stats).' },
    ],
    related: ['after-the-game', 'invite-codes', 'ranked', 'home-screen'],
  },
  {
    id: 'where-decks-live',
    section: 'getting-started',
    title: 'Where your decks live',
    summary:
      'Saved decks live in this browser until you sign in, and in your account afterwards. The Decks tab of Decks & Cubes (and the deckbuilder’s My Decks list) is the same list every lobby deck picker reads from.',
    body: [
      { kind: 'p', text: 'Browser storage is per-browser and easy to lose: clearing site data or history takes your decks with it, and they do not follow you to a phone or a second computer. An account is the fix — signing in later keeps everything you already built, and you are offered a one-click migration.' },
      { kind: 'p', text: 'A deck you want to keep without an account can also be exported as a decklist or a share link, both of which survive the browser.' },
    ],
    related: ['guest-vs-account', 'deckbuilder', 'deck-sharing', 'deck-import-export'],
  },
  {
    id: 'whats-new',
    section: 'getting-started',
    title: 'What’s new',
    summary:
      'The bell in the top bar is a short, hand-picked feed of milestones: a set becoming fully playable, a new way to play, or a feature big enough to change how you use the site.',
    body: [
      { kind: 'p', text: 'The badge counts entries you have not read; filter by All, Sets, or Modes & features, and Mark all as read when you are done. Read state is kept in this browser — on a first visit, the last month of entries shows as unread.' },
      { kind: 'p', text: 'Each set entry is also a pitch: what the set is like to play, and a “Try it if you like…” line, so you can decide from the feed alone whether it is worth a draft.' },
    ],
    related: ['set-completion', 'home-screen'],
  },
  {
    id: 'glossary',
    section: 'getting-started',
    title: 'Glossary',
    summary:
      'The words this app and this wiki use that are not Magic rules terms — from “axis” and “bracket” to “yield”, “hotseat” and “fineness”.',
    body: [
      { kind: 'table', head: ['Term', 'Meaning here'], rows: [
        ['Assay', 'Argentum Assay, the engine’s reader for printed Oracle text. ⚡ Assay-ready cards are ones it reads end to end.'],
        ['Axis', 'One of the four independent lobby choices: Cards, Rules, Table, Event.'],
        ['Bracket', 'A round-robin event: everyone plays everyone in 1v1 matches, with standings.'],
        ['Bye', 'A bracket round you sit out because the player count is odd.'],
        ['Cube', 'Your own hand-picked card pool, used instead of real sets for sealed or draft.'],
        ['Fineness', 'Assay’s coverage figure, in parts per thousand.'],
        ['Full Control', 'The priority mode that stops at every step.'],
        ['Hotseat', 'One person playing every seat from one window — a Scenario Builder option.'],
        ['Jump In', 'Pick two themed 20-card Jumpstart packs and play the 40-card deck they make.'],
        ['Lab', 'Development tools — Scenario Builder, AI Sandbox, LLM Tournament — only in dev builds.'],
        ['Placement', 'Your first 10 ranked games in a category, where the rating moves fastest.'],
        ['Pod', 'A multiplayer table: Free-for-All, Two-Headed Giant or Team vs. Team.'],
        ['Pool Play', 'Cube sealed where everyone builds from the whole cube, up to 4 copies each.'],
        ['Setup', 'A saved lobby configuration you can relaunch from “Jump back in”.'],
        ['Stop', 'A phase-bar marker that guarantees you priority at that step.'],
        ['Yield', 'A standing instruction to stop being asked about a repeating ability.'],
        ['⇄', 'A lobby option that can only be applied by opening a fresh lobby.'],
      ] },
    ],
    related: ['axes', 'priority-modes', 'yields', 'assay'],
  },

  // ── Game modes ─────────────────────────────────────────────────────────
  {
    id: 'play-wizard',
    section: 'modes',
    title: 'The mode catalogue: Pick a game',
    summary:
      'The home screen lists every way to play as a named tile, in three groups: Play right now (no deckbuilding), Build from packs (draft or sealed, then 40 cards), and Multiplayer tables (three or more players in one game).',
    body: [
      { kind: 'table', head: ['Mode', 'Players', 'What it is'], rows: [
        ['Constructed', '2–8', 'Bring one of your saved decks — one game, or a bracket with more players.'],
        ['Jump In', '2–8', 'Choose two themed Jumpstart packs; they make a 40-card deck, lands included.'],
        ['Random deck', '2', 'The server rolls a ready-made deck for you. Zero preparation.'],
        ['Momir Basic', '2', 'Sixty basics; discard one to create a random creature of that mana value.'],
        ['Draft', '2–8', 'Booster, Winston, Grid or Commander draft, then build 40 and play everyone.'],
        ['Sealed', '2–8', 'Open your own boosters (standard or Commander packs) and build from them.'],
        ['Free-for-All', '3–6', 'One shared game, every player for themselves.'],
        ['Two-Headed Giant', '4', 'Two teams of two sharing 30 life, turns and combat.'],
        ['Team vs. Team', '4–8', 'Two even teams; every player keeps their own life total and turn.'],
        ['Commander', '2–6', 'Commander rules with your own decks: a 1v1 duel or a Free-for-All pod.'],
      ] },
      { kind: 'p', text: 'Clicking a tile opens its launch panel (each one has its own link, `/play/<mode>`, so you can bookmark one). The panel asks only what that mode needs:' },
      { kind: 'ul', items: [
        'Play with — AI (starts right away), Friends (an invite link) or Anyone (a public lobby that shows up on everyone’s home screen).',
        'AI opponents — how many AI seats to fill, where the mode allows a choice.',
        'Mode-specific picks — the draft style, Standard or Commander boosters, the set, which deck you bring, or where a multiplayer table’s decks come from (your decks, Jump In, sealed or draft).',
        'Random deck from — whenever the server rolls your deck (the Random deck mode, or “Random deck” picked as your Constructed deck), optionally pin the set it is built from. Left on “Any set”, the server picks one set for the whole table: an opponent also on “Any set”, or an AI opponent on its default, plays the same set as you.',
      ] },
      { kind: 'p', text: 'Your choices are remembered per mode. The footer says what happens next — one game, or an event with stages — and the button says exactly what it will do: Play, Start draft, Open boosters, Create lobby or Open public lobby.' },
      { kind: 'p', text: 'Anything the panel does not ask keeps its default — the timer, pack count and so on — and stays editable in the lobby until you start. AI games skip the lobby, unless you click “Open a lobby to change them” under the Play button: the same game then opens as a lobby you start yourself, with every setting and AI seat editable.' },
    ],
    related: ['axes', 'roster-solo', 'roster-friend', 'roster-group', 'saved-setups', 'lobby-screen'],
  },
  {
    id: 'axes',
    section: 'modes',
    title: 'The four choices: Cards, Rules, Table, Event',
    summary:
      'Every game here is four independent picks: where your cards come from, which rules they are played under, who is at the table, and whether it is one game or a series.',
    body: [
      { kind: 'ul', items: [
        'Cards — bring a deck, a random pool, Momir Basic, Sealed, or one of the four drafts.',
        'Rules — Standard, or Commander.',
        'Table — 1v1, Free-for-All, Two-Headed Giant, or Team vs. Team.',
        'Event — a single game, or a round-robin bracket with standings.',
      ] },
      { kind: 'p', text: 'Read them in that order: what deck, under what rules, at what table, over how many games.' },
      { kind: 'p', text: 'They are independent: “Sealed” is not an alternative to “Tournament”, it is an alternative to “Draft”. A tournament is an alternative to a single game. And Commander is not a kind of draft — it is the rules row, so you can play Commander with a deck you built, with a sealed pool, or with a draft. That is why a 1v1 sealed game with one friend and an eight-player bracket are the same screen with different settings.' },
      { kind: 'p', text: 'The home screen’s mode catalogue picks all four for you from the tile you choose (a Commander draft or sealed means Commander rules); the lobby then lets you change any of the four, Rules included.' },
    ],
    related: ['cards-sealed', 'rules-commander', 'table-free-for-all', 'event-round-robin', 'axis-limits', 'lobby-switching'],
  },
  {
    id: 'cards-bring-a-deck',
    section: 'modes',
    title: 'Cards: Bring a deck',
    summary:
      'Play a deck you built or pasted. Optionally restrict everyone to a constructed format — Standard, Pioneer, Modern, Pauper, Legacy, Vintage, Commander, Brawl, Standard Brawl, Premodern, Highlander or Canadian Highlander.',
    body: [
      { kind: 'p', text: '“No restriction” lets any legal-in-the-engine card in. The restriction is checked when a deck is submitted, not when it is built.' },
    ],
    related: ['deckbuilder', 'highlander'],
  },
  {
    id: 'highlander',
    section: 'modes',
    title: 'Highlander and Canadian Highlander',
    summary:
      '100-card singleton decks without a commander, over the Vintage card pool, at 20 life. Canadian Highlander adds a points list: the strongest cards cost points, and a deck may spend at most 10.',
    body: [
      { kind: 'p', text: 'Both are deck-legality choices, not separate modes: pick one as the format in the deckbuilder, as the restriction under Cards → Bring a deck, or as the Constructed format in matchmaking. The game itself plays like any 1v1 constructed game.' },
      {
        kind: 'table',
        head: ['', 'Highlander', 'Canadian Highlander'],
        rows: [
          ['Deck size', 'At least 100', 'At least 100'],
          ['Copies', 'One of each card except basic lands', 'One of each card except basic lands'],
          ['Card pool', 'Vintage (restricted cards allowed)', 'Vintage (restricted cards allowed)'],
          ['Points', 'None', 'At most 10, from the format’s points list'],
          ['Sideboard', 'Allowed', 'None — the format has no sideboards'],
        ],
      },
      { kind: 'p', text: 'With Canadian Highlander picked, the deckbuilder shows each pointed card’s cost on its row and the deck’s total beside the card count.' },
      { kind: 'tip', text: 'The points list changes a few times a year. Argentum follows the format’s official list; see the link below for the current one.' },
    ],
    related: ['cards-bring-a-deck', 'deck-legality', 'matchmaking'],
    links: [{ label: 'Canadian Highlander points list', href: 'https://www.canadianhighlander.ca/points-list/' }],
  },
  {
    id: 'cards-random',
    section: 'modes',
    title: 'Cards: Random pool',
    summary:
      'The server picks a deck for you. The fastest route from a cold start to actually playing — no deckbuilding, no pool to sort.',
    body: [
      { kind: 'p', text: 'Without a format, the server opens eight boosters and builds the deck from them; the cards it left out are your sideboard, as in Sealed. Under a constructed format you get a 60-card deck and a fifteen-card wishboard from the same legal pool, so a wish or Learn card it rolls has something to fetch.' },
    ],
    related: ['outside-the-game'],
  },
  {
    id: 'cards-momir',
    section: 'modes',
    title: 'Cards: Momir Basic',
    summary:
      'No deckbuilding at all. Everyone runs 60 basic lands; discard a card and pay {X} to put a random creature with mana value X onto the battlefield.',
    body: [
      { kind: 'p', text: 'The Momir Vig avatar sits in the command zone. Creatures are rolled from every implemented set, so games look nothing alike.' },
      { kind: 'p', text: 'Every game also rolls one shared wishboard: fifteen cards both players own outside the game, at least a few of them Lessons. When the avatar hands you a creature that learns, or one that wishes, this is where it looks — open your deck (D) and pick the Outside the game tab to see it.' },
    ],
    related: ['outside-the-game'],
  },
  {
    id: 'cards-jump-in',
    section: 'modes',
    title: 'Jump In',
    summary: 'Choose two themed 20-card Jumpstart packs and play their combined deck. Lands are included, so there is nothing to build.',
    body: [{ kind: 'p', text: 'Start with Jumpstart 2022, or select the original Jumpstart set in the lobby. Each pick offers three different themes, with a published pack variant behind each. Explore any pack card by card before choosing. On your second pick your first pack stays on screen, and hovering an offer shows the colors and curve of the deck the two would make — you can still swap your first pack until you choose the second. After two picks, your exact 40-card deck is submitted automatically. Play a bracket with friends or AI opponents, or sit everyone down at one Free-for-All table; only complete, implemented packs without banned cards are offered.' }],
    related: ['roster-solo', 'roster-friend', 'event-round-robin', 'table-free-for-all'],
  },
  {
    id: 'cards-sealed',
    section: 'modes',
    title: 'Cards: Sealed',
    summary:
      'Open boosters and build a 40-card deck from what you got. Standard sealed uses 6 boosters by default; Commander Sealed opens Commander-shaped packs and builds a 60-card deck around a commander from your pool — up to 8 players share the pool, then play a 1v1 bracket or sit down as one pod at 40 life.',
    body: [
      { kind: 'p', text: 'The host picks the sets (mix several, or add a deferred “Random Set” that stays hidden until the game starts), how many boosters each player opens, and whether boosters are per-set or “chaos” — each pack mixing every selected set.' },
      { kind: 'p', text: 'A per-set booster opens like that set’s paper pack: 8-card packs for Arabian Nights, Antiquities, The Dark and Fallen Empires, each card as common as it was on the print sheet; Midnight Hunt’s and Crimson Vow’s two double-faced cards; March of the Machine’s battle; a Play Booster’s own wildcard odds, and its land slot when that can be a common dual or gain land. Foils, The List, Special Guests and bonus-sheet cards aren’t included, and a basic land only comes in a pack when it shares the slot with nonbasic lands — you get basics free while building.' },
    ],
    related: ['limited-deckbuilding', 'cards-draft', 'cards-cube'],
  },
  {
    id: 'cards-draft',
    section: 'modes',
    title: 'Cards: Draft',
    summary:
      'Four shapes: Booster (pass packs, 3–8 players), Winston (three face-down piles, exactly 2), Grid (pick a row or column from a 3×3 grid, 2–4) and Commander (Commander-shaped packs, up to 8 drafters).',
    body: [
      { kind: 'p', text: 'The host sets a pick timer and, for Booster and Commander drafts, whether each pick takes one card or two.' },
    ],
    related: ['limited-deckbuilding', 'cards-sealed', 'cards-cube'],
  },
  {
    id: 'cards-cube',
    section: 'modes',
    title: 'Cards: Cube',
    summary:
      'A cube is your own hand-picked card pool used instead of real sets. Any limited format can run on it — Sealed, Booster, Winston or Grid draft. A shuffled cube deals its packs; a cube set to Play or Draft Boosters prints them by rarity, like a real set.',
    body: [
      { kind: 'p', text: 'A cube replaces the set picker: pick a cube in the lobby and the Sets, booster-mix and per-set pack controls disappear, because there are no sets involved any more. Everything else about the format is unchanged — the same pick timers, the same deck building, the same bracket.' },
      { kind: 'p', text: 'A shuffled cube — the classic kind — deals its packs without replacement across the whole event. No card ever appears twice — not in two of your packs, and not in two different players’ pools. That is what makes a cube a curated pool rather than a set: every card in it is a deliberate choice, and each one shows up exactly as often as you put it in.' },
      { kind: 'p', text: 'A cube can instead open as boosters — see “Set cubes and booster packs”. Then packs are printed by rarity like a real set’s, and the capacity rule below doesn’t apply.' },
      { kind: 'ul', items: [
        'Pack size is part of a shuffled cube (15 by default, the community standard).',
        'Capacity is the one new constraint: players × packs × pack size must fit inside the cube. A 360-card cube seats 8 players at 3 packs of 15 exactly, and the lobby shows the sum live.',
        'Basic lands aren’t part of a cube — it names a set to take their art from, and the deckbuilder gives you unlimited basics as usual.',
        'The host’s ban list still applies, and banned cards come off the cube before capacity is counted.',
      ] },
      { kind: 'p', text: 'Cube Pool Play is the exception to all the dealing: it skips packs entirely. See its own topic.' },
    ],
    related: ['cube-building', 'cube-boosters', 'cube-sharing', 'cube-pool-play', 'cards-sealed', 'cards-draft'],
  },
  {
    id: 'cube-boosters',
    section: 'modes',
    title: 'Set cubes and booster packs',
    summary:
      'Set a cube’s Packs to Play Boosters or Draft Boosters and its packs are printed by rarity, the way a real set’s are: commons, uncommons, a rare that is a mythic one pack in eight. Ideal for a set extended with extra cards.',
    body: [
      { kind: 'p', text: 'Every card in a cube has a rarity. Cards added with “Add a set” keep the rarity that set printed them at; any other card uses its own, and you can change any card’s rarity by clicking its chip in the editor. A filled chip is a rarity you set for this cube; an outlined one is the card’s own.' },
      { kind: 'ul', items: [
        'Play Boosters: 7 commons, 3 uncommons, a rare or mythic, and 2 wildcards of any rarity — 13 cards a pack.',
        'Draft Boosters: 10 commons, 3 uncommons, and a rare or mythic — 14 cards a pack.',
        'No card repeats inside a pack, but packs are printed independently, so two players can open the same rare. There is no capacity limit — any cube can seat a full table.',
        'Running 2 copies of a card makes it twice as likely to show up in its rarity slot.',
        'A full pack needs enough different commons, uncommons and rares to fill its slots; the editor’s pack strip shows any slot the cube can’t fill.',
      ] },
      { kind: 'p', text: 'Example — an extended Bloomburrow: start a new cube from the Cubes tab of Decks & Cubes, Add a set → Bloomburrow, then Add a set → Bloomburrow Commander with Reprints turned off, so only its new Bloomburrow cards come in. Set Packs to Play Boosters, save, and draft it like the real set.' },
    ],
    related: ['cards-cube', 'cube-building', 'cube-sharing', 'cards-draft'],
  },
  {
    id: 'cube-pool-play',
    section: 'modes',
    title: 'Cube Pool Play',
    summary:
      'Cube Sealed with no dealing: every player builds from the entire cube at the same time, using up to 4 copies of any card. Nobody competes for cards, so the cube can be any size.',
    body: [
      { kind: 'p', text: 'Ordinary Sealed hands you a pool and the deck you can build is limited by what you opened. Pool Play removes that constraint: the whole cube is your pool, and so is everyone else’s. Two players can both build the same deck.' },
      { kind: 'p', text: 'It is closer to constructed than to limited — the cube becomes a shared, curated card list that everyone brews from at once. Good for testing a cube you are still tuning, for a group with wildly different experience levels, or for a small cube that could never seat the table under the usual capacity rule.' },
      { kind: 'ul', items: [
        'Up to 4 copies of any card, exactly like constructed. Basic lands stay unlimited.',
        'Minimum deck size is still 40.',
        'No capacity constraint — nothing is dealt, so a 100-card cube works fine for 8 players.',
        'A fifteen-card sideboard. In limited your sideboard is everything you didn’t play, but here that would be the whole cube, so instead you get fifteen random cube cards your deck doesn’t play — something for wishes and Learn to fetch.',
        'Pack size and pack count are ignored, and the controls for them are hidden.',
      ] },
      { kind: 'p', text: 'Set it on a cube Sealed lobby with the Card pool control: “Sealed packs” deals pools from the cube as usual, “Pool Play” gives everyone all of it.' },
    ],
    related: ['cards-cube', 'cube-building', 'cards-sealed', 'limited-deckbuilding', 'outside-the-game'],
  },
  {
    id: 'rules-standard',
    section: 'modes',
    title: 'Rules: Standard',
    summary:
      'The ordinary rules for whichever table you picked — 20 life at 1v1, no command zone, and 4 copies of a card if the deck legality allows it.',
    related: ['rules-commander', 'axes', 'playing-for-ante'],
  },
  {
    id: 'rules-commander',
    section: 'modes',
    title: 'Rules: Commander',
    summary:
      'Everyone designates a commander. It starts in the command zone, can be cast from there (paying {2} more each time it has been cast that way), returns there when it would die, and 21 combat damage from a single commander knocks a player out (CR 903).',
    body: [
      { kind: 'p', text: 'Rules is its own row, independent of where the cards came from: you can play Commander with a deck you built, with a sealed pool, or with any draft. “Commander Draft” and “Commander Sealed” are about the *packs* — Commander-Legends-shaped 20-card boosters with a legend in every one — and they simply switch this row on for you.' },
      { kind: 'p', text: 'Life totals: a pod plays paper multiplayer Commander’s 40. A 1v1 limited Commander game is tuned faster — the host picks Brawl (25 life, 16 commander damage) or Commander (30/21) — and a bracket honours that choice, while any multiplayer table overrides it back to 40.' },
      { kind: 'p', text: 'It plays at every table except Two-Headed Giant, whose team shares one life total (CR 810.4) and so has nowhere to put Commander’s per-player 40. Free-for-All and Team vs. Team pods are the ones to use.' },
      { kind: 'p', text: 'Setting the deck legality to Commander, Brawl or Standard Brawl is a *deck-construction* restriction — singleton, colour identity, card legality — and it switches this row on, because the two are not quite independent in that direction: colour identity is defined as a subset of *the commander’s* (CR 903.4), so without a commander there is nothing to measure it against. The reverse is free — Commander rules with no legality restriction at all is an ordinary thing to want, and the pod tests use exactly that.' },
      { kind: 'p', text: 'That is also why the commander legalities aren’t offered at a Two-Headed Giant table: not a separate rule, just the one above arriving by a different route.' },
      { kind: 'p', text: 'A deck with two partner commanders starts with both in the command zone. Each is taxed separately for its own casts, and commander damage is tracked per commander: 21 from one of them knocks a player out, damage from the two never adds up.' },
    ],
    related: ['rules-standard', 'axes', 'axis-limits', 'cards-bring-a-deck', 'commander-decks'],
  },
  {
    id: 'table-1v1',
    section: 'modes',
    title: 'Table: 1v1',
    summary: 'Two players, 20 life each. The table for every mode outside Multiplayer tables, unless you add more players and switch to a bracket.',
  },
  {
    id: 'table-free-for-all',
    section: 'modes',
    title: 'Table: Free-for-All',
    summary:
      'One game, everyone at the same table (2–6 players). Last player standing wins.',
    body: [
      { kind: 'p', text: 'The host chooses who each creature may attack: only the player to your left or right (CR 803), or any opponent (CR 802). A Commander pod starts on any opponent; every other pod starts on attack left. “Left” and “right” follow the seating order shown in the lobby. In game, the seat list names the rule, tags the one opponent you can attack (⚔ TARGET) and the one who can attack you (🛡 ATTACKS YOU), and dims everyone you can’t attack while you declare attackers.' },
    ],
    related: ['table-team-vs-team', 'multiplayer-camera'],
  },
  {
    id: 'table-two-headed-giant',
    section: 'modes',
    title: 'Table: Two-Headed Giant',
    summary:
      'Exactly four players in two teams of two (CR 810). Each team shares one 30-life total, takes its turns together, and attacks and blocks as one unit.',
    body: [
      { kind: 'p', text: 'Teams are randomised at game start by default, re-rolled every game. Switch to “Choose teams” and the host can click each player’s team chip to assign them by hand.' },
    ],
    related: ['table-team-vs-team'],
  },
  {
    id: 'table-team-vs-team',
    section: 'modes',
    title: 'Table: Team vs. Team',
    summary:
      'An even pod (4, 6 or 8) split into two teams — 2v2, 3v3 or 4v4 (CR 808). Unlike Two-Headed Giant nothing is shared: each player keeps their own life and their own turn, and is knocked out individually. The last team with anyone standing wins.',
    related: ['table-two-headed-giant'],
  },
  {
    id: 'event-single-game',
    section: 'modes',
    title: 'Event: Single game',
    summary: 'One game, then everyone is back at the lobby. Multiplayer tables offer a “Play Again” ready loop.',
    related: ['axis-limits'],
  },
  {
    id: 'event-round-robin',
    section: 'modes',
    title: 'Event: Round-robin bracket',
    summary:
      'Everyone plays everyone in a series of 1v1 matches; standings update after each round and most match wins takes it.',
    body: [
      { kind: 'p', text: 'Standings show wins–losses–draws, points and game win rate; hovering a row spells out the tiebreakers actually used (opponents’ match win %, game win %, opponents’ game win %, life differential).' },
      { kind: 'p', text: 'Odd player counts give someone a bye each round. When a round ends, everyone readies up for the next one; the host can add an extra round after the bracket completes.' },
    ],
    related: ['ranked', 'axis-limits'],
  },
  {
    id: 'playing-for-ante',
    section: 'modes',
    title: 'Playing for ante',
    summary:
      'An opt-in lobby setting: each player antes a random card from their deck before the game, and the winner takes the ante. Ranked games never play for ante.',
    body: [
      { kind: 'p', text: 'The host turns it on with the Play for ante row in the lobby’s Rules settings — in a quick game or in a tournament or Free-for-All lobby. Everyone else sees a “Stakes: For ante” chip at the top of the lobby, so nobody walks into it by surprise. Ranked and ante don’t mix: while Ranked is on, the ante row is locked off.' },
      { kind: 'p', text: 'Before the opening draw, a random card from each player’s deck goes into the ante. You can see every player’s stake on the board — a bronze Ante pile beside their command zone. Hover it for the card, click it to see every card staked so far. A “For ante” chip beside the phase bar reminds the table what’s at stake.' },
      { kind: 'h', text: 'What the winner gets' },
      { kind: 'ul', items: [
        'In a Limited tournament (sealed or draft), the cards the winner takes join their pool for the rest of the event. A loser whose deck falls below the minimum is topped back up with a basic land.',
        'Anywhere else, the win is a trophy: the card is listed in the Spoils panel on the winner’s profile. Nobody’s saved decks change.',
        'In a draw, the ante goes back to its owners.',
      ] },
      { kind: 'p', text: 'The game-over screen shows what you won and what you lost.' },
      { kind: 'h', text: 'Ante cards' },
      { kind: 'p', text: 'Cards that only work with ante — Contract from Below, Darkpact, Demonic Attorney, Jeweled Bird and Bronze Tablet — are legal only in a game played for ante. A deck holding one is refused in any other lobby. When one of them targets a card in the ante (Darkpact), you pick it from a browser of every player’s ante.' },
    ],
    related: ['ranked', 'profile', 'tournament-flow', 'lobby-screen', 'game-over'],
  },
  {
    id: 'ranked',
    section: 'community',
    title: 'Ranked play and ratings',
    summary:
      'Ranked games adjust each player’s ELO. Every player must be signed in — otherwise the game still runs, but silently counts as unranked.',
    body: [
      { kind: 'p', text: 'Turn it on with the Ranked row in a lobby’s Event settings. Ranked is available for 1v1 games only — a single game between two signed-in people, or a 1v1 bracket. Multiplayer tables and games against the AI are always casual.' },
      { kind: 'p', text: 'Matchmaking also has ranked queues: choose Ranked before you search, and the game counts automatically.' },
      { kind: 'p', text: 'You have three separate ratings, one per kind of game:' },
      { kind: 'table', head: ['Rating', 'Counts games played with'], rows: [
        ['Constructed', 'Your own or a restricted-format deck, and Momir Basic'],
        ['Limited', 'A pool: random decks, sealed and draft'],
        ['Commander', 'Commander rules'],
      ] },
      { kind: 'p', text: 'Every rating starts at 1200. Your first 10 games are placement and move it quickly; after that, an even game moves it by about 10 points. Tiers: Bronze below 1000, Silver to 1200, Gold to 1400, Platinum to 1600, Diamond to 2000, and Mythic above.' },
      { kind: 'p', text: 'Ratings, tiers and a rating-over-time chart appear on your Stats page and public profile once you have played a ranked game.' },
    ],
    related: ['guest-vs-account', 'axis-limits', 'stats', 'matchmaking', 'playing-for-ante'],
  },
  {
    id: 'profile',
    section: 'community',
    title: 'Your profile',
    summary:
      'Profile (from the account menu) shows your record at a glance, your recent games with both decks and their replays, and your recent tournaments. You can change your display name and avatar here.',
    body: [
      { kind: 'ul', items: [
        'Games, wins, losses and win rate.',
        'Recent games, ten to a page — open Decks to see both players’ lists, Watch the replay, or Share its link.',
        'Spoils — the cards you’ve won in games played for ante, and a count of the ones you lost.',
        'Recent tournaments, each opening its final standings and replays.',
        'Shortcuts to Full stats, My decks, Friends and Preferences.',
      ] },
      { kind: 'p', text: 'Click your avatar to choose one. Portraits offers 56 painted characters — the creature types of each colour, from Kithkin and Loxodon to Phyrexians and Eldrazi. Card art lets you search for any card, pick the printing whose art you like, and drag and zoom the circle to frame the part you want. Your avatar shows in the account menu, on your public profile, to friends and in messages, on the match intro, and as your portrait at the table, with your life total beneath it. At a table of three or more it leads your name on the player rail and board labels, and anyone spectating or watching the replay sees it beside your name too. “Use my initial” goes back to the letter. Avatars need an account; guests show their initial, and AI opponents sit behind a portrait of their own.' },
      { kind: 'p', text: 'Everyone also has a public profile at `/u/<id>` that anyone can open without signing in. It shows the same statistics and recent games but never your decklists. “This is how others see your profile” marks your own.' },
    ],
    related: ['stats', 'friends', 'guest-vs-account', 'playing-for-ante'],
  },
  {
    id: 'stats',
    section: 'community',
    title: 'Stats',
    summary:
      'The Stats page is a dashboard of how you play: record and win rate, ranked ratings, the colours, card types, curve, creature types, modes and sets you play most, head-to-heads and your most-played cards.',
    body: [
      { kind: 'p', text: 'Head to head lists your most-played human opponents (AI games are left out) with your record against each, linked to their profiles. Most-played cards preview on hover. The tournament panel lists the events you played.' },
      { kind: 'p', text: 'Stats need an account; games played as a guest are not counted.' },
    ],
    related: ['ranked', 'profile'],
  },
  {
    id: 'friends',
    section: 'community',
    title: 'Friends',
    summary:
      'Add friends by friend code to see when they are online. Your code is on the Friends page — it is your account id, not your email — and adding someone sends them a request to accept.',
    body: [
      { kind: 'ul', items: [
        'Add a friend — paste their code and Send request, or use + Add friend on their public profile.',
        'Requests — accept or decline incoming ones; cancel ones you sent. A red dot on the account menu means a request is waiting.',
        'Your friends — online or offline, Message, a link to their profile, and Unfriend.',
        'Hide my online status — friends will always see you as offline.',
        'Blocked — players you blocked from a result screen or a conversation, with Unblock.',
      ] },
      { kind: 'p', text: 'After a game against someone, you can also add them from the result screen.' },
      { kind: 'p', text: 'To play a friend, send them your lobby’s invite code or link; there is no in-app invite.' },
    ],
    related: ['messages', 'roster-friend', 'profile', 'invite-codes', 'after-the-game'],
  },
  {
    id: 'messages',
    section: 'community',
    title: 'Messages',
    summary:
      'Send private messages to other signed-in players — from Messages in the account menu, or from the chat button beside the fullscreen button on every screen, even mid-game. Friends can message each other freely; a message from anyone else arrives as a request you accept, delete or block.',
    body: [
      { kind: 'ul', items: [
        'Start a conversation — the pencil (New message) in Messages or in the chat panel lists all your friends, including ones you haven’t written to yet. Message on a friend’s row on the Friends page, or on any player’s profile, works too.',
        'Requests — a message from someone who isn’t your friend waits under the Requests tab. Accept it to reply (replying also accepts it), Delete it, or Block them. They aren’t told which you chose.',
        'Writing to someone who isn’t your friend — your first messages arrive as a request, and you can send three until they accept.',
        'Delete conversation — removes it for you only; if either of you writes again it starts fresh.',
        'Block — they can’t message you, you won’t be matched against each other, and their emotes stop reaching you. It also ends a friendship. Unblock from the Friends page.',
      ] },
      { kind: 'h', text: 'The chat button' },
      { kind: 'p', text: 'Away from the Messages page — in a game, the deckbuilder, a lobby — the speech-bubble button beside the fullscreen button opens a small chat panel under it. In a game it sits in the top-left row, after the preferences gear. The panel stays open while you keep playing, follows you from page to page, and closes with ×, Esc or the button. Typing in it never triggers game shortcuts.' },
      { kind: 'p', text: 'A new message puts an amber count on the button and drops a short preview under it; click the preview to answer. Don’t want that during a game? Turn off the bell in the chat panel (or Message alerts away from home in Preferences): away from the home screen the button then stays quiet, and your unread messages wait for you on the home screen.' },
      { kind: 'p', text: 'The number on the chat button counts unread messages and new requests; the Messages item in the account menu shows it too. Messages need an account — guests can’t send or receive them.' },
    ],
    related: ['friends', 'preferences', 'after-the-game', 'guest-vs-account'],
  },
  {
    id: 'after-the-game',
    section: 'community',
    title: 'Rematch, add friend, block',
    summary:
      'After a one-on-one game against another person, the result screen shows your opponent with Rematch, Add friend and Block.',
    body: [
      { kind: 'ul', items: [
        'Rematch — when you both ask, a new game starts right away with the same decks and settings. If they asked first, the button reads Accept rematch. Leaving the result screen takes the offer back.',
        'Add friend — sends them a friend request, or accepts theirs. Both players need to be signed in.',
        'Block — you will never be matched with them again, you stop seeing their emotes, and neither of you can send the other a rematch or friend request. They are not told; to them it looks like you left. Undo it from the same screen, or later from the Friends page.',
      ] },
      { kind: 'p', text: 'A guest can block too; the block lasts until the server restarts.' },
    ],
    related: ['matchmaking', 'friends', 'messages', 'emotes'],
  },
  {
    id: 'axis-limits',
    section: 'modes',
    title: 'Combinations that aren’t available yet',
    summary:
      'Not every point in the Cards × Rules × Table × Event space is wired up. Options that can’t be picked are shown disabled with the reason attached, rather than hidden.',
    body: [
      { kind: 'p', text: 'Everything below is a gap in the plumbing, not a rules decision — an option you can see and can’t use tells you the shape of the system; one that isn’t rendered just looks like nobody thought of it.' },
      { kind: 'ul', items: [
        'Bracket play is 1v1 only. Every multiplayer table plays exactly one shared game.',
        'A limited pool always runs as a bracket. Sealed and draft build a pool that is meant to be played more than once; with two players and one game per matchup, that is a single game anyway.',
        'Ranked is 1v1 only. Multiplayer tables are always casual.',
        'The AI cannot build a Commander deck from a limited pool yet. When everyone brings a deck, the host can pick a Commander deck for each AI seat.',
        'Commander rules cannot be played as Two-Headed Giant: a 2HG team shares one life total, and Commander gives every player their own 40. A 1v1 bracket, a Free-for-All pod and Team vs. Team all work.',
        'Momir Basic and a rolled random pool are 1v1 single games only. Neither exists at a multiplayer table or in a bracket.',
      ] },
    ],
    related: ['axes', 'ranked', 'lobby-switching'],
  },
  {
    id: 'lobby-switching',
    section: 'modes',
    title: 'Changing an axis can start a new lobby',
    summary:
      'Some axis values live on a different kind of lobby. Picking one (they are marked ⇄) opens a fresh lobby, so your invite code changes and anyone who has joined is dropped. You are asked first.',
    body: [
      { kind: 'p', text: 'Behind the scenes there are two lobby implementations: a small one for a single 1v1 game, and a larger one for limited pools, multiplayer tables and brackets. The lobby screen is the same either way, but a value only the other one can express means starting over on that one.' },
      { kind: 'ul', items: [
        'Marked ⇄ — selectable, but opens a new lobby. You get a confirmation listing exactly what is lost.',
        'Greyed out — nothing implements the combination yet. Hover for the reason.',
        'It is cheapest to change these before you share the invite code, which is the usual case.',
      ] },
    ],
    related: ['axes', 'axis-limits', 'invite-codes'],
  },
  {
    id: 'lobby-screen',
    section: 'modes',
    title: 'The lobby',
    summary:
      'Every multiplayer game, draft and bracket is set up in a lobby: the player list with deck status, five settings groups (Cards, Rules, Table, Event, This lobby), and an action bar that says exactly what is still blocking the start.',
    body: [
      { kind: 'h', text: 'Players' },
      { kind: 'p', text: 'Each row shows You or Host, and the deck status — “Choose deck ✎” or “✓ Deck ready · Change”. The host can add AI seats and remove them. The invite code, a copy button and a QR code sit at the top (not for AI-only games).' },
      { kind: 'h', text: 'Choosing your deck' },
      { kind: 'ul', items: [
        'My decks — your saved decks, from this browser or your account.',
        'Examples — ready-made starter decks served by the server.',
        'Paste — a decklist in `4 Lightning Bolt` form; a `Sideboard` header or `SB:` starts the sideboard.',
        'Random — the server builds you a deck, optionally from sets you choose.',
      ] },
      { kind: 'p', text: 'The deck is validated as you pick it, against the lobby’s legality restriction if there is one.' },
      { kind: 'h', text: 'Settings the host can change' },
      { kind: 'table', head: ['Setting', 'Values'], rows: [
        ['Card source', 'Sets, or a cube'],
        ['Packs', '1–6 packs per player for drafts, 1–16 boosters per player for sealed'],
        ['Pick timer', '30, 45, 60, 90 or 120 seconds, or no limit (Winston drafts call it the turn timer)'],
        ['Cards per pick', '1 or 2 (Booster and Commander drafts)'],
        ['Commander preset', 'Brawl (25 life / 16 commander damage), Commander (30/21), Pod (40/21)'],
        ['Minimum deck size', '40, 50, 60, 75 or 100; singleton on or off'],
        ['Teams', 'Random, or Choose teams (click a team chip)'],
        ['Free-for-All attacks', 'Left only, Right only, or Any opponent'],
        ['Games per matchup', '1–5 (brackets)'],
        ['Ranked', 'Casual or Ranked (1v1 only)'],
        ['Visibility', 'Private or Public'],
        ['AI assistance', 'Off or On — enables Suggest Pick and Auto-build'],
        ['Ban list', 'Cards nobody may play; save and reload named lists per set'],
      ] },
      { kind: 'p', text: 'Every group has a `?` for its help topic and a “!” when it holds the reason Start is disabled. The action bar writes that reason out; the primary button is Ready, Start Game, Start Draft or Start Tournament depending on the lobby.' },
      { kind: 'p', text: 'The lobby has no chat and no kick button — players leave themselves with Leave. Use your usual voice or chat app alongside.' },
    ],
    related: ['axes', 'lobby-switching', 'saved-setups', 'roster-solo', 'invite-codes', 'playing-for-ante'],
  },
  {
    id: 'saved-setups',
    section: 'modes',
    title: 'Saved setups: Jump back in',
    summary:
      'The host’s ★ Save setup button names the current lobby configuration. It reappears on the home screen under “Jump back in”, where one click rebuilds the whole lobby — sets, packs, timer, ban list, cube and deck.',
    body: [
      { kind: 'p', text: 'The rail also keeps an automatic “↺” chip for the last thing you played, so repeating it never needs a save. The ⋯ menu on a chip renames or deletes it; on the automatic chip, “Keep and name…” turns it into a permanent setup.' },
      { kind: 'p', text: 'A chip may note that some settings cannot be restored — usually because something it named, like a cube or a deck, no longer exists. Everything else is restored as saved.' },
    ],
    related: ['lobby-screen', 'home-screen'],
  },
  {
    id: 'roster-solo',
    section: 'modes',
    title: 'Playing the AI',
    summary:
      'Choose “AI” under Play with and you are seated against a built-in AI opponent straight away — no lobby, nobody else has to show up. It plays every mode, builds from limited pools, and fills multiplayer seats.',
    body: [
      { kind: 'p', text: 'How many AI opponents each mode starts with (the launch panel lets you change it where there is a range):' },
      { kind: 'table', head: ['Mode', 'AI opponents'], rows: [
        ['Constructed, Jump In', '1–7 (default 1)'],
        ['Sealed, Booster or Commander draft', '1–7 (default 3)'],
        ['Grid draft', '1–3'],
        ['Winston draft', '1'],
        ['Free-for-All', '2–5 (default 3)'],
        ['Team vs. Team', '3, 5 or 7'],
        ['Two-Headed Giant', '3'],
        ['Commander', '1–5 (default 3)'],
        ['Random deck, Momir Basic', '1'],
      ] },
      { kind: 'p', text: 'In a lobby, + Add AI adds a seat and the host removes one with ×. Each AI seat has its own deck chooser: Auto (the server rolls a deck, or the AI builds from its pool), From sets, or Pick a deck — any saved, example or pasted deck. Commander needs a picked Commander deck, so the AI’s commander can start in the command zone.' },
      { kind: 'p', text: 'Want a different timer, pack count or deck size, or AI seats with chosen decks? Under the launch panel’s Play button, “Open a lobby to change them” opens the same game as a lobby instead of starting it. Change what you like, then press Start.' },
      { kind: 'p', text: 'On Auto, the AI matches the deck you bring. Play a starter deck or your own list and the AI plays from the same set or sets: another starter deck from them when there is one — Boros Mice meets one of the other Bloomburrow tribes — otherwise a deck built from those sets, 60 cards against a 60-card deck and a sealed pool against a 40-card one. Bring a constructed deck drawn from many sets — a Standard deck, say — and the AI builds a fresh deck for the format yours was built for, so a Standard deck meets a different Standard deck each game. That is the format your saved deck or starter deck names (a Highlander deck meets a Highlander deck); a pasted list is matched to the narrowest of Standard, Pioneer, Modern, Legacy and Vintage it is legal in. The AI seat’s row in the lobby names the sets or the format it will use. Choose From sets or Pick a deck to decide yourself.' },
      { kind: 'p', text: 'There is a single AI strength; it is not adjustable. In a draft or sealed event the AI drafts and builds its own deck from what it is dealt.' },
      { kind: 'tip', text: 'Against the AI, the game-over screen offers Play Again, which rebuilds the same game in one click.' },
    ],
    related: ['axis-limits', 'cards-draft', 'event-round-robin', 'learn-to-play'],
  },
  {
    id: 'roster-friend',
    section: 'modes',
    title: 'Playing friends',
    summary:
      'Choose “Friends” under Play with and you get a private lobby with an invite code, a link and a QR code to share. When they join, everyone picks a deck and readies up.',
    body: [
      { kind: 'p', text: 'There is no in-app invite — your friends join with the code or the link, from any device. Friends lists show who is online, but joining still goes through the code.' },
      { kind: 'p', text: 'A friend lobby can also hold AI seats, so two humans can fill out a four-player table or a draft pod.' },
    ],
    related: ['invite-codes', 'ranked', 'event-single-game', 'friends'],
  },
  {
    id: 'roster-group',
    section: 'modes',
    title: 'Groups and public lobbies',
    summary:
      'Three to eight players: either one shared game — Free-for-All, Two-Headed Giant or Team vs. Team — or a round-robin bracket of 1v1 matches with standings. Choose “Anyone” to list the lobby publicly so strangers can join.',
    body: [
      { kind: 'p', text: 'Everyone joins with the same invite code, or from Open lobbies on the home screen when the lobby’s visibility is Public. A lobby holds as many seats as its table allows; the host starts once everyone has arrived.' },
      { kind: 'p', text: 'A group can bring their own decks or share a limited pool: sealed and draft both work at a multiplayer table as well as in a bracket.' },
    ],
    related: ['table-free-for-all', 'table-two-headed-giant', 'table-team-vs-team', 'event-round-robin', 'lobby-screen'],
  },

  // ── Draft & Sealed ─────────────────────────────────────────────────────
  {
    id: 'set-guide',
    section: 'limited',
    title: 'Choosing a set: the Set guide',
    summary:
      'Set guide, in the home screen’s top bar, lists every set that is fully playable for sealed and draft, with how its games play, what wins them, and power, speed and complexity ratings to compare them by. The play screen links to it under its set choice.',
    body: [
      { kind: 'p', text: 'Each set has a short summary on its tile — how a game of it feels and what decides it — a “Try it if you like” line, three five-step meters and its theme tags. The ratings compare the sets here with each other as Limited formats, not with Magic as a whole: Power is how strong the cards are, Speed how quickly games tend to end, Complexity how much there is to keep track of.' },
      { kind: 'ul', items: [
        'Quick filters answer the usual questions — Easy to learn, Fast & aggressive, Slow & grindy, High power, Before 2008 — and theme chips narrow it to sets built around creature types, graveyards, spells, artifacts and so on. Search also matches mechanics and tips, so `foretell` finds Kaldheim. Hover a theme tag or mechanic to see what it means for a game.',
        'Click a set to read about it up close: how its games play, three tips for drafting and playing it, what each of its mechanics does to a game, its draft archetypes, and a line on its world.',
        '+ Compare on up to four sets, then Compare, puts them side by side. The address bar keeps the comparison, so it can be shared.',
        'Map lays every set out by speed and complexity, coloured by power, for a feel of the whole catalogue at once.',
      ] },
      { kind: 'tip', text: 'The lobby’s set picker shows the same pitch and ratings for whichever set you point at, so you can decide without leaving the lobby.' },
      { kind: 'p', text: 'Sets that are only partly implemented are not in the guide; Implementation progress, linked from its header, shows how far each one has come.' },
    ],
    related: ['set-completion', 'draft-archetypes', 'draft-formats'],
  },
  {
    id: 'draft-picking',
    section: 'limited',
    title: 'The draft screen',
    summary:
      'Each pick shows the pack in front of you, the pick timer, the pass direction and your pool so far. Click a card, then press “Pick <card>”. When the timer runs out a card is picked for you.',
    body: [
      { kind: 'ul', items: [
        'The header shows the pack and pick number, an arrow for which way packs pass, the timer, and how many cards you have.',
        'The timer turns red in its last ten seconds; ∞ means the lobby has no time limit.',
        'With two cards per pick, choose both and press “Select 2 cards”.',
        'The Card Pool sidebar counts creatures and spells, shows your colours and your top creature types. On a phone the pack and the pool are two tabs.',
        'Between packs you see “Waiting for next pack…” while slower drafters finish.',
      ] },
      { kind: 'p', text: 'Hover any card for the full-size preview. The host can stop the draft; anyone else can Leave.' },
    ],
    related: ['cards-draft', 'draft-formats', 'draft-assist', 'draft-archetypes'],
  },
  {
    id: 'draft-formats',
    section: 'limited',
    title: 'Booster, Winston, Grid and Commander drafts',
    summary:
      'Four ways to draft. Booster passes packs round the table; Winston is a two-player pile draft; Grid takes a row or column from a 3×3 grid; Commander drafts Commander-shaped packs into a 60-card commander deck.',
    body: [
      { kind: 'table', head: ['Format', 'Players', 'How a pick works'], rows: [
        ['Booster', '3–8', 'Take one card (or two) and pass the rest. Pack direction alternates.'],
        ['Winston', '2', 'Look at face-down piles in order; Take Pile N, or Skip to the next one. Skipping the last pile takes a blind card from the deck.'],
        ['Grid', '2–4', 'Nine cards in a 3×3 grid; take a whole row or a whole column.'],
        ['Commander', 'up to 8', 'Like Booster, with Commander-Legends-shaped 20-card packs holding a legend each.'],
      ] },
      { kind: 'p', text: 'In Grid you can also look at what your opponent has taken. Winston’s timer is per turn rather than per pick.' },
      { kind: 'p', text: 'AI drafters can fill empty seats in every format.' },
    ],
    related: ['cards-draft', 'draft-picking', 'rules-commander'],
  },
  {
    id: 'draft-assist',
    section: 'limited',
    title: 'AI pick suggestions and Auto-build',
    summary:
      'With the lobby’s AI assistance set to On, the draft screen gets a ✨ Suggest Pick button and the deck builder gets an AI group in its top bar: Auto-build and Card scores. It is a learning aid; the host decides whether it is allowed.',
    body: [
      { kind: 'ul', items: [
        'Suggest Pick scores every card in the pack from 0 to 100 — green at 70 and up, amber from 45 — with a one-line reason on hover, and marks the top one “⭐ Best”. Re-suggest asks again; Clear hides the scores.',
        'Auto-build proposes the best deck it can make from your pool, lands included. If you have already started, it becomes Complete deck and keeps your picks while it fills the gaps.',
        'When the engine finds several decks, the strip under the top bar lists them as Builds — click one to switch. Undo puts back the deck you had before, as long as you haven’t edited it since.',
        'Card scores rates every card in your pool, and in your deck list, so you can make the cuts yourself. Scores use your deck so far for colour context; when the deck changes, the strip offers Re-score. Click Card scores again to hide them.',
        'With more than one engine on the server, the picker at the start of the AI group chooses which one Auto-build and Card scores use.',
      ] },
      { kind: 'tip', text: 'Assistance is off by default, so nobody in a competitive pod gets it unless the host turns it on for everyone. A draft or sealed game against the AI starts right away, so you choose it on the launch panel instead, under AI assistance.' },
    ],
    related: ['draft-picking', 'limited-deckbuilding'],
  },
  {
    id: 'draft-archetypes',
    section: 'limited',
    title: 'Archetypes',
    summary:
      'The Archetypes button opens a format guide for the set you are playing: each archetype’s colours, plan and key cards — and, once you have a pool, how many of your cards fit each one.',
    body: [
      { kind: 'p', text: 'With a pool open, each archetype tile counts your on-colour mythics, rares, uncommons and commons, colourless cards and lands, and the creature types that matter to it. In the deck builder, clicking an archetype filters your pool to its colours and highlights its creature types.' },
      { kind: 'p', text: 'Archetype guides are written per set, so not every set has one; the button only appears where one exists.' },
    ],
    related: ['draft-picking', 'limited-deckbuilding'],
  },
  {
    id: 'limited-deckbuilding',
    section: 'limited',
    title: 'Building from a sealed or drafted pool',
    summary:
      'After a draft or sealed opening you get a dedicated builder over just your pool: add cards, set basic-land counts, and submit. Limited decks need at least 40 cards including lands; Commander-shaped pools use the lobby’s minimum instead.',
    body: [
      { kind: 'h', text: 'The builder' },
      { kind: 'ul', items: [
        'Click a pool card to add it to the deck; click a deck card to send it back.',
        'Sort by colour, mana value or rarity. Filter by colour (Includes, Exactly or At most), type and Legendary, or type a query in the Scryfall-style search box.',
        'Your Deck shows the spell and land counts, the mana curve, colour pips against your mana sources, and your top creature types.',
        'Basic Lands has + / − per type and a Suggest button that fills the land count from your colours.',
        'In a Commander pool, the crown on a legendary creature (or a card that says it can be your commander) makes it your commander, and the ♛ Identity toggle restricts the pool to its colour identity. With one commander crowned, crowning a legal partner — another Partner card, a Background for a “Choose a Background” commander, and so on — adds it as a second commander; the identity is then both commanders’ combined.',
      ] },
      { kind: 'p', text: 'You can play every copy of a card you opened or drafted — five Shocks in your pool means up to five Shocks in your deck. The host can turn on Copy limit → Max 4 under Rules in the lobby to cap every card except basic lands at 4 copies, as in constructed. Cube Pool Play always caps at 4.' },
      { kind: 'p', text: 'Submit Deck stays disabled until the deck is big enough, and says how many cards it still needs. After submitting you can still press Edit Deck until the first round begins. Export gives an MTG Arena-format list to copy or download.' },
      { kind: 'p', text: 'The cards you leave out become your sideboard — the “outside the game” cards that wish-style effects can fetch. There is no sideboarding between games: the deck you submit plays every round.' },
      { kind: 'p', text: 'Save Deck on the standings screen keeps the deck in My Decks, with the exact printings you opened or drafted.' },
    ],
    related: ['cards-sealed', 'cards-draft', 'draft-assist', 'cube-pool-play', 'outside-the-game'],
  },
  {
    id: 'tournament-flow',
    section: 'limited',
    title: 'Playing the bracket: standings and rounds',
    summary:
      'Between matches you are on the standings screen: your next opponent, a Ready button, the live table, and buttons to share the event, watch replays, and view or save your deck.',
    body: [
      { kind: 'ol', items: [
        'Everyone submits a deck. Before round 1 you can still Edit Deck.',
        'The screen shows “Next Match: vs X” — or “Sitting out this round” on a bye.',
        'Press Ready for Next Round. The count shows how many are ready; the round starts when everyone is.',
        'Play your match. Matches still running elsewhere are listed live, with both life totals and a ▶ Watch button.',
        'Repeat until everyone has played everyone. The host can Add Round once the bracket is complete.',
      ] },
      { kind: 'p', text: 'Standings show record, points and game win rate; hover a row for the tiebreakers (opponents’ match win %, game win %, opponents’ game win %, life differential) and which one actually separated two players.' },
      { kind: 'p', text: 'Leave Tournament (confirm with a second click) drops you from the event. If a player disconnects, everyone sees a countdown banner; others can extend it by a minute, and kick them once two minutes have passed.' },
      { kind: 'p', text: 'Anyone can follow the event from its tournament link, and every finished match’s replay is under Replays.' },
    ],
    related: ['event-round-robin', 'limited-deckbuilding', 'spectating', 'replays', 'disconnects', 'playing-for-ante'],
  },
  {
    id: 'cube-building',
    section: 'limited',
    title: 'Building a cube',
    summary:
      'Cubes are built in the deckbuilder: flip the Deck builder | Cube builder switch at the top, or open a cube from the Cubes tab of Decks & Cubes. Flipping back and forth keeps what you were working on in each, saved or not, for the rest of the session. Browse and filter cards exactly as for a deck, add a whole set or paste a list, choose how the cube becomes packs, and save it. The lobby’s Cube panel has a compact editor for quick tweaks. Cubes live alongside your decks — in this browser as a guest, in your account when signed in.',
    body: [
      { kind: 'p', text: 'A cube is a list of card names with counts, so it isn’t tied to any set or printing. The builder keeps the deckbuilder’s card grid and filters; what changes in cube mode is the right-hand panel — no sideboard, format or 4-copy limit, but packs, rarities and pack health instead. Three ways to fill a cube:' },
      { kind: 'ul', items: [
        'Add a set — every implemented card of a set at once, each at the rarity that set printed it at. Toggles choose its booster cards, cards from its other products, and whether to keep reprints.',
        'Paste a list — plain text, MTG Arena or Moxfield format, one “count name” per line. The same parser the deckbuilder’s import uses, and “Copy list” writes the same format back out.',
        'Browse — the deckbuilder’s grid, filters and query language (“c:red t:creature cmc<=3”). Click a card to add it, right-click to take one out.',
      ] },
      { kind: 'p', text: 'Packs decides how the cube is played: a shuffled cube is dealt card by card, while Play Boosters and Draft Boosters print packs by rarity like a real set. The list filters by name and groups by type, colour or rarity, so a set-sized cube stays easy to work through.' },
      { kind: 'p', text: 'Like the deckbuilder’s Deck list, the tabs under the title switch between “Search cards” (the card grid, with the cube in the right-hand panel) and “Cube list”: the whole cube in wide columns with the hovered card shown large on the left. In the Cube list you add cards by typing a name in the bar above the list, and change counts and rarities right in the columns.' },
      { kind: 'p', text: 'Cubes are usually singleton (one of each), but counts are yours to set — the editor lets any card run as many copies as you want, and each copy is a separate physical card when packs are dealt.' },
      { kind: 'p', text: 'A cube whose names are all implemented is playable; one with cards the engine doesn’t have yet is not, and the editor says so in red with a one-click “drop the unimplemented cards”. You can still save a cube in that state — sets keep landing, so a cube naming a card that arrives next month is worth keeping — but a lobby won’t accept it until it resolves cleanly.' },
      { kind: 'p', text: 'The editor also shows the colour spread, mana curve and rarity spread as you go, which are what a cube is usually balanced on.' },
      { kind: 'p', text: 'Each cube on the Cubes tab of Decks & Cubes has Play, which opens a lobby with the cube already loaded — a booster draft or sealed, with friends or against AI.' },
    ],
    related: ['cards-cube', 'cube-boosters', 'cube-sharing', 'cube-pool-play', 'where-decks-live', 'search-syntax', 'deck-import-export'],
  },
  {
    id: 'cube-sharing',
    section: 'limited',
    title: 'Sharing a cube',
    summary:
      'Share in the cube editor or on a cube’s tile creates a link anyone can open to view the cube, save a copy and play it. Links need an account; anyone can copy a cube as a text list.',
    body: [
      { kind: 'p', text: 'A share link always shows your latest saved version, so a friend who opens it next week sees your edits. Turn sharing off from the same dialog and the link stops working; turning it on again makes a new link.' },
      { kind: 'p', text: 'Someone opening the link can save a copy into their own cubes — no account needed, it goes into their browser — and their copy is theirs from then on: it doesn’t change when yours does.' },
      { kind: 'p', text: 'Links need the cube saved in your account, because the link has to open on someone else’s machine. As a guest, “Copy list” gives you the cube as text that anyone can paste into their own cube editor.' },
    ],
    related: ['cube-building', 'cards-cube', 'cube-boosters', 'where-decks-live'],
  },

  // ── Playing a game ─────────────────────────────────────────────────────
  {
    id: 'mulligan',
    section: 'playing',
    title: 'Opening hand and mulligans',
    summary:
      'Every game opens on your seven-card hand with Keep Hand and Mulligan. Mulligans are London-style: you always draw seven, and after keeping you choose which cards go to the bottom.',
    body: [
      { kind: 'p', text: 'The badge says whether you are on the play or on the draw. After a mulligan the screen tells you how many cards you will bottom; once you keep, click that many cards and press Confirm. View Battlefield hides the screen so you can look at the table first.' },
      { kind: 'p', text: 'In a bracket the round number and both players’ records are shown above your hand. While your opponent decides, you see “Waiting for your opponent”.' },
    ],
    related: ['game-table', 'pass-button', 'subgames'],
  },
  {
    id: 'game-table',
    section: 'playing',
    title: 'Reading the table',
    summary:
      'Your board is at the bottom, opponents at the top, the stack in the middle. Each player has a life orb, their zone piles, and badges for anything that changes the usual rules — poison, energy, radiation, experience, hand size, commander damage, day or night.',
    body: [
      { kind: 'h', text: 'Around each life total' },
      { kind: 'ul', items: [
        'POISON n/10 — appears once a player has poison counters. A Two-Headed Giant team loses at 15.',
        '⚡ n — energy counters.',
        '☢ n RAD — your radiation counters. At your precombat main phase, a Radiation ability appears on the stack; you can respond to it before it mills cards and removes counters.',
        'XP n — experience counters, which some commanders give you and then count.',
        'HAND — the maximum hand size, only when it is not 7.',
        '⚔ commander damage — taken from each commander, against the 21 (or preset) threshold; it turns red within 5 of lethal.',
        'Chips for active effects on that player — hover one to see what it does. The City’s Blessing, the Ring’s temptation and Speed have their own badges. An emblem with an ability you can activate right now glows green; click it to activate.',
        '👑 The Monarch — the crown chip marks the monarch. They draw an extra card at the beginning of their end step, and a creature that deals them combat damage takes the crown for its controller.',
        '🗝️ The Initiative — marks the player who has the initiative. It sits beside the crown when one player holds both; see “Dungeons and the initiative”.',
      ] },
      { kind: 'h', text: 'Elsewhere' },
      { kind: 'ul', items: [
        'The phase bar across the top, with the turn number and whose turn it is.',
        'Day / Night — a ☀ or ☾ badge beside it once a day/night card has started the cycle.',
        'Command — the command zone, holding your commander; its cost badge already includes commander tax. A dungeon you are venturing through shows there too.',
        'Deck, Graveyard and Exile piles, plus Plotted, Suspended and similar piles when a card uses one.',
        'Stack (N) — what is waiting to resolve, newest on top. Identical items can be collapsed.',
        'An eye badge, top left, counts spectators; hover it for their names.',
      ] },
      { kind: 'p', text: 'Opponent decisions are shown as they happen — “<name> is choosing…”, with the card that asked.' },
    ],
    related: ['phase-bar', 'card-badges', 'zone-browsers', 'card-preview', 'multiplayer-camera', 'dungeons-initiative', 'subgames'],
  },
  {
    id: 'subgames',
    section: 'playing',
    title: 'Subgames (Shahrazad)',
    summary:
      'Shahrazad makes the players play a whole separate game of Magic, with their libraries as their decks. The table switches to that game, framed in gold with a SUBGAME badge, until it is over — then the main game picks up where it stopped.',
    body: [
      { kind: 'p', text: 'When the subgame starts, a short card announces it and who goes first (chosen at random). You then see your opening hand for the subgame and can mulligan as usual. Everything on the table — life totals, battlefield, graveyard — is the subgame’s; nothing from the main game is there, and nothing that happens in the subgame touches the main game.' },
      { kind: 'p', text: 'Hover or tap the SUBGAME badge beside the phase bar to see the card that started it, what it does when the subgame ends, and each player’s life total in the main game that is waiting.' },
      { kind: 'p', text: 'When the subgame ends, every card in it goes back into its owner’s library in the main game, which is shuffled, and the spell finishes resolving — for Shahrazad, everyone who didn’t win loses half their life, rounded up.' },
      { kind: 'tip', text: 'Concede offers two choices during a subgame: Concede subgame gives up just this game (the main game resumes), while Concede the game leaves the whole table.' },
    ],
    related: ['game-table', 'mulligan'],
  },
  {
    id: 'dungeons-initiative',
    section: 'playing',
    title: 'Dungeons and the initiative',
    summary:
      'Cards that venture into the dungeon put a dungeon in your command zone and move you room by room; each room you enter does its effect. The initiative ventures you into Undercity, and the 🗝️ badge shows who has it.',
    body: [
      { kind: 'h', text: 'Venturing' },
      { kind: 'ul', items: [
        'The first time you venture, a picker shows the dungeon cards — Lost Mine of Phandelver, Dungeon of the Mad Mage and Tomb of Annihilation. Click one, then press Venture.',
        'Each later venture moves your marker one room down. Where a room has two exits, the dungeon map opens with the rooms you can reach lit up; click one to move there.',
        'The room’s effect goes on the stack like any triggered ability, choosing targets if it has any.',
        'When the last room’s effect has resolved, the dungeon is completed and leaves your command zone. Your next venture starts a new one — you may pick the same dungeon again.',
      ] },
      { kind: 'h', text: 'Where to look' },
      { kind: 'ul', items: [
        'The Dungeon tile in each player’s command-zone column names the dungeon and the room the 🧭 marker is on, with a bar for how deep they are. Click it to see the whole map.',
        'Opponents’ rail chips show 🧭 and their current room, plus 👑 and 🗝️ for the monarch and the initiative.',
        'The tile keeps a count of the dungeons that player has completed — what “if you’ve completed a dungeon” cards check.',
        'The game log has a line for every venture, every completed dungeon and every time the initiative changes hands.',
      ] },
      { kind: 'h', text: 'The initiative' },
      { kind: 'p', text: 'Taking the initiative ventures you into Undercity, and so does each of your upkeeps while you keep it. Deal combat damage to the player who has it and you take it — and venture. Undercity can only be entered this way: an ordinary venture never offers it. If you are already in another dungeon, the initiative moves you along that one instead.' },
      { kind: 'tip', text: 'The initiative and the monarch are separate: one player can hold both, or each can sit with a different player.' },
    ],
    related: ['game-table', 'card-badges'],
  },
  {
    id: 'dexterity-flips',
    section: 'playing',
    title: 'Flipping cards (Chaos Orb, Falling Star)',
    summary:
      'Cards that tell you to flip them onto the table — Chaos Orb, Falling Star — are played for real: the battlefield is laid out as a card table, you aim, press and hold to swing, and let go with the needle on the gold line. Your aim and your timing decide what it lands on.',
    body: [
      { kind: 'p', text: 'When Chaos Orb’s ability or a Falling Star spell resolves, a table opens with every permanent lying where it would on a paper battlefield — creatures in front, lands at the back, tapped cards sideways, Auras and Equipment peeking out from behind what they’re attached to. Your side is at the bottom; the card sits in your hand at the edge, and the card’s text is shown above the table.' },
      { kind: 'ol', items: [
        'Aim — move over the table. The crosshair is your aim and the dashed card shows where a perfect release lands; anything it would affect lights up red, your own permanents amber, and what it can’t affect grey (Chaos Orb spares tokens; Falling Star only hits creatures). A clean flip lands within a few millimetres of your aim, so aim carefully.',
        'Swing — press and hold. The swing starts the moment you press, so aim first. The needle swings up and down the gauge, and the dashed card slides with it: below the gold line the card falls short, back towards your own permanents; above it, it sails long. Your hand also sways left and right — the dot under the card — pulling the flip to that side, so let go with it centred. Every swing is quicker than the last, and hold too long and your hand starts to shake.',
        'Let go — release with the needle on the gold line to land on your aim. Let go far too low and the card won’t turn over completely, which means it does nothing.',
      ] },
      { kind: 'p', text: 'The server decides where it lands from your aim and release, so every player sees the same flight. If it turned over, it affects everything it came to rest on, yours included: Chaos Orb destroys each nontoken permanent it touches and then itself; Falling Star deals 3 damage to each creature it lands on and taps them. Your opponent watches the same flip from their side of the table.' },
      { kind: 'tip', text: 'Aim at the seam between two cards to take both — a precise aim and a clean release can. Your first swing is the slowest, so it’s usually the one to take. Space works as the swing button too, and Esc stops a swing without throwing.' },
    ],
    related: ['decisions', 'game-table'],
  },
  {
    id: 'emotes',
    section: 'playing',
    title: 'Emotes',
    summary:
      'The speech-bubble button beside your life total (beside the turn tracker at a multiplayer table) sends the table a quick message — a greeting, a reaction, or a bit of table talk.',
    body: [
      { kind: 'table', head: ['Group', 'Says'], rows: [
        ['Hello & goodbye', 'Hello! · Good luck, have fun! · Thanks! · Good game!'],
        ['Reactions', 'Well played. · Love this deck! · Ooh, nice combo! · Of course you drew that. · Didn’t see that coming! · Ouch.'],
        ['Table talk', 'Doing the math… · Oops. You saw nothing. · Land. Please. Any land. · Is that all you’ve got?'],
      ] },
      { kind: 'p', text: 'Messages appear as a bubble over the sender’s life total for a few seconds — at a multiplayer table, on their board’s name plate, or beside their chip in the player list while their board is off screen. There is no free typing, and a short cooldown keeps it friendly. The AI answers a greeting, and says thanks for a compliment.' },
      { kind: 'tip', text: 'Not in the mood? Switch off “Show their emotes” at the bottom of the emote menu to mute an opponent for the rest of the game. At a multiplayer table each switch carries that seat’s colour.' },
    ],
    related: ['game-table', 'after-the-game'],
  },
  {
    id: 'pass-button',
    section: 'playing',
    title: 'The pass button',
    summary:
      'The big button bottom right always says what pressing it will do: Resolve the top of the stack, Pass, End Turn, or skip ahead “To <Step>” — to the next step where something can happen.',
    body: [
      { kind: 'table', head: ['Label', 'Meaning'], rows: [
        ['Resolve', 'There is something on the stack; passing lets it resolve.'],
        ['Pass', 'It is someone else’s turn and you are giving priority back.'],
        ['To <Step>', 'Skip ahead to that step — Upkeep, Draw, Main 1, Combat, Attackers, Blockers, Damage, End Step and so on.'],
        ['To My Turn / To Opponent’s Turn', 'Nothing else will stop you before then.'],
        ['End Turn', 'Pass priority for the rest of your turn.'],
      ] },
      { kind: 'p', text: 'Its colour helps too: orange when it resolves the stack, blue on your own turn, amber when you are responding. Above it sit Undo, the Auto/Manual Tap toggle, the priority-mode button and ? Help, which opens this wiki in a drawer without leaving the game.' },
      { kind: 'p', text: 'During combat the button is replaced by combat controls: Attack All, Skip Attacking or Attack with N while declaring attackers; No Blocks or Confirm Blocks while declaring blockers.' },
    ],
    related: ['priority-modes', 'stops', 'targeting-and-combat', 'undo'],
  },
  {
    id: 'priority-modes',
    section: 'playing',
    title: 'Priority modes: Auto, Stops, Full Control',
    summary:
      'Auto passes for you whenever you have nothing worth doing. Stops pauses on opponent spells and abilities, on combat damage, and in a declare attackers step where nothing attacks. Full Control gives you priority at every single step.',
    body: [
      { kind: 'p', text: 'The button cycles Auto → Stops → Full Control. Auto is right for most games; switch to Full Control when you need a specific window, such as responding in your own upkeep. Full Control also lets you order your own simultaneous triggers. Games start in Auto unless you pick another starting mode in Preferences.' },
      { kind: 'p', text: 'Auto never passes when you have a decision that matters — it is a convenience, not a rules shortcut.' },
      { kind: 'p', text: 'In Auto and Stops your own spells and abilities pass straight to your opponent. To respond to one of your own — Overkill on the creature whose Jaws of Defeat trigger is waiting, say — switch that kind on under Hold priority over my own in Preferences, or play that turn in Full Control.' },
    ],
    related: ['stops', 'yields', 'preferences', 'own-stack-stops', 'trigger-order'],
  },
  {
    id: 'stops',
    section: 'playing',
    title: 'Stops on the phase bar',
    summary:
      'Hover a step on the phase bar to reveal two dots: a blue “my turn” stop and an amber “opponent turn” stop. Click one and you will always get priority at that step.',
    body: [
      { kind: 'p', text: 'Stops are part of your Preferences: they apply to every game you play, and follow you to other devices once you are signed in. Preferences also lists them all in one table, for both turns.' },
    ],
    related: ['priority-modes', 'phase-bar', 'preferences'],
  },
  {
    id: 'own-stack-stops',
    section: 'playing',
    title: 'Responding to your own spells and triggers',
    summary:
      'By default your own spells and abilities pass straight to your opponent. Preferences → Hold priority over my own gives you the window back, kind by kind, whenever you can respond.',
    body: [
      { kind: 'p', text: 'Combo turns often need it. Jaws of Defeat triggers when your creature enters; cast Overkill on that creature while the trigger waits and the opponent loses life equal to the gap between its power and its huge negative toughness.' },
      { kind: 'ul', items: [
        'My triggered abilities — stop when one of your triggers is on top of the stack.',
        'My activated abilities — stop after you activate an ability.',
        'My spells — stop after you cast a spell.',
      ] },
      { kind: 'p', text: 'A stop only happens when you have something to cast or activate in response, so turning these on costs no clicks the rest of the time. The setting applies to the game you are in as soon as you change it, including from the in-game gear.' },
    ],
    related: ['priority-modes', 'preferences', 'stops'],
  },
  {
    id: 'preferences',
    section: 'playing',
    title: 'Preferences',
    summary:
      'The gear on the home screen (or Preferences in the account menu) sets how your table behaves: the priority mode games start in, your standing stops, auto-tap, how identical permanents stack, and motion. In a game, the gear beside the fullscreen button opens the same settings over the board.',
    body: [
      { kind: 'h', text: 'Gameplay' },
      { kind: 'ul', items: [
        'Starting priority mode — Auto, Stops or Full Control. Every game begins in it; the mode button still switches it for the game you are in.',
        'Always stop at — your standing stops, for your turn and your opponents’. The phase-bar dots edit the same list.',
        'Hold priority over my own — triggered abilities, activated abilities, spells: get priority back while one of yours waits on the stack, whenever you can respond to it.',
        'Auto-tap mana, Order my triggers automatically, Choices with only one answer (answer instantly, answer for me after a short pause, or ask me every time), and Follow the action for games with three or more players.',
      ] },
      { kind: 'h', text: 'Battlefield' },
      { kind: 'p', text: 'Identical permanents — same name, tapped state, counters, damage and so on — collapse into one stack with a count. Lands, creatures and other permanents each have their own rule:' },
      { kind: 'table', head: ['Setting', 'What it does'], rows: [
        ['Stack from', 'The fewest identical permanents that form a stack. At 4, three Forests sit side by side and a fourth stacks them. Never stack keeps every card on its own.'],
        ['Stacks of', 'The most cards in one stack. At most 5 turns twelve Saprolings into stacks of 5, 5 and 2.'],
        ['Visible stack layers', 'How many cards a stack fans out before the rest hide behind its count.'],
      ] },
      { kind: 'h', text: 'Display' },
      { kind: 'p', text: 'Reduce motion cuts animations short, whatever your device is set to. Card preview on hover can be turned off if the full-size preview gets in your way; on a touch screen, press and hold still opens it.' },
      { kind: 'h', text: 'Messages' },
      { kind: 'p', text: 'Signed in, Message alerts away from home decides whether new messages put an unread count on the chat button and show a preview while you’re in a game, a draft or on another page. The home screen always shows them.' },
      { kind: 'tip', text: 'Changes save as you make them. As a guest they live in this browser; signed in, they follow you to every device.' },
    ],
    related: ['stops', 'priority-modes', 'own-stack-stops', 'auto-tap', 'trigger-order', 'forced-choices', 'card-preview', 'messages', 'guest-vs-account'],
  },
  {
    id: 'phase-bar',
    section: 'playing',
    title: 'The phase bar',
    summary:
      'The strip of pips across the top is the turn. The lit pip is the current step; the colour tells you whose turn it is.',
    related: ['stops'],
  },
  {
    id: 'auto-tap',
    section: 'playing',
    title: 'Auto Tap vs. Manual Tap',
    summary:
      'Auto Tap picks lands for you when you cast something. Manual Tap hands you the choice — useful when the lands you spend now decide what you can cast later.',
    related: ['priority-modes', 'preferences'],
  },
  {
    id: 'mana-payment',
    section: 'playing',
    title: 'Paying costs by hand',
    summary:
      'With Manual Tap — or by choosing “Choose which lands to tap” in a card’s action menu — you pay a cost yourself: click highlighted mana sources until every pip is covered, then press Pay.',
    body: [
      { kind: 'p', text: 'Each pip of the cost shows whether it is paid from your mana pool, covered by a source you selected, or not yet covered. Auto Pay hands the rest to the auto-tapper; Decline backs out. If paying would sacrifice something, a warning says so first.' },
      { kind: 'p', text: 'When a permanent lets you pay life for a spell discount, the card menu offers a separate cast option showing the life payment. Choose the ordinary cast option to keep your life. Each available source can contribute once.' },
      { kind: 'p', text: 'A Phyrexian pip like {G/P} gets its own Mana | 2 Life switch beside the cost, with the life total it leaves you at. A pip none of your lands can make starts on 2 Life; flip it and the selected lands adjust to cover what is still owed in mana.' },
      { kind: 'p', text: 'Alternative ways to pay — convoke, delve, tapping creatures for generic mana and the like — open their own selector, which shows the remaining cost as you choose.' },
    ],
    related: ['auto-tap', 'decisions'],
  },
  {
    id: 'decisions',
    section: 'playing',
    title: 'Choices and prompts',
    summary:
      'When a spell or ability needs an answer — a mode, a value for X, a target, how to split damage, the order of cards — a prompt says what is being asked and by which card. Most prompts have View Battlefield to look at the table before answering.',
    body: [
      { kind: 'ul', items: [
        'Yes / No — “may” abilities. With several identical triggers, “Yes to all” answers them together.',
        'Choose mode — pick the required number of modes; escalate and similar costs are shown as you go.',
        'Choose X — a slider with − and + buttons, capped at the most you can pay.',
        'Targets — click a highlighted card or a player’s life total, then Confirm. Optional targets can be declined.',
        'Remove counters as a cost — use the − and + controls on highlighted battlefield permanents. You may remove several counters from one permanent or spread the payment across them; Confirm becomes available when the required total is allocated. Open Adjust allocations in the prompt for a named list when the board is crowded or an attachment is hard to reach.',
        'Divide damage — assign amounts to each target; “Lethal” marks the point where a creature dies.',
        'Combat damage — with trample, deathtouch or several blockers, you assign attacking damage yourself, split among the blockers however you like (only trample needs every blocker at lethal before damage reaches the player).',
        'Order — cards going to the top of a library get arrow buttons; your own triggers can be dragged into order (see Ordering your triggers).',
        'Search — pick cards from a library, or Fail to Find.',
        'Piles — split cards into two piles, or assign them randomly.',
        'Options — longer lists (a card name, a creature type) come with a search box.',
      ] },
      { kind: 'p', text: 'A strip above each prompt names the cards involved — the source, its target, what triggered it — so a long chain of triggers stays readable.' },
      { kind: 'p', text: 'Secret choices stay private until everyone has confirmed. Pick a number in the prompt, or click a highlighted creature to vote for it; Select None submits an optional ballot without a vote. The game log reveals everyone’s choices together after the last answer.' },
    ],
    related: ['targeting-and-combat', 'yields', 'mana-payment', 'dexterity-flips', 'trigger-order', 'forced-choices'],
  },
  {
    id: 'forced-choices',
    section: 'playing',
    title: 'Choices with only one answer',
    summary:
      'Some questions have only one legal answer — “you may reveal a Dragon card from your hand” with none in hand, a search with nothing left to find, a mandatory trigger whose only legal target is the creature itself. Unless you set Choices with only one answer to Ask me every time, the game answers those for you — at once, or after a short pause — and a short notice at the top of the board says what it answered.',
    body: [
      { kind: 'p', text: 'You are still asked every one of these questions, and your opponent sees you deciding for a moment. That is on purpose: if the game skipped the question whenever you had no answer but “no”, the missing pause would tell your opponent what is in your hand or library. The pause changes every time — usually a second or so, now and then two — so it looks like a person clicking rather than a timer.' },
      { kind: 'p', text: 'Only questions the server marks as having a single legal answer are answered for you. Anything with a real choice — including which order cards go in — still waits for you, and so does a question that is how you look at cards (“look at the top three…”), so you always get to see them.' },
      { kind: 'p', text: 'Preferences → Gameplay → Choices with only one answer has three settings. Answer instantly is the fastest: no pause at all, but an observant opponent can tell from the speed that you had no choice — and so, sometimes, what you don’t have in hand. Answer for me (the default) waits the short, human-like pause first, so a forced answer is hard to tell from a quick real one, though an opponent who watches closely may still read something into the timing. Ask me every time shows each of these questions like any other, so you click through them yourself and your timing gives nothing away.' },
      { kind: 'p', text: 'When everyone at the table can already see that a question has only one answer, there is no pause in either answering setting: a Flametongue Kavu entering with no other creature in play has to target itself, and waiting would hide nothing. The game only skips the pause when every possible answer was in plain view — creatures on the battlefield, cards in graveyards, spells on the stack. If the answer could depend on a card nobody else can see — a hand, a library, a face-down creature — the pause stays.' },
      { kind: 'p', text: 'With Ask me every time, a “you may…” you can’t actually do (“you may discard a creature card” with no creature card in hand) shows its Yes button greyed out, so No is the only answer.' },
      { kind: 'tip', text: 'Click the notice to dismiss it early.' },
    ],
    related: ['decisions', 'preferences', 'trigger-order'],
  },
  {
    id: 'trigger-order',
    section: 'playing',
    title: 'Ordering your triggers',
    summary:
      'When several of your abilities trigger at once, they go on the stack in the order they triggered. Switch to Full Control for the turn that needs it, or turn off Order my triggers automatically (Preferences → Gameplay), to choose the order yourself — scry before you draw, make the token before the counters go out.',
    body: [
      { kind: 'p', text: 'Full Control always lets you order them, whatever the preference says — the priority-mode button is the quickest way in, and switching back to Auto or Stops returns to your preference. The first time the game orders your triggers for you where the order could matter, a short notice says so; Always let me order turns the preference off.' },
      { kind: 'p', text: 'When you order them, the game stops before your triggers go on the stack and lists them top to bottom in the order they will resolve: the top one resolves first. Drag a row, use its arrows, or press Alt+↑/↓ on it, then Put on the stack. Reset order puts them back in the suggested order.' },
      { kind: 'p', text: 'An ability that checks this turn as it resolves — Infusion’s “if you gained life this turn” — is marked checks as it resolves and starts at the bottom, so it sees the life your other triggers gain first. Automatic ordering does the same.' },
      { kind: 'p', text: 'You are only asked when the order can matter. A single trigger, or several copies of the same ability that choose no targets (two Soul Wardens seeing one creature enter), goes straight on the stack. Abilities that need targets pick them after you set the order, one by one.' },
      { kind: 'p', text: 'If your opponent’s abilities triggered at the same moment, the player whose turn it is orders theirs first, so the other player’s end up on top and resolve first.' },
      { kind: 'tip', text: 'Tick Don’t ask again in the prompt to go back to automatic ordering; it takes effect straight away.' },
    ],
    related: ['preferences', 'decisions', 'yields', 'priority-modes'],
  },
  {
    id: 'yields',
    section: 'playing',
    title: 'Yields — stop being asked',
    summary:
      'Tell the game to stop asking about a repeating ability: auto-pass it on the stack, or always answer its “you may” Yes or No. Every copy of the ability follows the same yield.',
    body: [
      { kind: 'p', text: 'This is the fix for a repeating trigger asking you the same question every turn. There are three ways in:' },
      { kind: 'p', text: '• When an ability asks “you may…”, tick Remember my answer under the Yes/No buttons. The answer you give becomes automatic for the rest of the game.' },
      { kind: 'p', text: '• When an ability is on top of the stack, press the Yield… button under it. Choose Auto-pass this turn, Auto-pass all game, Always answer Yes or Always answer No.' },
      { kind: 'p', text: '• Right-click any ability on the stack for the same menu.' },
      { kind: 'p', text: 'An ability you are yielding to shows a small marker on the stack (Auto-pass, Always yes or Always no). All of them are listed in the Active yields panel in the bottom-right corner while they are in force. Click a marker to change it, or press × in the panel to be asked again.' },
      { kind: 'tip', text: 'Yields never choose targets, order triggers, pick modes or pay costs for you. They only pass priority or answer a plain “you may”.' },
    ],
    shortcuts: ['stack-yield-menu'],
    related: ['priority-modes', 'trigger-order'],
  },
  {
    id: 'card-preview',
    section: 'playing',
    title: 'Reading a card in full',
    summary:
      'Hover any card — hand, battlefield, stack, or the top of a graveyard or exile pile — and it opens full-size beside the pointer. On a touch screen, press and hold the card instead, or tap it and pick “View card”.',
    body: [
      { kind: 'p', text: 'The preview shows what is true right now, not just what is printed: power and toughness broken into base, effects and damage, counters, granted types and abilities, who controls it, what protects it, and the ways you can currently play it. Hold the hover for a moment and the card’s rulings appear.' },
      { kind: 'p', text: 'The preview is read-only; nothing you do to it changes the game. Press F while a double-faced card is open to see its other face.' },
    ],
    shortcuts: ['flip-dfc'],
    related: ['targeting-and-combat', 'zone-browsers', 'card-badges', 'preferences'],
  },
  {
    id: 'targeting-and-combat',
    section: 'playing',
    title: 'Casting, attacking and blocking',
    summary:
      'Drag a card from your hand onto the battlefield to cast it, drag an attacker onto a defender to attack, and drag a blocker onto an attacker to block. Clicking works everywhere dragging does.',
    body: [
      { kind: 'p', text: 'Some abilities require one card from each eligible graveyard. The target prompt shows the total required; after you select a card, the other cards from that graveyard cannot be selected. Deselect it to choose a different card there, then confirm once every required graveyard is covered.' },
      { kind: 'p', text: 'Clicking a card with more than one way to play opens its action menu — Cast, Play, Turn Face-Up, an activated ability, or View card. A permanent’s abilities are in the same menu.' },
      { kind: 'p', text: 'A split card lists each half in its action menu. When a card with Fuse is in your hand, the fused option casts both halves together. Choose and confirm the targets for the left half, then for the right half; the stack shows one spell and players can respond normally.' },
      { kind: 'p', text: 'Repeatable additional costs such as Squad appear as separate cast options: Squad ×1, Squad ×2, and so on. Choose the number of payments in the card’s action menu, then select any cards the cost asks you to discard or exile.' },
      { kind: 'p', text: 'For an activated ability that says “Choose one”, choose the mode and any targets before its costs are paid. Cancel either prompt to keep your mana and permanent untapped. The stack shows the chosen mode so other players can respond. A mode that can be chosen only once stays used even if the ability is countered.' },
      { kind: 'p', text: 'For spells that require targets with equal toughness or another shared value, your first pick narrows the highlighted choices. Deselect all picks to choose a different group. You can still confirm no targets when the spell allows it.' },
      { kind: 'p', text: 'Some spells ask for targets in sequence. Choose the player or permanent first, then Confirm to see the valid next targets. Back lets you change an earlier choice and updates the later choices.' },
      { kind: 'p', text: 'Attacking: Attack All sends every creature that can attack, Skip Attacking declares none, and after choosing some the button reads Attack with N. In multiplayer each attacker needs a defender; the banner says who you are allowed to attack.' },
      { kind: 'p', text: 'Enlist: after confirming attackers, each enlisting creature asks you to click an eligible creature on the battlefield to tap. Choose Select None to skip it. The power bonus appears on the stack after the attack is declared, so players can respond before it resolves.' },
      { kind: 'p', text: 'Blocking: drag each blocker to an attacker, then Confirm Blocks — or No Blocks. Clear resets your choices.' },
      { kind: 'p', text: 'Crowded combat: hover any attacker or blocker (long-press on a phone) to pick out its block. Everything tied to it lights up — its blockers or the attackers it blocks, their other blockers, and the rest of its band — the rest of the board dims, and a caption on the card names who it is blocking or blocked by.' },
      { kind: 'p', text: 'A blocked attacker stays blocked even after its blockers die or leave combat. When that happens its red attack chevron turns into a blue Blocked tag, and any attack arrow it has fades to a dashed line, so it does not look like it is still getting through.' },
      { kind: 'p', text: 'Dragging one attacker onto another bands them (CR 702.22) — the drop lights up only when the band would be legal: creatures with banding plus at most one without, or creatures that all share the quality one of them "bands with other" (any number of legendary creatures, when one of them bands with other legendary creatures). On a phone, swipe left and right on the opponent strip to move between boards.' },
    ],
    related: ['pass-button', 'decisions', 'multiplayer-camera', 'zone-browsers'],
  },
  {
    id: 'zone-browsers',
    section: 'playing',
    title: 'Browsing zones',
    summary:
      'Click a graveyard, exile or library pile to open a full browser of its contents. Press D to open the deck browser, which tracks what is left in your library.',
    body: [
      { kind: 'p', text: 'When a spell or ability on the stack targets a card in a graveyard, that card is pulled out beside its pile, marked Targeted with its name and what is targeting it, and the targeting arrow points at it. The stack’s text box names the target too. Hover the card to read it, or click it to open the graveyard, where the targeted card is highlighted.' },
      { kind: 'p', text: 'When a graveyard card can escape, click it in the zone browser and choose its Escape action. The action shows the mana cost and prompts you for any cards to exile. If more than one escape cost is available, choose the one you want. Temporary escape grants expire at the end of the turn.' },
      { kind: 'p', text: 'A graveyard card you can beam up appears beside your hand, outlined in purple and showing its beam me up cost. Click it and choose its Beam me up action, then click the creature to return — only creatures that can pay are highlighted, so one under Marooned is not — and confirm. When a card like Scotty, Miracle Worker also grants beam me up, each source is its own action, named after the card granting it.' },
      { kind: 'p', text: 'Junk tokens have an action in their card menu during your main phase while the stack is empty. Activate it to sacrifice the token and exile your top card; the exiled card appears translucent beside your hand. Click it to play it before the turn ends, paying its normal cost and following normal timing and land-play limits.' },
      { kind: 'p', text: 'The deck browser has two views: Deck list (everything still in your library) and Library order, which shows cards whose position you know — after a scry, for instance — with the top card marked “Next draw”. When you own cards outside the game, a third view, Outside the game, shows them.' },
    ],
    related: ['outside-the-game', 'targeting-and-combat'],
    shortcuts: ['deck-browser', 'escape'],
  },
  {
    id: 'outside-the-game',
    section: 'playing',
    title: 'Cards from outside the game',
    summary:
      'Learn, the wishes (Burning Wish, Living Wish…) and a few others bring in a card you own from outside the game — your sideboard. Every mode gives you one; press D and open Outside the game to see yours.',
    body: [
      { kind: 'p', text: 'Only these effects can touch your sideboard, and nobody else can see it. When one resolves you choose from the cards that match — a sorcery for Burning Wish, a Lesson for Learn — or choose nothing.' },
      { kind: 'p', text: 'Wish is different: it lets you play one card from outside the game this turn, straight from your sideboard. While you may, your Deck pile glows and is marked Play; open it, and the cards you can play now are highlighted on the Outside the game tab. Click one to cast it (or play it, if it’s a land) — you pay for it and follow the usual timing, and a land uses your land drop.' },
      { kind: 'h', text: 'Where your sideboard comes from' },
      { kind: 'table', head: ['Mode', 'Outside the game'], rows: [
        ['Sealed, Draft, cube Sealed', 'Every card in your pool you didn’t put in your deck.'],
        ['Your own or a premade deck', 'The sideboard you saved with it — up to 15 cards. A deck with wishes or Learn and an empty sideboard is warned about in the deckbuilder.'],
        ['Random deck', 'The rest of the boosters it was built from; under a constructed format, a fifteen-card wishboard from the legal pool.'],
        ['Momir Basic', 'One wishboard rolled for the game and shared by both players: fifteen cards from every set, with Lessons for the creatures that learn.'],
        ['Cube Pool Play', 'Fifteen random cube cards your deck doesn’t play.'],
      ] },
      { kind: 'p', text: 'A wishboard is a menu, not a pile: every card in it is different, and it always covers the kinds of card the wishes ask for — Lessons, instants, sorceries, creatures, a land, an artifact and an enchantment — when the pool has them.' },
    ],
    related: ['zone-browsers', 'cards-momir', 'cards-random', 'limited-deckbuilding', 'deckbuilder'],
  },
  {
    id: 'card-badges',
    section: 'playing',
    title: 'Card badges',
    summary:
      'Small labels on a card mark a state the card text alone will not tell you: Plotted, Prepared, Warped, Dashed, Band N, and counters.',
    body: [
      { kind: 'ul', items: [
        'Plotted (CR 718) — sitting face-up in exile; cast it for free on a later turn.',
        'Prepared (Secrets of Strixhaven) — a copy of its spell waits castable in exile; casting the copy unprepares the creature.',
        'Warped (CR 702.185) — exiled at the beginning of the next end step, then castable again from exile.',
        'Dashed (CR 702.109) — has haste; returned to its owner\'s hand at the beginning of the next end step.',
        'Band N (CR 702.22) — which attacking band this creature belongs to.',
      ] },
    ],
  },
  {
    id: 'undo',
    section: 'playing',
    title: 'Undo',
    summary:
      'The undo button takes back your most recent action when the server can still safely rewind — typically a tap or a cast that has not resolved.',
  },
  {
    id: 'game-log',
    section: 'playing',
    title: 'The game log',
    summary: 'A running record of everything that happened, in rules order. Useful when an interaction resolved differently than you expected.',
    body: [
      { kind: 'p', text: 'Open it with the Log (N) toggle; N counts the entries. For a complete look back after the game, the replay steps through every action.' },
      { kind: 'p', text: 'Dice are rolled automatically when an ability resolves. Mr. House counts the Treasure mana spent on that activation, including mana you floated before activating him. The log shows each die’s size and result beside the card that rolled it; all players see the same results.' },
    ],
    related: ['replays'],
  },
  {
    id: 'game-over',
    section: 'playing',
    title: 'Conceding and the end of a game',
    summary:
      'Concede sits top right and asks for confirmation. When the game ends you see Victory!, Draw or Defeat with the reason, and buttons to play again, keep watching or watch the replay.',
    body: [
      { kind: 'p', text: 'The confirmation says what conceding means at your table: in a 1v1 it ends the game, in a pod the others play on, and in Two-Headed Giant your whole team is out.' },
      { kind: 'ul', items: [
        'Play Again — against the AI, rebuilds the same game in one click.',
        'Keep Watching — in multiplayer, an eliminated player can stay and spectate the rest; Leave Game exits.',
        'Watch Replay — opens this game’s replay.',
        'Return to Menu — leave the game.',
      ] },
      { kind: 'p', text: 'In a game played for ante, the result also shows which cards you won and lost.' },
    ],
    related: ['replays', 'tournament-flow', 'disconnects', 'playing-for-ante'],
  },
  {
    id: 'disconnects',
    section: 'playing',
    title: 'Disconnects and reconnecting',
    summary:
      'Lose your connection and the client keeps retrying on its own; reload the page and you are put back in your game. Your opponent sees a countdown, and if you are not back in time the game is conceded for you.',
    body: [
      { kind: 'ul', items: [
        'In a game, an opponent who drops gets a two-minute countdown before they auto-concede.',
        'In a lobby or between bracket rounds, the grace period is five minutes; other players can add a minute, or kick them after two.',
        'Opening the same game in a second tab moves your session there; the first tab offers “Use here” to take it back.',
        'Jump In remembers your pack choices if you reconnect mid-pick.',
        'When the server is updated, a notice says so and your game waits on the table; it reconnects on its own within a minute or two and carries on where it stopped. No one is conceded for the update.',
      ] },
      { kind: 'tip', text: 'Your seat is tied to this browser, so reconnect from the same browser you joined with.' },
    ],
    related: ['game-over', 'tournament-flow', 'troubleshooting'],
  },

  // ── Decks ──────────────────────────────────────────────────────────────
  {
    id: 'deckbuilder',
    section: 'decks',
    title: 'The deckbuilder',
    summary:
      'Search the full implemented card pool, click to add a copy, right-click or shift-click to remove one. Decks save to this browser, or to your account when signed in.',
    body: [
      { kind: 'h', text: 'Finding cards' },
      { kind: 'ul', items: [
        'The search box takes plain names or the full query language.',
        'Filters: set (search by name or code, sorted by name or release), colour with Includes / Exactly / At most, card type and Legendary, subtype, rarity, mana value, power and toughness ranges, and common keywords. Clear filters resets them.',
        'Sort by name, mana value, colour or rarity. Basic lands are not in the grid — use Suggest basic lands instead.',
      ] },
      { kind: 'h', text: 'Your deck' },
      { kind: 'ul', items: [
        'The Deck pane shows the card count, colour pips and mana curve.',
        'The sideboard drop zone holds up to 15 cards you own outside the game. A deck with wishes or Learn cards needs one — the panel says so, and counts your Lessons.',
        'Suggest basic lands fills your land base from your colours and curve.',
        'Pick a format to restrict the catalogue to legal cards and check the deck live.',
      ] },
      { kind: 'h', text: 'Toolbar' },
      { kind: 'ul', items: [
        'My decks — every saved deck, searchable and sortable (recent, name, card count, colour), with rename and delete. Each shows which formats it is legal in.',
        'Examples — load a ready-made starter deck to play or tweak.',
        'Import, Edit as text (plain, Arena or Moxfield), and Share.',
        'New deck, Save, Save as and Delete. On a narrow screen the tools fold into More.',
      ] },
    ],
    shortcuts: ['deckbuilder-remove', 'flip-dfc'],
    related: ['search-syntax', 'card-browser', 'card-printings', 'deck-legality', 'commander-decks', 'deck-sharing', 'where-decks-live'],
  },
  {
    id: 'card-printings',
    section: 'decks',
    title: 'Choosing a printing',
    summary:
      'Every card in a deck can use any printing the engine knows — a specific set’s art and frame. Open a card’s printing picker to choose one, or filter the grid to a set and new copies use that set’s printing.',
    body: [
      { kind: 'p', text: 'The picker lists every printing, searchable by set, collector number or artist; Use default goes back to the latest. Printings travel with the deck: share links carry them, and imports read the set code and collector number of Arena-style lines.' },
      { kind: 'p', text: 'A drafted or sealed deck you save keeps the printings you actually opened.' },
    ],
    related: ['deckbuilder', 'deck-import-export'],
  },
  {
    id: 'deck-legality',
    section: 'decks',
    title: 'Formats and deck legality',
    summary:
      'The deckbuilder’s format dropdown — Standard, Pioneer, Modern, Pauper, Legacy, Vintage, Commander, Brawl, Standard Brawl, Premodern, Highlander or Canadian Highlander — filters the catalogue to legal cards and checks the deck as you build: “Legal ✓” or “N issues”.',
    body: [
      { kind: 'p', text: 'Hover the issue count for the list. Individual rows are flagged too: not legal in the format, too many copies, singleton violations, cards outside the commander’s colour identity.' },
      { kind: 'p', text: 'A lobby can restrict everyone to a format, in which case the deck is checked again when you submit it. “No format” lets in anything the engine implements.' },
      { kind: 'p', text: 'A card that imported but is not implemented yet is kept as a placeholder and marked; it cannot be played until it is.' },
    ],
    related: ['deckbuilder', 'cards-bring-a-deck', 'set-completion', 'highlander'],
  },
  {
    id: 'commander-decks',
    section: 'decks',
    title: 'Commander decks',
    summary:
      'Click the crown on a legendary creature in your deck — or a card that says it can be your commander — to make it the commander. Choose the Commander, Brawl or Standard Brawl format to have singleton and colour identity checked for you.',
    body: [
      { kind: 'p', text: 'Planeswalkers are not commanders by default: only the ones whose text says “can be your commander” get a working crown.' },
      { kind: 'h', text: 'Two commanders' },
      { kind: 'p', text: 'A deck can have two commanders when they are a legal pair: two cards with Partner; two with the same “Partner—” ability, such as Partner—Friends forever; a “Partner with” card and the card it names; a commander with Choose a Background and a legendary Background enchantment; or a Doctor’s companion and a legendary Time Lord Doctor. With one commander crowned, click the crown on its partner to add it as the second; clicking a crowned card un-crowns it.' },
      { kind: 'ul', items: [
        'The deck is still 100 cards, counting both commanders.',
        'Its colour identity is both commanders’ identities combined.',
        'Each commander has its own commander tax, and 21 combat damage from either one alone knocks a player out — the two don’t add together.',
        'If the two can’t pair, the deck’s issues list says why.',
      ] },
      { kind: 'h', text: 'Searching inside your colours' },
      { kind: 'p', text: 'Once a commander is crowned in a Commander, Brawl or Standard Brawl deck, the card search shows only cards inside the commander’s colour identity (both commanders’ combined for a pair). The ♛ Identity chip beside the search box turns that off when you want to browse everything; crowning a different commander turns it back on.' },
      { kind: 'p', text: 'Imported lists get their commander set automatically — from a `Commander` section, an Archidekt `Commander` category or a `*CMDR*` marker, and both of them when two are listed. MTGO and Moxfield exports that put the commander on the sideboard (or in a last block after a blank line) work too: in a 100-card list, a one- or two-card sideboard of cards that can lead the deck together is read as the commanders, and the import preview says so. Importing a deck with a commander switches the deckbuilder to the Commander format. A Commander deck shows up in the Commander mode’s deck picker and can be handed to an AI seat.' },
    ],
    related: ['rules-commander', 'deck-legality', 'deckbuilder', 'deck-import-export', 'companions'],
  },
  {
    id: 'companions',
    section: 'decks',
    title: 'Companions',
    summary:
      'A card with Companion — Lurrus, Kaheera, Zirda and the rest — can wait outside your deck as its companion. Put it on your sideboard and click “☆ Make companion” beside it; in the game it sits face up next to your command zone, and once per game you can pay {3} to put it into your hand.',
    body: [
      { kind: 'p', text: 'Each companion has a condition your deck has to meet, printed after “Companion —”. The deckbuilder checks it as you build and lists what breaks it — a deck with an even-cost card can’t take an odd-only companion, for example. The companion itself doesn’t count toward the condition; only the deck does.' },
      { kind: 'ul', items: [
        'Any format: a constructed deck keeps the companion on its sideboard, and it counts toward the four-copy limit with the copies in your deck.',
        'Commander: the companion isn’t one of your 100 cards. Your commander counts toward the condition, the companion has to fit the commander’s colour identity, and it can’t share a name with a card in your deck.',
        'Only one card can be your companion.',
      ] },
      { kind: 'h', text: 'In the game' },
      { kind: 'p', text: 'Both players see each other’s companion, labelled “Companion” beside the command zone. On your turn, in a main phase with nothing on the stack, click it to pay {3} and put it into your hand. That doesn’t use the stack, so you can cast it straight away if you have the mana. You can only do this once per game; after that the card is an ordinary card — if it dies, it goes to your graveyard.' },
      { kind: 'p', text: 'An imported list with a `Companion` section sets the deck’s companion for you, and the lobby’s paste box reads it the same way.' },
    ],
    related: ['commander-decks', 'deck-import-export', 'deck-legality', 'deckbuilder'],
  },
  {
    id: 'search-syntax',
    section: 'decks',
    title: 'Search syntax',
    summary:
      'The deckbuilder, card browser, cube builder and sealed pool search speak a Scryfall-style query language — `t:creature`, `c<=rw`, `cmc>=4`, `o:flying`, `f:standard`, `is:legendary`. The `?` button beside the search box lists every operator with examples.',
    body: [
      { kind: 'p', text: 'The grammar is Scryfall’s, deliberately: bare words match names, `key:value` filters, `-` negates, `or` and parentheses group, quotes hold phrases together, and the comparison operators `:` `=` `>` `<` `>=` `<=` work wherever a value is ordered.' },
      { kind: 'ul', items: [
        'Colour — `c:rg`, `c:azorius`, `c<=rw`, `c:colorless`, and `id:` for colour identity.',
        'Type and text — `t:goblin`, `t:legendary`, `o:flying` for oracle text, `kw:trample` for keywords.',
        'Cost — `mv>=4` (`cmc` also works), `m:{2/G}` for a specific mana cost.',
        'Stats — `pow>=4`, `tou<2`, `loy:3`, and `pow>tou` to compare two of them. A `*` power or toughness has no number, so it matches no comparison.',
        'Printing — `s:fdn` for a set, `r:mythic` for rarity.',
        'Legality — `f:standard`, `f:commander`, `f:pauper`.',
        'Flags — `is:legendary`, `is:permanent`, `is:multicolor`, `is:vanilla`, `is:dfc`.',
      ] },
      { kind: 'p', text: 'Under the search box a line reads your query back in plain words — “Cards where name contains "bolt" and power ≥ 4” — so you can see how it was understood. A mistyped filter is underlined with what went wrong; when there is an obvious fix, such as `powr` for `pow`, a Use button applies it. The `?` panel also lists every filter with its operators.' },
      { kind: 'p', text: 'Not every filter Scryfall documents is implemented. Anything needing data the engine does not carry — prices, artists, printing dates — is rejected rather than silently ignored, and so is anything the card model does not distinguish yet: `is:split` answers “split-card layout not modelled”. A query never quietly means something other than what you typed.' },
    ],
    related: ['deckbuilder', 'card-browser'],
    links: [{ label: 'Scryfall’s full syntax reference', href: 'https://scryfall.com/docs/syntax' }],
  },
  {
    id: 'deck-import-export',
    section: 'decks',
    title: 'Import and export',
    summary:
      'Paste a decklist exported from MTG Arena, MTGO, Moxfield, Archidekt, TappedOut, deckstats or plain text (`4 Lightning Bolt`) straight into the deckbuilder or a lobby deck picker, and export the same way.',
    body: [
      { kind: 'p', text: 'The common deck sites’ exports are accepted interchangeably, so whatever your other tool exports should paste in as-is. These line shapes are recognised:' },
      { kind: 'ul', items: [
        '`4 Lightning Bolt` — plain.',
        '`4x Lightning Bolt` — the Moxfield / Archidekt “x”.',
        '`4 Lightning Bolt (LEA) 161` — Arena, with set code and collector number. Give these when you want a specific printing; without them you get the latest.',
        '`1 Cardname (SET) *F* *A* 42 #tag` — Moxfield bulk edit. Foil, alter and tag markers are read and discarded.',
        '`1x Sol Ring (CMM) 410 [Ramp]` — Archidekt. Categories are discarded, except that a `Commander` category makes the card your commander and a category marked `{noDeck}` (Maybeboard, a sideboard) keeps it out of the deck.',
        '`1x Atraxa, Praetors’ Voice *CMDR*` — TappedOut’s commander marker; deckstats’ `# !Commander` and `[SET#123]` prefix are read too.',
        '`SB: 2 Counterspell` — the MTGO sideboard prefix.',
      ] },
      { kind: 'p', text: 'Section headers are case-insensitive and may end in `:` or a card count, or sit behind `//`: `Deck` / `Mainboard` / `Main Deck` / `Maindeck`, `Sideboard` / `Side` / `SB`, `Commander` / `Commanders` / `EDH`, `Companion`, `Maybeboard` / `Considering`, and `About`. The main deck, the sideboard and the commander are all imported; the maybeboard is listed in the preview but left out. Blank lines and other lines starting with `//` or `#` are ignored.' },
      { kind: 'p', text: 'A `Companion` section makes its card the deck’s companion: it is kept on your sideboard, and the deckbuilder checks the companion’s condition against the deck. See Companions.' },
      { kind: 'p', text: 'MTGO and Moxfield’s MTGO or plain-text exports do not label the commander: it sits on the sideboard or in a last block after a blank line. When the deck has no commander marked, and that block is one or two cards that can lead the deck together, they become the commander(s) — but only in the Commander format or a 100-card list, so a 60-card deck with a real sideboard is never touched. The preview says when it did this.' },
      { kind: 'p', text: 'A line the parser cannot read is listed in the preview as skipped, and a card it reads but cannot match is reported and imported as a placeholder, so an import never silently loses cards. Export writes the plain `4 Lightning Bolt` shape, which every one of the above tools reads.' },
    ],
    related: ['deckbuilder', 'deck-sharing', 'commander-decks', 'companions'],
  },
  {
    id: 'deck-sharing',
    section: 'decks',
    title: 'Share links',
    summary:
      'A deck can be shared as a single URL that carries the whole list — no account needed on either end. Opening it drops the deck into the recipient’s deckbuilder.',
    body: [
      { kind: 'p', text: 'Use Share in the deckbuilder; the link is copied for you. It carries the deck’s name, cards, format, commander and chosen printings. The sideboard is not included — export the decklist as text if the sideboard matters.' },
    ],
    related: ['deckbuilder', 'deck-import-export'],
  },
  {
    id: 'card-browser',
    section: 'cards',
    title: 'The card browser',
    summary:
      'Cards, in the home screen’s top bar, shows every card you can play in Argentum — search and filter it like the deckbuilder, and click a card to see it up close. Nothing to build first.',
    body: [
      { kind: 'p', text: 'Every card listed is implemented, so anything you find here works in a game. The search box, the filter menu and the sort are the deckbuilder’s, query language and all; the tiles are bigger, and the grid button beside the result count switches between big and smaller cards.' },
      { kind: 'h', text: 'A card up close' },
      { kind: 'ul', items: [
        'Click a card for its art, Oracle text and the formats it is legal in. A double-faced card shows both faces.',
        'Every printing is listed under the text — pick one to see its art.',
        'The type line, the set and the rarity are searches: click “Goblin” to see every playable Goblin, or a set to see all of it.',
        '← and → step through the results, Esc or Back closes.',
        'Start a deck with it opens the deckbuilder on a new deck holding the card, in the printing you were looking at. A card that can be a commander also offers Build a Commander deck, which crowns it in a new Commander deck.',
        'Copy link copies a link straight to the card.',
      ] },
      { kind: 'tip', text: 'The address bar keeps your search and the open card, so a link to a search — `s:dmu r:mythic`, say — opens the same results for whoever you send it to.' },
      { kind: 'p', text: 'Decks & Cubes links here with Browse cards, and a set opened on Set Completion offers Browse cards for its playable ones.' },
    ],
    related: ['search-syntax', 'deckbuilder', 'set-completion', 'card-printings'],
  },
  {
    id: 'set-completion',
    section: 'cards',
    title: 'Which cards are implemented',
    summary:
      'Set Completion, under Sets in the home screen’s top bar, lists every set and how much of it the engine can actually play. Useful before committing to a deck or picking a set to draft.',
    body: [
      { kind: 'p', text: 'The deckbuilder only ever offers implemented cards, so a deck you build there always works. This page is the other direction: it tells you what is missing from a set you had in mind.' },
      { kind: 'p', text: 'Open a set to get its full card list, each card marked implemented or missing. A handful come back “not planned” — cards needing something the engine will never carry, like ante or physical dexterity — and those are left out of every total, so the percentage measures what there is any intention of building.' },
      { kind: 'ul', items: [
        'The banner counts distinct cards implemented, printings, complete sets and cards not planned; View progress charts implementation over time.',
        'Filter sets by name or code, and by All, Standard, In progress or Complete.',
        'The list opens on Main sets. Switch with the type chips — All types, Commander, Supplemental (Masters, Modern Horizons, Jumpstart, starter sets), Decks & boxes (Duel Decks, Planechase, From the Vault) or Un-sets. Each chip counts the sets it would show with your other filters applied.',
        'Sets outside the main line carry a coloured badge naming their exact type and a matching stripe down the left edge, so a Commander product stands out in the grid.',
        'Once you narrow the list, a line above the grid totals the sets in view. The filters, sort and the open set live in the address bar, so a filtered view can be bookmarked or shared.',
        'Sort by Newest, Most complete, Most cards done, Most Assay-ready, Least AI-read or A–Z.',
        'Inside a set, filter its cards by Implemented, Missing, ⚡ Assay-ready, AI gaps or Not planned.',
        'Set tiles with implemented cards carry an AI-reading score such as “✓ AI 82%”: the share of them the built-in AI fully understands. It fills in a moment after the page loads.',
      ] },
      { kind: 'p', text: 'The second tab, Assay Explorer, is a different question about the same corpus: not which cards are written, but how much of Magic’s printed text the engine can read at all.' },
    ],
    related: ['set-guide', 'card-browser', 'deckbuilder', 'assay-ready', 'assay-explorer', 'ai-reading'],
  },
  {
    id: 'assay-ready',
    section: 'cards',
    title: '⚡ Assay-ready cards',
    summary:
      'On Set Completion, ⚡ Assay-ready counts the cards a set is still missing that Argentum Assay already reads end to end — the ones the engine could express today, with nothing new built for them.',
    body: [
      { kind: 'p', text: 'It measures how much of what is left is cheap, which is a different question from how much is done. A set sitting at 40% with two hundred Assay-ready cards is closer to finished than its percentage suggests; one at 95% with none left is down to the cards that need new engine work first.' },
      { kind: 'ul', items: [
        'Set tiles carry a “⚡ 34 Assay-ready” tag in the footer — shown only above zero, because a row of “0 Assay-ready” across nine hundred sets would bury the sets where the number is the reason to look.',
        'Sorting the grid by “Most Assay-ready” puts the sets holding the most of that work first.',
        'Inside a set, the ⚡ Assay-ready filter lists exactly those cards, and each one wears a ⚡ on its tile.',
      ] },
      { kind: 'p', text: 'Hovering any card in a set gives you Assay’s reading of it: that it reads the card whole, or which line it declined and what stopped it. A card the reading does not cover stays silent rather than showing a negative — “Assay can’t read this” and “nobody asked Assay” are different statements, and only the first is worth a badge.' },
      { kind: 'p', text: 'The counts come from a ledger baked from a full run of the parser rather than being recomputed per request, so they move when the grammar does rather than continuously.' },
    ],
    related: ['set-completion', 'assay', 'assay-explorer'],
  },

  // ── Advanced ───────────────────────────────────────────────────────────
  {
    id: 'keyboard-shortcuts',
    section: 'advanced',
    title: 'Keyboard shortcuts',
    summary: 'The complete list of keys the client listens for.',
    body: [{ kind: 'shortcuts' }],
  },
  {
    id: 'replays',
    section: 'community',
    title: 'Replays',
    summary:
      'Finished games can be replayed frame by frame. Scrub with the timeline, step with the arrow keys, play/pause with space.',
    body: [
      { kind: 'p', text: 'Replays in the top bar lists every game you finished, newest first, with who played, who won and how long it was; switch to Oldest first, or By tournament to group a tournament’s games in round order. Replays are also linked from your profile, from a tournament’s Replays button, and from Watch Replay at the end of a game.' },
      { kind: 'p', text: 'Every replay has its own link that anyone can open — they watch it as a spectator, so hidden cards stay hidden.' },
      { kind: 'h', text: 'Replay files' },
      { kind: 'p', text: 'Export saves a finished game as a compact replay file; Open file in the replay list plays one back (up to 8 MB). Uploaded replays can be watched but not re-shared.' },
      { kind: 'h', text: 'From replay to scenario' },
      { kind: 'p', text: 'On a desktop, Share as scenario copies a link that opens the Scenario Builder at exactly the current frame, and Save snapshot downloads it as a file for the builder’s Load file.' },
      { kind: 'p', text: 'Without an account your replay list follows this browser. Very long games may be marked “Partial recording”.' },
    ],
    shortcuts: ['replay-frame', 'replay-play', 'escape'],
    related: ['scenario-builder', 'profile', 'spectating'],
  },
  {
    id: 'spectating',
    section: 'community',
    title: 'Spectating',
    summary:
      'Live games are listed on the home screen and inside tournaments. A spectator sees both boards and steers the same camera a player does — but never sees a hidden zone.',
    body: [
      { kind: 'p', text: 'Press Spectate on a Live Games row, or ▶ Watch beside a running match on a tournament’s standings. “View: <name> ⟳” switches whose side of the table you sit on; Back to Overview returns to the list.' },
      { kind: 'p', text: 'Both hands stay face down. In a two-player game the name label in each corner shows how many cards that player is holding; at a bigger table the player rail shows it.' },
      { kind: 'p', text: 'When the game ends, the final board stays up with the winner and how the game was won. You go back to the overview after a few seconds, or straight away with Back to Overview; Stay on this board keeps you there to look it over.' },
      { kind: 'p', text: 'Players see how many people are watching, and hovering the eye badge lists them.' },
    ],
    related: ['multiplayer-camera', 'tournament-flow', 'replays'],
  },
  {
    id: 'multiplayer-camera',
    section: 'advanced',
    title: 'Multiplayer camera: Overview, Follow, pin',
    summary:
      'Overview shows every opponent board side by side; turn it off to focus one board at a time. Follow slides the view to whoever is acting; turn it off for a manual camera.',
    body: [
      { kind: 'p', text: 'Number keys 1–9 jump to an opponent’s board — each chip in the rail shows its key — and 0 toggles the overview. Clicking a chip pins that board (📌) and pauses Follow until you press Esc, click the chip again, or click Follow. With the focused camera you can also swipe left and right across the opponent’s board.' },
      { kind: 'p', text: 'Overview is desktop and landscape-tablet only — three boards side by side are unusable on a portrait phone.' },
    ],
    shortcuts: ['opponent-boards', 'overview', 'escape', 'strip-swipe'],
    related: ['table-free-for-all'],
  },
  {
    id: 'lab-tools',
    section: 'advanced',
    title: 'Lab tools',
    summary:
      'Debugging and content tools, not part of normal play: the Scenario Builder, the AI Sandbox and the LLM Tournament runner. They only appear in development builds, because they need server endpoints a production deployment does not expose.',
    body: [
      { kind: 'p', text: 'When available, they are linked from the Lab row in the home-screen footer. A link to one — such as a replay’s “Share as scenario” — still opens on any server that has the development endpoints switched on.' },
    ],
    related: ['scenario-builder', 'ai-sandbox', 'llm-tournament', 'assay-custom-cards'],
  },
  {
    id: 'scenario-builder',
    section: 'advanced',
    title: 'Scenario Builder',
    summary:
      'Start a game from any board state you like: drag cards into each seat’s battlefield, hand, graveyard, exile, library or command zone, choose the phase and the active player, and press Start.',
    body: [
      { kind: 'ul', items: [
        'Edit a placed card — tapped, counters, what it is attached to, the choices it entered with.',
        'Opponent: Yourself (hotseat — you play every seat in one window), the AI, or Two players (each gets their own link). Three- and four-seat scenarios are always hotseat.',
        'Undo and Redo with Ctrl/⌘ Z and Ctrl/⌘ Shift Z. View JSON or Edit as JSON for exact control; Load file opens a saved snapshot.',
        'Share copies an editable link to the scenario.',
        'Custom cards: paste a Scryfall card object and play a card that does not exist.',
      ] },
      { kind: 'p', text: 'A replay frame can be opened here directly with Share as scenario, which is the quickest way to reproduce something odd that happened in a real game.' },
    ],
    shortcuts: ['scenario-undo'],
    related: ['lab-tools', 'replays', 'assay-custom-cards'],
  },
  {
    id: 'ai-sandbox',
    section: 'advanced',
    title: 'AI Sandbox',
    summary:
      'A lobby with only AI players: pick sets for a sealed pool, the number of AI players, games per pairing and how decks are built, then watch the built-in AI play itself and spot where it goes wrong.',
    body: [
      { kind: 'p', text: 'Standings fill in as games finish, live games can be watched (optionally automatically), and New lobby starts another run.' },
    ],
    related: ['lab-tools', 'llm-tournament'],
  },
  {
    id: 'llm-tournament',
    section: 'advanced',
    title: 'LLM Tournament',
    summary:
      'Pits language models against each other in a sealed bracket: list the models (seeded top to bottom), the set, how decks are built and the match length, and follow the participants with their token use and cost.',
    related: ['lab-tools', 'ai-sandbox'],
  },
  {
    id: 'ai-insight',
    section: 'advanced',
    title: 'AI Insight',
    summary:
      'On servers that enable it, a desktop-only panel in the game shows what the built-in AI would do at each decision, and can hand a decision to the AI with “Let the AI play it”.',
    body: [
      { kind: 'p', text: 'It is meant for working on the AI rather than for help in a real game, and is off on ordinary servers.' },
    ],
    related: ['ai-sandbox'],
  },
  {
    id: 'troubleshooting',
    section: 'advanced',
    title: 'Troubleshooting and FAQ',
    summary:
      'Answers to the questions that come up most: lost decks, a ranked game that did not count, a card that is missing, an ability that keeps asking, and a stop you cannot get.',
    body: [
      { kind: 'h', text: 'My decks disappeared' },
      { kind: 'p', text: 'Guest decks live in browser storage, which clearing site data wipes and which does not follow you to another device. Sign in to keep decks in an account; export a decklist or a share link for anything you want to be sure of keeping.' },
      { kind: 'h', text: 'My ranked game did not count' },
      { kind: 'p', text: 'Ranked needs a 1v1 game where both players are signed in. If either was a guest, or there was an AI seat, it played as casual.' },
      { kind: 'h', text: 'I cannot find a card in the deckbuilder' },
      { kind: 'p', text: 'The deckbuilder only offers implemented cards. Set Completion shows what is missing from each set; imports keep unimplemented cards as marked placeholders.' },
      { kind: 'h', text: 'The same trigger asks me every turn' },
      { kind: 'p', text: 'Right-click (or long-press) it on the stack and choose a yield: Always answer Yes, Always answer No, or Always yield.' },
      { kind: 'h', text: 'I never get to respond in a particular step' },
      { kind: 'p', text: 'Auto passes when it thinks you have nothing worth doing. Set a stop on that step in the phase bar, or switch to Full Control.' },
      { kind: 'h', text: 'I cannot respond to my own trigger' },
      { kind: 'p', text: 'Your own spells and abilities pass to your opponent by default. Turn on Hold priority over my own in Preferences for the kinds you want to answer.' },
      { kind: 'h', text: 'My opponent disappeared' },
      { kind: 'p', text: 'You will see a countdown; if they do not come back the game is conceded for them.' },
      { kind: 'h', text: 'There are no Lab links' },
      { kind: 'p', text: 'They only appear in development builds.' },
      { kind: 'h', text: 'Something looks wrong with a card' },
      { kind: 'p', text: 'Open the game’s replay, find the frame, and report it on Discord or GitHub (both linked in the home-screen footer) with the replay link.' },
    ],
    related: ['where-decks-live', 'ranked', 'yields', 'stops', 'disconnects', 'set-completion'],
  },
  {
    id: 'assay',
    section: 'cards',
    title: 'Argentum Assay',
    summary:
      'Argentum’s own reader for printed Magic text. It parses a card’s Oracle text into the engine’s model of that card — and because every grammar rule is written to run backwards too, it can print that model back out and check the two match word for word.',
    body: [
      { kind: 'p', text: 'Cards here are written by hand and each one carries its own test, and that is not changing. Assay answers the question that comes before writing one: does the engine already have the vocabulary this card needs? A line it reads is a line the engine can already express. A line it cannot is declined — and a decline names the exact word it stopped at instead of quietly approximating the rest.' },
      { kind: 'p', text: 'Running the grammar backwards is what makes that reading worth trusting, because it can be checked at the scale of the whole corpus without anyone reading the output:' },
      { kind: 'ul', items: [
        'The touchstone parses every distinct piece of Oracle text Scryfall publishes and prints it back. A rule that quietly drops an “other”, or flattens “up to three” into “three”, fails on the next card that uses it.',
        'The differential sets Assay’s reading of an already-implemented card against the definition someone hand-wrote for it. A disagreement is a bug in one of the two — and it has turned up both kinds.',
      ] },
      { kind: 'p', text: 'Coverage is reported as fineness, in parts per thousand, after the assay a metallurgist runs on ore — the name is the promise that it reports a number rather than “looks fine”. Text it cannot read is counted, never dropped.' },
      { kind: 'p', text: 'Two things in this app are built on it: the ⚡ Assay-ready counts on Set Completion, and the Assay Explorer tab beside them.' },
    ],
    related: ['assay-ready', 'assay-explorer', 'assay-custom-cards', 'set-completion'],
  },
  {
    id: 'assay-explorer',
    section: 'cards',
    title: 'The Assay Explorer',
    summary:
      'The second tab on Set Completion. It runs against the live parser and answers three things: how much of Magic’s printed text the grammar reads today, what is blocking the rest, and what any text you paste into it parses to.',
    body: [
      { kind: 'p', text: 'What each of its views is for:' },
      { kind: 'ul', items: [
        'Overview — the fineness numbers: how much of the corpus round-trips, how much is declined, and how many cards are read whole rather than only in part.',
        'Cards — the sweep, browsable by set or across the whole corpus, each card showing the lines that read and the ones that did not. An AI column marks which implemented cards the built-in AI reads fully, and the AI gaps filter lists the ones it does not.',
        'Declines — the ranked gap report: which missing piece of grammar blocks the most real cards. This is the backlog putting itself in order.',
        'Grammar — every rule reachable from an ability line, with how many cards actually use each one.',
        'Differential — Assay’s reading of an implemented card set against the hand-written definition, and every place the two disagree.',
        'Parse — paste any Oracle text and read what it becomes, with a caret on the word a decline stopped at.',
      ] },
      { kind: 'p', text: 'The explorer only ever reads public card text — no game, no account, no deck — so it is mounted on every server rather than being a dev-build tool. The first time anyone opens it the server fetches Scryfall’s bulk Oracle data and sweeps it, so the numbers arrive shortly after the page does; the parser and the grammar tree work straight away either way.' },
      { kind: 'p', text: 'Opening a card shows the printed text, Assay’s reading of each line, and below them an AI reading panel: what the built-in AI understands of each of the card’s abilities.' },
      { kind: 'p', text: 'Differential is the exception. It compares against the project’s own test fixtures, which a deployed server does not ship, so here it reports that it has nothing to compare against. It fills in when the explorer is run from a checkout of the repository.' },
    ],
    related: ['assay', 'assay-ready', 'set-completion', 'ai-reading'],
  },
  {
    id: 'ai-reading',
    section: 'cards',
    title: 'What the AI understands of a card',
    summary:
      'The built-in AI opponent reads each card through a handful of readers before it plays it. The AI reading panel on an Assay Explorer card page shows, ability by ability, which parts it understands and which it falls back to a generic default on.',
    body: [
      { kind: 'p', text: 'The readers answer questions like: does this target help or hurt the creature it lands on, so should it go on mine or on yours? What is this card for — removal, a pump, card draw? Can combat see this pump coming? Is there a good moment to spend this until-end-of-turn effect?' },
      { kind: 'ul', items: [
        'Each of the card’s spells and abilities gets a row, marked ✓ read when every reader understood it and ✕ gaps when one did not, with the reason in plain words — “target 1: can’t tell if this helps or hurts”.',
        'The chip at the top sums it up: “AI reads 3/4 abilities”.',
        'A missing card that Assay reads whole gets a preview instead: what the AI would understand if the card were implemented the way Assay reads it.',
        'Static abilities are listed but not checked; the AI sees their effect on the board.',
      ] },
      { kind: 'p', text: 'A gap does not stop the card from working. The AI still plays it, using a generic default for that part, which is where it is most likely to misjudge the card.' },
      { kind: 'p', text: 'Every set on Set Completion gets an AI-reading score: the share of its implemented cards the AI reads with no gaps at all. The tag is banded so the sets needing attention stand out — ✓ 75% or more, ◐ 55–74%, ! under 55% — and sorting by “Least AI-read” puts the weakest first.' },
      { kind: 'ul', items: [
        'Click a set’s score, or its AI gaps tab, to see the cards the AI does not fully read, grouped by what it misses — “doesn’t know what the effect is for: AddCountersEffect”. One fix to the AI covers every card in a group, so the biggest groups are the work worth doing first.',
        'The panel also gives a second, gentler number: how many of the set’s individual abilities are read.',
        'Pick a group to narrow the card grid to it, or click a card name to open it in the Assay Explorer at its AI reading.',
      ] },
    ],
    related: ['assay-explorer', 'set-completion'],
  },
  {
    id: 'assay-custom-cards',
    section: 'advanced',
    title: 'Custom cards',
    summary:
      'The Scenario Builder can play a card that does not exist: paste a Scryfall-shaped card object, Assay reads it, and once it reads every line you can drop the compiled card into any zone and play with it. A dev-build tool — it needs endpoints a production deployment does not expose.',
    body: [
      { kind: 'p', text: 'Each printed line comes back with a verdict of its own — read, read (respelled), not read — and a caret on the word a decline stopped at. The card only becomes addable once every line reads: a half-read card would sit on the board looking correct and be quietly missing an ability, which is worse than being refused outright.' },
      { kind: 'p', text: 'None of this touches the card corpus. The compiled card is scoped to that one scenario session, and cards that actually ship are still written by hand and still carry their own test — Assay makes a draft quick to trust, it does not make the review optional.' },
    ],
    related: ['lab-tools', 'assay'],
  },
]

export function topicById(id: string): HelpTopic | undefined {
  return HELP_TOPICS.find((t) => t.id === id)
}

export function topicsInSection(section: HelpSection): readonly HelpTopic[] {
  return HELP_TOPICS.filter((t) => t.section === section)
}

export function sectionMeta(section: HelpSection) {
  // Non-null: `HelpSection` is exactly the set of ids in HELP_SECTIONS.
  return HELP_SECTIONS.find((s) => s.id === section)!
}

/**
 * Everything about a topic that a reader might type into the search box, lower-cased once.
 *
 * Includes the section's own title and blurb (so "modes" finds the mode topics) and, for the
 * shortcuts block, the whole shortcut table — otherwise searching "escape" or "spectate" would miss
 * the one topic that actually documents those keys.
 */
function topicHaystack(topic: HelpTopic): string {
  const meta = sectionMeta(topic.section)
  const parts: string[] = [topic.title, topic.summary, meta.title, meta.blurb]
  for (const block of topic.body ?? []) {
    if (block.kind === 'p' || block.kind === 'h' || block.kind === 'tip') parts.push(block.text)
    else if (block.kind === 'ul' || block.kind === 'ol') parts.push(...block.items)
    else if (block.kind === 'table') parts.push(...block.head, ...block.rows.flat())
    else for (const s of SHORTCUTS) parts.push(s.keys, s.label, s.where)
  }
  return parts.join(' ').toLowerCase()
}

const HAYSTACKS = new Map(HELP_TOPICS.map((t) => [t.id, topicHaystack(t)]))

/** Topics matching every whitespace-separated term in `query`, in registry order. */
export function searchTopics(query: string): readonly HelpTopic[] {
  const terms = query.toLowerCase().split(/\s+/).filter(Boolean)
  if (terms.length === 0) return []
  return HELP_TOPICS.filter((t) => {
    const haystack = HAYSTACKS.get(t.id) ?? ''
    return terms.every((term) => haystack.includes(term))
  })
}

/** Deep link to a topic on the help page. */
export function helpHref(topic: HelpTopic): string {
  return `/help/${topic.section}#${topic.id}`
}
