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

export type HelpSection = 'getting-started' | 'modes' | 'playing' | 'decks' | 'advanced'

export type HelpBlock =
  | { kind: 'p'; text: string }
  | { kind: 'ul'; items: readonly string[] }
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
    blurb: 'Your first five minutes: a name, a game, and where your decks live.',
  },
  {
    id: 'modes',
    title: 'Game modes',
    blurb: 'Three independent choices — Cards, Table, Event — and the three questions that set them.',
  },
  {
    id: 'playing',
    title: 'Playing a game',
    blurb: 'Priority, stops, tapping, yields and the board controls.',
  },
  {
    id: 'decks',
    title: 'Decks',
    blurb: 'Building, importing, sharing, and building from a limited pool.',
  },
  {
    id: 'advanced',
    title: 'Advanced',
    blurb: 'Shortcuts, replays, spectating, the multiplayer camera and the Lab tools.',
  },
]

export const HELP_TOPICS: readonly HelpTopic[] = [
  // ── Getting started ────────────────────────────────────────────────────
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
      'Answer the three questions under PLAY — who with, what with, how — and you land in a lobby set up that way. You can change all of it once you are inside.',
    body: [
      { kind: 'p', text: '“Just me” plus “Bring a deck” is the shortest path: two clicks, then pick a deck and ready up. The third question is skipped whenever there is only one possible answer.' },
      { kind: 'p', text: 'Everything you start ends in a lobby, and every lobby shows the same three settings. Nothing is a dead end — a lobby you opened as a 1v1 can become a four-player game without going back to the menu.' },
    ],
    related: ['play-wizard', 'axes', 'invite-codes'],
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

  // ── Game modes ─────────────────────────────────────────────────────────
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
      { kind: 'p', text: 'The home screen asks about Cards, Table and Event and picks the Rules to match (a Commander draft or sealed means Commander rules); the lobby then lets you change any of the four, Rules included.' },
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
    summary: 'Two players, 20 life each. The default for every preset except Multiplayer.',
  },
  {
    id: 'table-free-for-all',
    section: 'modes',
    title: 'Table: Free-for-All',
    summary:
      'One game, everyone at the same table (2–6 players). Last player standing wins.',
    body: [
      { kind: 'p', text: 'The host chooses who each creature may attack: any opponent (CR 802), or only the player to your left or right (CR 803). “Left” and “right” follow the seating order shown in the lobby.' },
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
    section: 'modes',
    title: 'Ranked play',
    summary:
      'Ranked games adjust each player’s ELO. Every player must be signed in — otherwise the game still runs, but silently counts as unranked.',
    body: [
      { kind: 'p', text: 'Ranked is currently available on 1v1 games only. Multiplayer tables are always casual.' },
    ],
    related: ['guest-vs-account', 'axis-limits'],
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
    id: 'play-wizard',
    section: 'modes',
    title: 'Starting a game: three questions',
    summary:
      'The home screen asks who you are playing with, what you are playing with, and how it is played. The answers decide which lobby you get; everything stays changeable once you are in it.',
    body: [
      { kind: 'p', text: 'Who you are playing with comes first because it rules out the most: a rolled random pool or a Momir game only exists as a 1v1, and Commander AI needs a deck chosen by the host.' },
      { kind: 'p', text: 'All three questions stay on screen, numbered, with your answer under each. Click an answer to go back and change it; the answers after it are re-checked against the change.' },
      { kind: 'ul', items: [
        'A question with only one possible answer is decided for you and marked “auto”.',
        'Each option says whether it is one game or an event with steps — “Play right away” and “One game” versus “Build a deck first” and “Several rounds · standings”. Once everything is answered, the line above the button spells the whole sequence out.',
        'Greyed-out options are combinations nothing implements yet — hover for the reason.',
        'Options that are simply missing would contradict an earlier answer. A group of five is not offered a 1v1 single game.',
        'Nobody is asked how many seats to open. A lobby holds as many as its table allows, people join until it is full, and the host starts whenever everyone has arrived.',
        'The lobby you land in can change all of it, so a wrong turn costs nothing.',
        '“Play again” repeats your last setup in one click.',
      ] },
    ],
    related: ['axes', 'axis-limits', 'roster-solo', 'roster-friend', 'roster-group'],
  },
  {
    id: 'roster-solo',
    section: 'modes',
    title: 'Just me',
    summary:
      'You and the built-in AI. Nobody else has to show up, and the game starts as soon as you have picked a deck.',
    body: [
      { kind: 'p', text: 'At 1v1 the AI can play any of the card sources — your own deck, a rolled pool, or Momir Basic.' },
      { kind: 'p', text: 'Every table is open too: the lobby starts with a useful AI roster — four players for a shared table, six for a limited round robin, and four for a brought-deck round robin. You can add or remove AI seats in the lobby without making the maximum capacity the default.' },
      { kind: 'p', text: 'The AI builds from the pool it is dealt, and in a lobby where everyone brings a deck the server normally rolls it one. Commander is the exception: choose a saved, example, or pasted Commander deck for the AI so its commander can start in the command zone.' },
    ],
    related: ['axis-limits', 'cards-draft', 'event-round-robin'],
  },
  {
    id: 'roster-friend',
    section: 'modes',
    title: 'A friend',
    summary:
      'One human opponent. You get an invite code and a QR code to share; when they join, both of you pick a deck and ready up.',
    related: ['invite-codes', 'ranked', 'event-single-game'],
  },
  {
    id: 'roster-group',
    section: 'modes',
    title: 'A group',
    summary:
      'Three to eight players. Either one shared game — Free-for-All, Two-Headed Giant or Team vs. Team — or a round-robin bracket of 1v1 matches with standings.',
    body: [
      { kind: 'p', text: 'Everyone joins with the same invite code. The host sets the number of seats and can start once they are filled.' },
      { kind: 'p', text: 'A group can bring their own decks or share a limited pool: sealed and draft both work at a multiplayer table as well as in a bracket.' },
    ],
    related: ['table-free-for-all', 'table-two-headed-giant', 'table-team-vs-team', 'event-round-robin'],
  },

  // ── Playing a game ─────────────────────────────────────────────────────
  {
    id: 'priority-modes',
    section: 'playing',
    title: 'Priority modes: Auto, Stops, Full Control',
    summary:
      'Auto passes for you whenever you have nothing worth doing. Stops pauses on opponent spells and abilities and on combat damage. Full Control gives you priority at every single step.',
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
    id: 'targeting-and-combat',
    section: 'playing',
    title: 'Targeting, attacking and blocking',
    summary:
      'Drag a card from your hand onto the battlefield to cast it, drag an attacker onto a defender to attack, and drag a blocker onto an attacker to block. Clicking works everywhere dragging does.',
    body: [
      { kind: 'p', text: 'Dragging one attacker onto another bands them (CR 702.22). On a phone, swipe left and right on the opponent strip to move between boards.' },
    ],
    related: ['multiplayer-camera'],
  },
  {
    id: 'zone-browsers',
    section: 'playing',
    title: 'Browsing zones',
    summary:
      'Click a graveyard, exile or library pile to open a full browser of its contents. Press D to open the deck browser, which tracks what is left in your library.',
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
  },

  // ── Decks ──────────────────────────────────────────────────────────────
  {
    id: 'deckbuilder',
    section: 'decks',
    title: 'The deckbuilder',
    summary:
      'Search the full implemented card pool, click to add a copy, right-click or shift-click to remove one. Decks save to this browser, or to your account when signed in.',
    shortcuts: ['deckbuilder-remove', 'flip-dfc'],
    related: ['search-syntax', 'deck-sharing'],
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
      { kind: 'p', text: 'Section headers are case-insensitive: `Deck` / `Mainboard` / `Main Deck` / `Maindeck`, `Sideboard` / `Side` / `SB`, `Commander` / `Commanders` / `EDH`, `Companion`, and `About`. Only the main deck and commander are imported. Blank lines and lines starting with `//` or `#` are ignored.' },
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
    related: ['deckbuilder'],
  },
  {
    id: 'limited-deckbuilding',
    section: 'decks',
    title: 'Building from a sealed or drafted pool',
    summary:
      'After a draft or sealed opening you get a dedicated builder over just your pool: add cards, set basic-land counts, and submit. Standard limited wants at least 40 cards including lands.',
    body: [
      { kind: 'p', text: 'Anything you leave out of the deck stays available as your sideboard between games in a match. You can save a drafted deck to My Decks from the standings screen — the printings you actually drafted are preserved.' },
    ],
    related: ['cards-sealed', 'cards-draft', 'cube-pool-play'],
  },
  {
    id: 'cube-building',
    section: 'decks',
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
  {
    id: 'set-completion',
    section: 'decks',
    title: 'Which cards are implemented',
    summary:
      'Set Completion, on the home screen under Build & Browse, lists every set and how much of it the engine can actually play. Useful before committing to a deck or picking a set to draft.',
    body: [
      { kind: 'p', text: 'The deckbuilder only ever offers implemented cards, so a deck you build there always works. This page is the other direction: it tells you what is missing from a set you had in mind.' },
    ],
    related: ['deckbuilder'],
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
    section: 'advanced',
    title: 'Replays',
    summary:
      'Finished games can be replayed frame by frame. Scrub with the timeline, step with the arrow keys, play/pause with space.',
    body: [
      { kind: 'p', text: 'A replay can also be turned into a scenario: pick a frame, hand it to the Scenario Builder, and start a fresh game from exactly that board state.' },
    ],
    shortcuts: ['replay-frame', 'replay-play', 'escape'],
    related: ['lab-tools'],
  },
  {
    id: 'spectating',
    section: 'advanced',
    title: 'Spectating',
    summary:
      'Live games are listed on the home screen and inside tournaments. A spectator sees both boards and steers the same camera a player does — but never sees a hidden zone.',
    related: ['multiplayer-camera'],
  },
  {
    id: 'multiplayer-camera',
    section: 'advanced',
    title: 'Multiplayer camera: Overview, Follow, pin',
    summary:
      'Overview shows every opponent board side by side; turn it off to focus one board at a time. Follow slides the view to whoever is acting; turn it off for a manual camera.',
    body: [
      { kind: 'p', text: 'Number keys 1–9 jump to an opponent’s board and 0 toggles the overview. Clicking an opponent chip pins that board until you press Esc.' },
      { kind: 'p', text: 'Overview is desktop and landscape-tablet only — three boards side by side are unusable on a portrait phone.' },
    ],
    shortcuts: ['opponent-boards', 'overview', 'escape'],
    related: ['table-free-for-all'],
  },
  {
    id: 'lab-tools',
    section: 'advanced',
    title: 'Lab tools',
    summary:
      'Debugging and content tools, not part of normal play: the Scenario Builder (start a game from a hand-authored board state), the LLM Tournament runner, and the AI Sandbox (a table of bots playing each other, so you can watch the built-in AI and spot where it goes wrong). They only appear in dev builds, because they all need server endpoints a production deployment does not expose.',
    related: ['replays', 'set-completion'],
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
    if (block.kind === 'p') parts.push(block.text)
    else if (block.kind === 'ul') parts.push(...block.items)
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
