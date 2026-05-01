# Standalone Deckbuilder

A proper deckbuilder accessible from the main menu, covering all implemented
cards, with query-language + menu search and full saved-deck management
(create, save, rename, delete).

It reuses the existing `useDeckLibrary` Zustand store, the `/api/decks/*`
endpoints, and the visual language of `DeckBuilderOverlay.tsx` — but is
decoupled from the sealed/draft flow (which is tied to `draftSlice` and a
fixed card pool).

## Scope

- New page at `/deckbuilder` and `/deckbuilder/:deckId`, accessible from the
  main menu (`ConnectionOverlay` in `web-client/src/components/ui/GameUI.tsx`).
- Card pool = **all implemented cards**, served by extending
  `GET /api/decks/cards`.
- Search via **query language** (Scryfall-ish) **and** a side panel of menu
  filters — both write into the same predicate.
- Save / load / rename / delete already exist on `useDeckLibrary`; no schema
  change needed.
- Validation via existing `POST /api/decks/validate`.

## Existing infrastructure (no work needed)

- `useDeckLibrary` (`web-client/src/store/deckLibrary.ts`) — localStorage-backed
  store with `saveDeck`, `deleteDeck`, `renameDeck`, `getDeck`. Versioned
  envelope at `argentum.decks`.
- `GET /api/decks/cards` — slim metadata for every registered card.
- `POST /api/decks/validate` — server-authoritative deck validation.
- `DeckPicker.tsx` — already implements paste/save flow, mana-curve viz,
  color pips, type counts, validation debounce — these are extractable
  primitives.
- React Router v6 already wired in `main.tsx`.

## Phase 1 — Backend: enrich the card catalog (small)

`DecksController.CardSummaryDTO` is too slim for a deckbuilder (no oracle
text, no image, no P/T). Extend it with optional fields so the picker keeps
working:

- Add `oracleText: String?`, `power: String?`, `toughness: String?`,
  `imageUri: String?`, `keywords: List<String>` (parsed from the card
  definition).
- File:
  `game-server/src/main/kotlin/com/wingedsheep/gameserver/controller/DecksController.kt`.
- No new endpoints; the picker already only reads the fields it cares about.

If image URIs aren't already on `CardDefinition.metadata`, fall back to the
existing card-image lookup the rest of the client uses — otherwise serve
`null` and let the grid render text-only tiles with hover preview.

## Phase 2 — Client: query language + filter module

New module `web-client/src/components/deckbuilder/cardFilter.ts`:

- Parser → `(card: CardSummary) => boolean`.
- Tokens:
  - `t:creature` — type
  - `c:wug` / `c<=wu` — color / color identity
  - `cmc:3` / `cmc>=4` / `cmc<=2` — mana value
  - `o:flying` — substring oracle text
  - `pow>=4`, `tou<=2` — power / toughness
  - `r:rare` — rarity
  - `s:blb` — set
  - `is:legendary` — supertype / keyword
  - Bare word = name substring.
  - `-` prefix negates. Whitespace = AND.
- Pure function, unit-testable, no React.

Filter menu UI (`FilterPanel.tsx`) writes into the same query string via
toggles (color checkboxes, CMC slider, type chips, rarity, set, keyword
multi-select). Single source of truth = the parsed query, so menu and
free-text stay in sync.

## Phase 3 — Page: `DeckbuilderPage.tsx`

Location: `web-client/src/components/deckbuilder/DeckbuilderPage.tsx`.
Three-column layout matching `DeckBuilderOverlay.tsx`:

- **Left rail**: filter panel + saved-deck sidebar (list with rename /
  delete / "new deck" / duplicate).
- **Center**: search bar + sortable card grid. Lazy-load card images via
  `IntersectionObserver` (catalog is 1000+ cards). Click adds, right-click
  or shift-click removes; respects 4-of rule (basics excepted) using
  current count from the deck.
- **Right rail**: deck list (spells / lands), name input, mana curve, color
  pips, type counts, validation status, **Save / Save As / Delete**, **Back
  to menu**. Reuse `ManaCurveBars` and the stat blocks from `DeckPicker.tsx`
  — extract them into `web-client/src/components/deckbuilder/shared/` so the
  picker, sealed builder, and standalone builder all share one
  implementation.

State is **local to the page**, not in `gameStore` — the deckbuilder runs
offline and shouldn't touch the WebSocket store or `draftSlice`. Persistence
goes through `useDeckLibrary`. Server validation is debounced like in
`DeckPicker.tsx`.

## Phase 4 — Routing + main menu entry

- Register routes in `web-client/src/main.tsx`:
  - `/deckbuilder` → new empty deck
  - `/deckbuilder/:deckId` → load by id from `useDeckLibrary`; 404 fallback
    if missing.
- In `ConnectionOverlay` (the home screen), add a **Deckbuilder** button
  alongside Replays. `useNavigate()` to `/deckbuilder`.
- "Back to menu" navigates to `/`.

## Phase 5 — Sealed/draft alignment (deferred)

Don't fold the sealed `DeckBuilderOverlay` into this page in the same PR —
it has sealed-specific concerns (fixed pool, basic-land counters,
submit-to-server, archetype hints, AI synergy suggestions). Just extract
the **shared visual primitives** (grid tile, deck list row, curve, color
pips) under `components/deckbuilder/shared/` so both consumers render
identically. Full convergence can come later.

## Open questions

1. **Format / legality**: should the deckbuilder enforce a format
   (Standard-style legality, set restriction), or always "all implemented
   cards, ≥60, 4-of"? The current `/api/decks/validate` only enforces the
   latter — confirm before adding a format dropdown.
2. **Images**: are card images already exposed on the backend in a form
   that can be added to `CardSummaryDTO`, or should the grid stay text-only
   with hover preview? Affects Phase 1 weight.
3. **Query language scope**: the Scryfall-style set above, or simpler
   text-name search + menu only? More syntax = more parser tests.

## Suggested PR breakdown

1. Phase 1 + 2 (backend DTO extension + filter module with unit tests) —
   small, mergeable independently.
2. Phase 3 + 4 (page, routing, menu entry) — the user-visible chunk.
3. Phase 5 (shared primitive extraction, sealed builder refactor to use
   them) — cleanup, no behaviour change.
