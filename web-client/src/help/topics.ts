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
    blurb: 'Which cards and sets are playable, and Argentum Assay — the engine’s reader for Oracle text.',
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
        'Deckbuilder — build, import and save decks.',
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
        'Public Lobbies — open lobbies anyone may join, with an online-player count. Refreshes every few seconds.',
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
      'Guests can play everything. An account (one magic link, no password) adds decks that follow you between devices, friends, ranked play, stats and saved replays.',
    body: [
      { kind: 'p', text: 'Signing in later keeps the decks you built as a guest — you are offered a one-click migration.' },
      { kind: 'ul', items: [
        'Decks saved to the account instead of this browser',
        'Friends list and online presence',
        'Ranked games (every player in the game must be signed in, otherwise it silently plays unranked)',
        'Full stats dashboard and permanent replays',
      ] },
    ],
    related: ['ranked', 'replays'],
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
      'Saved decks live in this browser until you sign in, and in your account afterwards. The deckbuilder’s My Decks list is the same list every lobby deck picker reads from.',
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
      { kind: 'p', text: 'Anything the panel does not ask keeps its default — the timer, pack count and so on — and stays editable in the lobby until you start. AI games skip the lobby entirely.' },
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
      'Play a deck you built or pasted. Optionally restrict everyone to a constructed format — Standard, Pioneer, Modern, Pauper, Legacy, Vintage, Commander, Brawl, Standard Brawl or Premodern.',
    body: [
      { kind: 'p', text: '“No restriction” lets any legal-in-the-engine card in. The restriction is checked when a deck is submitted, not when it is built.' },
    ],
    related: ['deckbuilder'],
  },
  {
    id: 'cards-random',
    section: 'modes',
    title: 'Cards: Random pool',
    summary:
      'The server picks a deck for you. The fastest route from a cold start to actually playing — no deckbuilding, no pool to sort.',
  },
  {
    id: 'cards-momir',
    section: 'modes',
    title: 'Cards: Momir Basic',
    summary:
      'No deckbuilding at all. Everyone runs 60 basic lands; discard a card and pay {X} to put a random creature with mana value X onto the battlefield.',
    body: [
      { kind: 'p', text: 'The Momir Vig avatar sits in the command zone. Creatures are rolled from every implemented set, so games look nothing alike.' },
    ],
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
      'A cube is your own hand-picked card pool used instead of real sets. Any limited format can run on it — Sealed, Booster, Winston or Grid draft — and packs are dealt from the cube instead of from boosters.',
    body: [
      { kind: 'p', text: 'A cube replaces the set picker: pick a cube in the lobby and the Sets, booster-mix and per-set pack controls disappear, because there are no sets involved any more. Everything else about the format is unchanged — the same pick timers, the same deck building, the same bracket.' },
      { kind: 'p', text: 'Packs are dealt without replacement across the whole event. No card ever appears twice — not in two of your packs, and not in two different players’ pools. That is what makes a cube a curated pool rather than a set: every card in it is a deliberate choice, and each one shows up exactly as often as you put it in.' },
      { kind: 'ul', items: [
        'Pack size is part of the cube (15 by default, the community standard).',
        'Capacity is the one new constraint: players × packs × pack size must fit inside the cube. A 360-card cube seats 8 players at 3 packs of 15 exactly, and the lobby shows the sum live.',
        'Basic lands aren’t part of a cube — it names a set to take their art from, and the deckbuilder gives you unlimited basics as usual.',
        'The host’s ban list still applies, and banned cards come off the cube before capacity is counted.',
      ] },
      { kind: 'p', text: 'Cube Pool Play is the exception to all the dealing: it skips packs entirely. See its own topic.' },
    ],
    related: ['cube-building', 'cube-pool-play', 'cards-sealed', 'cards-draft'],
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
        'No sideboard. In limited your sideboard is everything you didn’t play, but here that would be the whole cube, so Pool Play decks have none — which also means cards that fetch from outside the game have nothing to find.',
        'Pack size and pack count are ignored, and the controls for them are hidden.',
      ] },
      { kind: 'p', text: 'Set it on a cube Sealed lobby with the Card pool control: “Sealed packs” deals pools from the cube as usual, “Pool Play” gives everyone all of it.' },
    ],
    related: ['cards-cube', 'cube-building', 'cards-sealed', 'limited-deckbuilding'],
  },
  {
    id: 'rules-standard',
    section: 'modes',
    title: 'Rules: Standard',
    summary:
      'The ordinary rules for whichever table you picked — 20 life at 1v1, no command zone, and 4 copies of a card if the deck legality allows it.',
    related: ['rules-commander', 'axes'],
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
    ],
    related: ['rules-standard', 'axes', 'axis-limits', 'cards-bring-a-deck'],
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
      { kind: 'p', text: 'The host chooses who each creature may attack: only the player to your left (the default) or right (CR 803), or any opponent (CR 802). “Left” and “right” follow the seating order shown in the lobby. In game, the seat list names the rule, tags the one opponent you can attack (⚔ TARGET) and the one who can attack you (🛡 ATTACKS YOU), and dims everyone you can’t attack while you declare attackers.' },
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
    related: ['guest-vs-account', 'axis-limits', 'stats', 'matchmaking'],
  },
  {
    id: 'profile',
    section: 'community',
    title: 'Your profile',
    summary:
      'Profile (from the account menu) shows your record at a glance, your recent games with both decks and their replays, and your recent tournaments. You can change your display name here.',
    body: [
      { kind: 'ul', items: [
        'Games, wins, losses and win rate.',
        'Recent games, ten to a page — open Decks to see both players’ lists, Watch the replay, or Share its link.',
        'Recent tournaments, each opening its final standings and replays.',
        'Shortcuts to Full stats, My decks and Friends.',
      ] },
      { kind: 'p', text: 'Everyone also has a public profile at `/u/<id>` that anyone can open without signing in. It shows the same statistics and recent games but never your decklists. “This is how others see your profile” marks your own.' },
    ],
    related: ['stats', 'friends', 'guest-vs-account'],
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
        'Your friends — online or offline, a link to their profile, and Unfriend.',
        'Hide my online status — friends will always see you as offline.',
        'Blocked — players you blocked from a result screen, with Unblock.',
      ] },
      { kind: 'p', text: 'After a game against someone, you can also add them from the result screen.' },
      { kind: 'p', text: 'To play a friend, send them your lobby’s invite code or link; there is no in-app invite.' },
    ],
    related: ['roster-friend', 'profile', 'invite-codes', 'after-the-game'],
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
    related: ['matchmaking', 'friends', 'emotes'],
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
    related: ['axes', 'lobby-switching', 'saved-setups', 'roster-solo', 'invite-codes'],
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
      { kind: 'p', text: 'Everyone joins with the same invite code, or from Public Lobbies on the home screen when the lobby’s visibility is Public. A lobby holds as many seats as its table allows; the host starts once everyone has arrived.' },
      { kind: 'p', text: 'A group can bring their own decks or share a limited pool: sealed and draft both work at a multiplayer table as well as in a bracket.' },
    ],
    related: ['table-free-for-all', 'table-two-headed-giant', 'table-team-vs-team', 'event-round-robin', 'lobby-screen'],
  },

  // ── Draft & Sealed ─────────────────────────────────────────────────────
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
      'With the lobby’s AI assistance set to On, the draft screen gets a ✨ Suggest Pick button and the deck builder gets 🪄 Auto-build and ✨ Score cards. It is a learning aid; the host decides whether it is allowed.',
    body: [
      { kind: 'ul', items: [
        'Suggest Pick scores every card in the pack from 0 to 100 — green at 70 and up, amber from 45 — with a one-line reason on hover, and marks the top one “⭐ Best”. Re-suggest asks again; Clear hides the scores.',
        'Auto-build proposes the best deck it can make from your pool, lands included. If you have already started, it becomes Complete Deck and fills the gaps.',
        '“Builds:” chips switch between the candidate decks it found.',
        'Score cards rates every card in your pool so you can make the cuts yourself.',
      ] },
      { kind: 'tip', text: 'Assistance is off by default, so nobody in a competitive pod gets it unless the host turns it on for everyone.' },
    ],
    related: ['draft-picking', 'limited-deckbuilding', 'deck-advisor'],
  },
  {
    id: 'deck-advisor',
    section: 'limited',
    title: 'The deck advisor',
    summary:
      'In the limited deck builder, “✨ Ask the deck advisor” opens a chat with an AI that can see your pool. Ask it questions, or ask it to highlight cards or build the deck for you.',
    body: [
      { kind: 'p', text: 'Try “highlight all removal”, “Is my mana curve okay?”, “Should I splash a third color?” or “Build me a control deck with lots of removal”. It can highlight pool cards, clear highlights, or replace your whole deck — lands included — and you can undo any of it by hand.' },
      { kind: 'p', text: 'Enter sends, Shift+Enter adds a new line. The conversation is not saved; reloading starts a fresh one.' },
    ],
    related: ['limited-deckbuilding', 'draft-assist'],
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
        'In a Commander pool, the crown on a legendary creature or planeswalker makes it your commander, and a toggle restricts the pool to its colour identity.',
      ] },
      { kind: 'p', text: 'Submit Deck stays disabled until the deck is big enough, and says how many cards it still needs. After submitting you can still press Edit Deck until the first round begins. Export gives an MTG Arena-format list to copy or download.' },
      { kind: 'p', text: 'The cards you leave out become your sideboard — the “outside the game” cards that wish-style effects can fetch. There is no sideboarding between games: the deck you submit plays every round.' },
      { kind: 'p', text: 'Save Deck on the standings screen keeps the deck in My Decks, with the exact printings you opened or drafted.' },
    ],
    related: ['cards-sealed', 'cards-draft', 'draft-assist', 'deck-advisor', 'cube-pool-play'],
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
    related: ['event-round-robin', 'limited-deckbuilding', 'spectating', 'replays', 'disconnects'],
  },
  {
    id: 'cube-building',
    section: 'limited',
    title: 'Building a cube',
    summary:
      'Build a cube from the lobby’s Cube panel: paste a list or search for cards, set the pack size, and save it. Cubes live alongside your decks — in this browser as a guest, in your account when signed in.',
    body: [
      { kind: 'p', text: 'A cube is a list of card names with counts, so it isn’t tied to any set or printing. Two ways to fill one:' },
      { kind: 'ul', items: [
        'Paste a list — plain text, MTG Arena or Moxfield format, one “count name” per line. The same parser the deckbuilder’s import uses.',
        'Search and add — the same query language as the deckbuilder search, so “c:red t:creature cmc<=3” works.',
      ] },
      { kind: 'p', text: 'Cubes are usually singleton (one of each), but counts are yours to set — the editor lets any card run as many copies as you want, and each copy is a separate physical card when packs are dealt.' },
      { kind: 'p', text: 'A cube whose names are all implemented is playable; one with cards the engine doesn’t have yet is not, and the editor says so in red with a one-click “drop the unimplemented cards”. You can still save a cube in that state — sets keep landing, so a cube naming a card that arrives next month is worth keeping — but a lobby won’t accept it until it resolves cleanly.' },
      { kind: 'p', text: 'The editor also shows the colour spread and mana curve as you go, which are what a cube is usually balanced on.' },
    ],
    related: ['cards-cube', 'cube-pool-play', 'where-decks-live', 'search-syntax', 'deck-import-export'],
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
    related: ['game-table', 'pass-button'],
  },
  {
    id: 'game-table',
    section: 'playing',
    title: 'Reading the table',
    summary:
      'Your board is at the bottom, opponents at the top, the stack in the middle. Each player has a life orb, their zone piles, and badges for anything that changes the usual rules — poison, energy, hand size, commander damage, day or night.',
    body: [
      { kind: 'h', text: 'Around each life total' },
      { kind: 'ul', items: [
        'POISON n/10 — appears once a player has poison counters. A Two-Headed Giant team loses at 15.',
        '⚡ n — energy counters.',
        'HAND — the maximum hand size, only when it is not 7.',
        '⚔ commander damage — taken from each commander, against the 21 (or preset) threshold; it turns red within 5 of lethal.',
        'Chips for active effects on that player — hover one to see what it does. The City’s Blessing, the Ring’s temptation and Speed have their own badges.',
      ] },
      { kind: 'h', text: 'Elsewhere' },
      { kind: 'ul', items: [
        'The phase bar across the top, with the turn number and whose turn it is.',
        'Day / Night — a ☀ or ☾ badge beside it once a day/night card has started the cycle.',
        'Command — the command zone, holding your commander; its cost badge already includes commander tax.',
        'Deck, Graveyard and Exile piles, plus Plotted, Suspended and similar piles when a card uses one.',
        'Stack (N) — what is waiting to resolve, newest on top. Identical items can be collapsed.',
        'An eye badge, top left, counts spectators; hover it for their names.',
      ] },
      { kind: 'p', text: 'Opponent decisions are shown as they happen — “<name> is choosing…”, with the card that asked.' },
    ],
    related: ['phase-bar', 'card-badges', 'zone-browsers', 'card-preview', 'multiplayer-camera'],
  },
  {
    id: 'emotes',
    section: 'playing',
    title: 'Emotes',
    summary:
      'The speech-bubble button beside your life total sends your opponent a quick message — a greeting, a reaction, or a bit of table talk.',
    body: [
      { kind: 'table', head: ['Group', 'Says'], rows: [
        ['Hello & goodbye', 'Hello! · Good luck, have fun! · Thanks! · Good game!'],
        ['Reactions', 'Well played. · Love this deck! · Ooh, nice combo! · Of course you drew that. · Didn’t see that coming! · Ouch.'],
        ['Table talk', 'Doing the math… · Oops. You saw nothing. · Land. Please. Any land. · Is that all you’ve got?'],
      ] },
      { kind: 'p', text: 'Messages appear as a bubble over the sender’s life total for a few seconds. There is no free typing, and a short cooldown keeps it friendly. The AI answers a greeting, and says thanks for a compliment.' },
      { kind: 'tip', text: 'Not in the mood? Switch off “Show their emotes” at the bottom of the emote menu to mute an opponent for the rest of the game.' },
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
      { kind: 'p', text: 'The button cycles Auto → Stops → Full Control. Auto is right for most games; switch to Full Control when you need a specific window, such as responding in your own upkeep.' },
      { kind: 'p', text: 'Auto never passes when you have a decision that matters — it is a convenience, not a rules shortcut.' },
    ],
    related: ['stops', 'yields'],
  },
  {
    id: 'stops',
    section: 'playing',
    title: 'Stops on the phase bar',
    summary:
      'Hover a step on the phase bar to reveal two dots: a blue “my turn” stop and an amber “opponent turn” stop. Click one and you will always get priority at that step.',
    body: [
      { kind: 'p', text: 'Stops are saved in this browser and apply to every game you play.' },
    ],
    related: ['priority-modes', 'phase-bar'],
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
    related: ['priority-modes'],
  },
  {
    id: 'mana-payment',
    section: 'playing',
    title: 'Paying costs by hand',
    summary:
      'With Manual Tap — or by choosing “Choose which lands to tap” in a card’s action menu — you pay a cost yourself: click highlighted mana sources until every pip is covered, then press Pay.',
    body: [
      { kind: 'p', text: 'Each pip of the cost shows whether it is paid from your mana pool, covered by a source you selected, or not yet covered. Auto Pay hands the rest to the auto-tapper; Decline backs out. If paying would sacrifice something, a warning says so first.' },
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
        'Divide damage — assign amounts to each target; “Lethal” marks the point where a creature dies.',
        'Combat damage — with trample, deathtouch or several blockers, you assign attacking damage yourself.',
        'Order — damage assignment order and cards going to the top of a library get arrow buttons.',
        'Search — pick cards from a library, or Fail to Find.',
        'Piles — split cards into two piles, or assign them randomly.',
        'Options — longer lists (a card name, a creature type) come with a search box.',
      ] },
      { kind: 'p', text: 'A strip above each prompt names the cards involved — the source, its target, what triggered it — so a long chain of triggers stays readable.' },
    ],
    related: ['targeting-and-combat', 'yields', 'mana-payment'],
  },
  {
    id: 'yields',
    section: 'playing',
    title: 'Yields — stop being asked',
    summary:
      'Right-click (or long-press) an ability on the stack to open its yield menu: yield until end of turn, always yield, always answer Yes, always answer No, or revoke.',
    body: [
      { kind: 'p', text: 'This is the fix for a repeating optional trigger asking you the same question every turn. Active yields are listed in a panel while they are in force, so you can revoke one at any time.' },
    ],
    shortcuts: ['stack-yield-menu'],
    related: ['priority-modes'],
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
    related: ['targeting-and-combat', 'zone-browsers', 'card-badges'],
  },
  {
    id: 'targeting-and-combat',
    section: 'playing',
    title: 'Casting, attacking and blocking',
    summary:
      'Drag a card from your hand onto the battlefield to cast it, drag an attacker onto a defender to attack, and drag a blocker onto an attacker to block. Clicking works everywhere dragging does.',
    body: [
      { kind: 'p', text: 'Clicking a card with more than one way to play opens its action menu — Cast, Play, Turn Face-Up, an activated ability, or View card. A permanent’s abilities are in the same menu.' },
      { kind: 'p', text: 'Attacking: Attack All sends every creature that can attack, Skip Attacking declares none, and after choosing some the button reads Attack with N. In multiplayer each attacker needs a defender; the banner says who you are allowed to attack.' },
      { kind: 'p', text: 'Blocking: drag each blocker to an attacker, then Confirm Blocks — or No Blocks. Clear resets your choices.' },
      { kind: 'p', text: 'Dragging one attacker onto another bands them (CR 702.22). On a phone, swipe left and right on the opponent strip to move between boards.' },
    ],
    related: ['pass-button', 'decisions', 'multiplayer-camera'],
  },
  {
    id: 'zone-browsers',
    section: 'playing',
    title: 'Browsing zones',
    summary:
      'Click a graveyard, exile or library pile to open a full browser of its contents. Press D to open the deck browser, which tracks what is left in your library.',
    body: [
      { kind: 'p', text: 'The deck browser has two views: Deck list (everything still in your library) and Library order, which shows cards whose position you know — after a scry, for instance — with the top card marked “Next draw”.' },
    ],
    shortcuts: ['deck-browser', 'escape'],
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
    ],
    related: ['replays', 'tournament-flow', 'disconnects'],
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
        'The sideboard drop zone holds cards kept alongside the deck. Most decks leave it empty.',
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
    related: ['search-syntax', 'card-printings', 'deck-legality', 'commander-decks', 'deck-sharing', 'where-decks-live'],
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
      'The deckbuilder’s format dropdown — Standard, Pioneer, Modern, Pauper, Legacy, Vintage, Commander, Brawl, Standard Brawl or Premodern — filters the catalogue to legal cards and checks the deck as you build: “Legal ✓” or “N issues”.',
    body: [
      { kind: 'p', text: 'Hover the issue count for the list. Individual rows are flagged too: not legal in the format, too many copies, singleton violations, cards outside the commander’s colour identity.' },
      { kind: 'p', text: 'A lobby can restrict everyone to a format, in which case the deck is checked again when you submit it. “No format” lets in anything the engine implements.' },
      { kind: 'p', text: 'A card that imported but is not implemented yet is kept as a placeholder and marked; it cannot be played until it is.' },
    ],
    related: ['deckbuilder', 'cards-bring-a-deck', 'set-completion'],
  },
  {
    id: 'commander-decks',
    section: 'decks',
    title: 'Commander decks',
    summary:
      'Click the crown on a legendary creature or planeswalker in your deck to make it the commander. Choose the Commander, Brawl or Standard Brawl format to have singleton and colour identity checked for you.',
    body: [
      { kind: 'p', text: 'Imported lists with a `Commander` section get their commander set automatically. A Commander deck shows up in the Commander mode’s deck picker and can be handed to an AI seat.' },
    ],
    related: ['rules-commander', 'deck-legality', 'deckbuilder'],
  },
  {
    id: 'search-syntax',
    section: 'decks',
    title: 'Search syntax',
    summary:
      'The deckbuilder search speaks a Scryfall-style query language — `t:creature`, `c<=rw`, `cmc>=4`, `o:flying`, `f:standard`, `is:legendary`. The `?` button beside the search box lists every operator with examples.',
    body: [
      { kind: 'p', text: 'The grammar is Scryfall’s, deliberately: bare words match names, `key:value` filters, `-` negates, `or` and parentheses group, quotes hold phrases together, and the comparison operators `:` `=` `>` `<` `>=` `<=` work wherever a value is ordered.' },
      { kind: 'ul', items: [
        'Colour — `c:rg`, `c:azorius`, `c<=rw`, `c:colorless`, and `id:` for colour identity.',
        'Type and text — `t:goblin`, `t:legendary`, `o:flying` for oracle text, `kw:trample` for keywords.',
        'Cost — `mv>=4` (`cmc` also works), `m:{2/G}` for a specific mana cost.',
        'Stats — `pow>=4`, `tou<2`, `loy:3`.',
        'Printing — `s:fdn` for a set, `r:mythic` for rarity.',
        'Legality — `f:standard`, `f:commander`, `f:pauper`.',
        'Flags — `is:legendary`, `is:permanent`, `is:multicolor`, `is:vanilla`, `is:dfc`.',
      ] },
      { kind: 'p', text: 'Not every filter Scryfall documents is implemented. Anything needing data the engine does not carry — prices, artists, printing dates — is rejected rather than silently ignored, and so is anything the card model does not distinguish yet: `is:split` answers “split-card layout not modelled”. A query never quietly means something other than what you typed.' },
    ],
    related: ['deckbuilder'],
    links: [{ label: 'Scryfall’s full syntax reference', href: 'https://scryfall.com/docs/syntax' }],
  },
  {
    id: 'deck-import-export',
    section: 'decks',
    title: 'Import and export',
    summary:
      'Paste an Arena-style decklist (`4 Lightning Bolt`) straight into the deckbuilder or a lobby deck picker, and export the same way.',
    body: [
      { kind: 'p', text: 'Arena, Moxfield and plain text are accepted interchangeably, so whatever your other tool exports should paste in as-is. These line shapes are recognised:' },
      { kind: 'ul', items: [
        '`4 Lightning Bolt` — plain.',
        '`4x Lightning Bolt` — the Moxfield “x”.',
        '`4 Lightning Bolt (LEA) 161` — Arena, with set code and collector number. Give these when you want a specific printing; without them you get the latest.',
        '`1 Cardname (SET) *F* *A* 42 #tag` — Moxfield bulk edit. Foil, alter and tag markers are read and discarded.',
        '`SB: 2 Counterspell` — the MTGO sideboard prefix.',
      ] },
      { kind: 'p', text: 'Section headers are case-insensitive: `Deck` / `Mainboard` / `Main Deck` / `Maindeck`, `Sideboard` / `Side` / `SB`, `Commander` / `Commanders` / `EDH`, `Companion`, and `About`. The main deck, the sideboard and the commander are all imported; `Companion` and `About` are skipped. Blank lines and lines starting with `//` or `#` are ignored.' },
      { kind: 'p', text: 'A line that looks like a card but cannot be matched is reported rather than dropped, so an import never silently loses cards. Export writes the plain `4 Lightning Bolt` shape, which every one of the above tools reads.' },
    ],
    related: ['deckbuilder', 'deck-sharing'],
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
    id: 'set-completion',
    section: 'cards',
    title: 'Which cards are implemented',
    summary:
      'Set Completion, on the home screen under Build & Browse, lists every set and how much of it the engine can actually play. Useful before committing to a deck or picking a set to draft.',
    body: [
      { kind: 'p', text: 'The deckbuilder only ever offers implemented cards, so a deck you build there always works. This page is the other direction: it tells you what is missing from a set you had in mind.' },
      { kind: 'p', text: 'Open a set to get its full card list, each card marked implemented or missing. A handful come back “not planned” — cards needing something the engine will never carry, like ante or physical dexterity — and those are left out of every total, so the percentage measures what there is any intention of building.' },
      { kind: 'ul', items: [
        'The banner counts distinct cards implemented, printings, complete sets and cards not planned; View progress charts implementation over time.',
        'Filter sets by name or code, and by All, Standard, In progress or Complete.',
        'Sort by Newest, Most complete, Most cards done, Most Assay-ready or A–Z.',
        'Inside a set, filter its cards by Implemented, Missing, ⚡ Assay-ready or Not planned.',
      ] },
      { kind: 'p', text: 'The second tab, Assay Explorer, is a different question about the same corpus: not which cards are written, but how much of Magic’s printed text the engine can read at all.' },
    ],
    related: ['deckbuilder', 'assay-ready', 'assay-explorer'],
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
      { kind: 'p', text: 'Replays in the top bar lists every game you finished, grouped by tournament, with who played, who won and how long it was. Replays are also linked from your profile, from a tournament’s Replays button, and from Watch Replay at the end of a game.' },
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
        'Cards — the sweep, browsable by set or across the whole corpus, each card showing the lines that read and the ones that did not.',
        'Declines — the ranked gap report: which missing piece of grammar blocks the most real cards. This is the backlog putting itself in order.',
        'Grammar — every rule reachable from an ability line, with how many cards actually use each one.',
        'Differential — Assay’s reading of an implemented card set against the hand-written definition, and every place the two disagree.',
        'Parse — paste any Oracle text and read what it becomes, with a caret on the word a decline stopped at.',
      ] },
      { kind: 'p', text: 'The explorer only ever reads public card text — no game, no account, no deck — so it is mounted on every server rather than being a dev-build tool. The first time anyone opens it the server fetches Scryfall’s bulk Oracle data and sweeps it, so the numbers arrive shortly after the page does; the parser and the grammar tree work straight away either way.' },
      { kind: 'p', text: 'Differential is the exception. It compares against the project’s own test fixtures, which a deployed server does not ship, so here it reports that it has nothing to compare against. It fills in when the explorer is run from a checkout of the repository.' },
    ],
    related: ['assay', 'assay-ready', 'set-completion'],
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
