# Spec — draft entry points & hint text (`xs`, `Wo`, the reason layer)

Porting contract for the draft-time surface: scoring a booster, choosing the pick, and the
human-readable hint shown for the suggested card. Builds on `SPEC_scoring.md` (`jm`/`aX`/`oX` and
helpers). This is the thinnest layer — the math is done; here it's argmax + presentation.

The pick itself needs only `score()` + argmax. **The entire hint-text apparatus is cosmetic**: it
picks flavor strings at random and never changes which card is chosen. Port §1–§2 to draft; port §3
only when you want the "why" blurb. The verbatim string arrays (~80 of them, lines 47900–48436 of the
pretty bundle) can be copied as-is or rewritten — they carry no logic.

---

## 1. `Wo` — argmax pick

`Wo(cards, scoreMap)` → index of the best card:
```
best=0, bestTotal=-inf, bestRaw=-inf
for i in cards.indices:
  s = scoreMap[cards[i].id]; total = s?.total ?? 0; raw = s?.rawRating ?? 0
  if total > bestTotal || (total == bestTotal && raw > bestRaw):
      bestTotal=total; bestRaw=raw; best=i
return best
```
Tie-break: higher `total`, then higher `rawRating`. (Same rule the builder sorts cards by.)

---

## 2. `xs` — score a booster

`xs(booster, picks, ratings, removal, arch, archColors)` → `Map<cardId, CardScore>`.
```
if (arch == null || arch.isEmpty()): return oX(booster, picks, ratings)   # fallback path, §2.1
scores = Map()
for card in booster: scores[card.id] = jm(card, picks, ratings, removal, arch, archColors)  # 6-arg jm
# (the rest of xs only ANNOTATES scores[chosen].summary; totals are already final here)
return scores
```
Drafting consumers (DraftPage): `val scores = xs(...); val pick = booster[Wo(booster, scores)]`.
`scores` also drives per-card tooltips (each card's `total`, `reasons`, and — for the chosen card —
`summary`).

**`jm` here gets only 6 args** → `removalFlag` and `forcedArch` are undefined: removal uses the
"need N / too much" branch, archetype is auto-detected (no forced build). Contrast the *builder*,
which passes both (`SPEC_deckbuild.md`).

### 2.1 Fallback summaries (`oX`, no archetype data — the common path)
`oX` scores with `aX` (`SPEC_scoring.md` §5) and attaches a one-line `summary` to the argmax only:
- if exactly 2 committed colors and the pick is on-color → `"Best on-color card available (deck: XX, rating R)."`
- else → `"Highest-rated card in pack (rating R); colors still open."`
No reason layer beyond that. The full §3 prose only runs on the `jm` path (the tagged sets).

---

## 3. The hint layer (only when `arch` present)

After choosing `d = argmax`, `u = scores[d.id]`, `xs` builds a paragraph into `u.summary`. All inputs
come from `jm` output + simple pack facts. This section is a decision tree selecting flavor strings.

### 3.1 Derived inputs
```
p   = picks.count{ !isLand }                 # picks made so far (drives "pick stage")
S   = d.name ; R = u.rawRating
U   = R >= 3.9        # bomb         x = R >= 4.3   # premium bomb
G   = colors(d) ; D = G mapped to color words (W→white …); T = letter→word map
W   = titlecase(u.deckContext.primary) or "deck"        # archetype display name
y   = dominantReason(u)                       # §3.2 — the category driving the score
# raw-best card in pack (ignoring fit):
A   = nonland booster card with max (rating ?? 0) ; P = that rating
M   = (A == null || A.id == d.id)             # is the pick also the raw-best card?
# closest competitor to the pick:
(f, b, gMin) = the card whose (u.total - its total) is smallest & ≥0   # f=card, b=its score, g=gap
# current colors of the deck (for "off-color" tests):
colorsSet = arch-colors of deckContext.primary, else deckColors(nonland picks).take(2)
Z/X = G.isNotEmpty && G.all { it !in colorsSet }   # the pick is off our colors
```

### 3.2 Dominant-reason classifier `dominantReason(u)` (`l`)
Find the reason with the highest `reasonPoints`; lowercase its text; map by substring (first match):
```
"card quality"                      → "quality"
"on-archetype" | "secondary deck"   → "archetype"
"removal"                           → "removal"
"curve" | "fills curve"             → "curve"
"fixing" | "land"                   → "mana"
"on-color" | "color fit"            → "color"
"hate draft"                        → "hate"
otherwise                           → "quality"
```
This category `y` selects which family of flavor sentences the summary uses.

### 3.3 "Best card is off-color" explanation `I`
Built only when `!M && A != null && p >= ac` (a strictly better raw card exists but we're passing it).
Determines whether A is off our colors (`E`), then composes: `"<A is best but off-color> <take S>
because <reason by y>"`. The `y`-specific clauses cover removal-shortfall (`q/6`), curve gap
(`<n>-drops`), archetype fit (`W`), or "best on-color option". If `I` is built it **overrides** `F`
below (`F = I.trim()`).

### 3.4 Main narration `F` by pick stage
`ot` = optional "despite we're in `W`" clause when off-archetype. Branch on `p`:

**Early (`p < ac`)** — emphasis on anchoring:
- premium bomb `x` → "<take S>: it's a bomb." + (if a leading archetype `w[0]`) "anchor our draft
  around `<Archetype> (colors)`" else (if have colors) "flexible, leaning `<colors>`".
- strong `U` → similar, "build around" / "looking for `<Archetype>` cards".
- else: if a pool bomb exists `H` → on/off-color bomb-support line; else `y=="quality"` → "strongest
  card; open to signals" / else "strong early pick; open to signals".

**Mid (`p < g2`)**: if off-color `Z` → switch on `y` (removal / curve / default "powerful, take it" or
"strongest option"); else on-color → switch on `y` (archetype `W` / removal / curve / mana / default
"strongest for `W`").

**Late (else)**: if off-color `Z` → removal / curve / **hate** / default; else → archetype / removal /
curve / mana / hate / default. (Late adds the hate-draft family that early/mid omit.)

### 3.5 Appends (added to `F`, in order)
1. **Sideboard note**: if `R ≤ 2.5 && offColor && p ≥ ac` → replace `F` with a "straight to the
   sideboard" line.
2. **Open-color signal**: if `p ≥ 4 && offColor && R ≥ 3 && (15 - boosterSize) ≥ 4` → append
   "a `R/5` card at Pick `<15-boosterSize>` suggests `<colors>` is open."
3. **Slam dunk**: if `gMin > 1 && picks.size ≥ 6` → append "easy pick / windmill slam" line.
4. **Runner-up mention**: if `f != null && b != null && gMin ≤ 0.5`:
   - `gMin ≤ 0.2`: if runner-up's dominant reason differs from `y` → "Another strong pick is `<f>` if
     you value `<reason word>` over `<reason word>`" (reason-word map: quality→"raw card quality",
     archetype→"archetype synergy", removal→"removal", curve→"curve", mana→"mana fixing",
     color→"color consistency", hate→"denying opponents"); else → "it's neck and neck." Optionally
     append an open-signal line for `f` if it's a good off-color card late.
   - `0.2 < gMin ≤ 0.5`: "`<f>` is also reasonable." + optional open-signal.
5. **`I` override** (§3.3): if `I` was built, `F = I.trim()` (discards the above narration).
6. **Actionable advice** (`p ≥ ac`), appended as extra sentences when applicable:
   - **Removal pace**: target `6`; pace `(6/38)*p`. If the pick is removal and on-color removal count
     `< pace - 0.5 && < 6` → "aiming for 6 removal, have N, prioritize removal."
   - **Curve gap**: bucket `b = cmcBucket(pick.cmc)`; if `b>0 && iW[b]*fill ≥ 1.5 && have < target*0.4`
     → "curve light on `b`-drops (have/target); this helps."
   - **Splash fixing**: if there are on-pool ≥BOMB off-color cards we want and not enough fixing for
     them and the pick is a land → "want to splash `<names>` but need `<colors>` fixing."

`u.summary = F`. `xs` returns the score map unchanged otherwise.

---

## 4. Porting notes

- **The pick is independent of all of §3.** A minimal correct port is `Wo(booster, xs(...))`. Ship that
  first; the summary is polish.
- **`p` is the pick *stage*** = count of nonland picks so far, not the literal pick number. `ac=4`,
  `g2=17` split early/mid/late. "Pick N" *display* uses `15 - boosterSize` or `p+1` (pack-relative).
- **RNG**: `h(arr) = arr[floor(random()*arr.length)]` is the only randomness in the whole system and it
  only chooses wording. For reproducible hints, seed it; for parity tests, ignore `summary` and compare
  `total`/`rawRating` only.
- **Fallback vs full**: empty `arch` → `oX` (one-line summary, no §3). Only the tagged sets reach the
  full reason tree. Don't block the port on §3 for untagged sets — they never use it.
- **Reason strings are load-bearing for `dominantReason`**: the classifier matches substrings of the
  reasons emitted by `jm`. If you reword `jm`'s reason strings in the port, update `dominantReason`'s
  substring table to match, or `y` will misclassify.
- **`deckContext.primary` → display**: titlecase by splitting on space/underscore/hyphen. `archColors`
  maps the archetype name to its color letters for the "(WU)" annotations.
