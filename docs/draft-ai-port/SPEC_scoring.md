# Spec — per-card scoring (`jm`, `oX`/`aX`, helpers)

Porting contract for the Kotlin reimplementation. Language-agnostic pseudocode with
exact constants, transcribed from `index-CgvY9PKD.pretty.js`. This is the shared core:
**drafting** runs the scorer over each booster card and takes the argmax; **deckbuilding**
(`vX`) runs the same scorer greedily over the pool. Spec'd here: the scorer and its helpers.
The builders (`vX`, `Tm`, `Wm`) are a separate doc.

Minified→meaning names follow `AUTOBUILD_FINDINGS.md`'s symbol map.

---

## 1. Data model

A **Card** exposes (all optional unless noted):

| Field | Type | Notes |
|-------|------|-------|
| `id` | string | identity in score maps |
| `name` | string (req) | matched against ratings/removal/archetype via `nameKey` |
| `colors` | `List<"W"|"U"|"B"|"R"|"G">` | preferred color source |
| `color_identity` | same | fallback color source for lands/fixing |
| `mana_cost` | string | e.g. `"{1}{U}{B}"`, `"{W/U}{2/G}"`; may be `"a // b"` for split |
| `card_faces[0].mana_cost` / `.type_line` | | fallback when top-level absent |
| `type_line` | string | substring-tested (lowercased) for "land"/"creature"/etc. |
| `cmc` | number | mana value |
| `rarity` | `"mythic"|"rare"|"uncommon"|"common"` | |
| `prices.usd` | string | hate-draft signal |

**Inputs to the scorer `score(card, pool, ratings, removal, arch, archColors, removalFlag, forcedArch)`** (`jm`):
- `card` — the card to score.
- `pool` — cards already picked/in deck (**includes lands**). `nonland = pool.filter(!isLand)`.
- `ratings` — `Map<nameKey, Double>` from the set `.txt` (see `data/README.md`).
- `removal` — `Set<lowercased name>` from removal JSON.
- `arch` — `Map<nameKey, {archetypes:[{archetype,role}], fixing:[colors], splashable:Bool}>`.
  Empty for every set whose table ships no archetype columns; the tagged ones are whichever files
  exist under `draftai/archetypes/`, which moves with each data refresh.
- `archColors` — `Map<archetypeName, [colors]>` (from `Bm`, see §6). May be null.
- `removalFlag` (`i`) — Boolean. When true, removal scoring uses the simple "need N" branch
  (deckbuild passes this); drafting leaves it false/undefined.
- `forcedArch` (`l`) — archetype name to build toward; bypasses archetype detection. Drafting
  omits it (undefined); deckbuild's `vX` passes the chosen archetype.

Drafting calls `score(card, picks, ratings, removal, arch, archColors)` (last two args undefined).

**Output (`CardScore`):** `{ total: Double, rawRating: Double, reasons: List<String>, reasonPoints: List<Double>, deckContext: {primary: String?, secondary: String?} }`.
`reasons[i]` ↔ `reasonPoints[i]` are parallel; `reasonPoints[i]` is the marginal point delta
that reason contributed (used by the hint layer to find the dominant reason). `total` is the
sum, floored at 0 (except the basic-land sentinel −1). Argmax for the pick is by `total`,
tie-broken by `rawRating` (`Wo`).

---

## 2. Constants

```
QJ  = 2        # quality baseline subtracted in color-weight accumulation (aW)
po  = 3.5      # "meaningful color presence" threshold
JU  = 2        # fallback on-color flat bonus
ZJ  = 0.9
Os  = po/ZJ    # = 3.888..., fallback color-spec divisor
tX  = 2 ; eX = 0.8 ; rX = 0.3      # fallback color-spec tuning
nX  = 17       # pool size above which oW opens both top colors
ac  = 4        # "early pick" cutoff: pool nonland count < ac → early-game branch
g2  = 17       # late-pool threshold
iX  = 3 ; lX = 15
cX  = {mythic:0.15, rare:0.1, uncommon:0, common:0}     # rarity quality bonus (jm)
o_  = {mythic:4.5, rare:3.5, uncommon:2.5, common:1.5}  # rarity→rating fallback (nW)
iW  = {0:0, 1:1.5, 2:6, 3:6, 4:4, 5:2, 6:1}             # curve target per CMC bucket
eW  = ["W","U","B","R","G"]                              # canonical color order
BOMB = 3.9     # the bomb / splashable-bomb rating cutoff used everywhere
```

(`M4`, `dl`, `ba`, `Mu`, `iW`-as-`lW` belong to the builder/final-score spec.)

---

## 3. Pure helpers

`nameKey(name)` (`gt`): take substring before `//`, NFD-normalize, strip combining
diacritics (`̀-ͯ`), replace `_`→space, trim, **lowercase**. All map lookups use this.

`typeLine(card)` (`Le`): `card.type_line ?? card.card_faces[0].type_line ?? ""`.
- `isLand` (`It`) = `typeLine.lower.contains("land")`
- `isBasic` (`Ce`) = `typeLine.lower.contains("basic land")`
- `isCreature` (`_e`) = `typeLine.lower.contains("creature")`
- `isLegendaryCreature` (`pr`) = contains "legendary" AND "creature"
- `isPermanent` (`pX`) = contains any of creature/planeswalker/artifact/enchantment/battle

`rating(name, ratings)` (`Wt`) = `ratings.get(nameKey(name))` (may be null).
`ratingOrDefault(card, ratings)` (`Gu`) = `rating ?? (rarity=="mythic" ? 4 : 0)`.
`ratingFallback(card, ratings)` (`nW`) = `rating ?? o_[rarity] ?? o_.common` (fallback scorer only).

`cmcBucket(x)` (`ki`) = `min(floor(x ?? 0), 6)`.

`colors(card)` (`jt`): if `card.colors` non-empty → return it. Else parse `mana_cost` (or face0):
collect every `{C}` and both sides of every `{C/C2}` hybrid symbol, C∈WUBRG → distinct list.

`manaCostStr(card)` = `card.mana_cost ?? card.card_faces[0].mana_cost ?? ""`.

`fitsColors(card, allowed)` (`Te`): from `manaCostStr`; let `A = Set(allowed)`.
- For each `{X/Y}` hybrid: if neither X nor Y in A → **false**.
- Remove hybrid/Phyrexian symbols (`{C/[CP2]}` case-insensitive), then for each remaining `{X}`:
  if X ∉ A → **false**. Empty cost → **true**. Otherwise true.

`offPipsInString(cost, A)` (`Uf`): count of `{X/Y}` hybrids with neither in A, **plus** count of
plain `{X}` (after stripping hybrids) with X ∉ A.
`offColorsInString(cost, A)` (`XU`): the Set of those off-colors (both hybrid sides when both off).

`offPips(card, allowed)` (`ai`): split `manaCostStr` on `//`; if ≤1 face → `offPipsInString(cost,A)`;
else `min` over faces. `offColors(card, allowed)` (`dX`): same but returns `offColorsInString` of the
**min-off-pip face**.

`fixersFor(color, allowed, pool, arch)` (`uX`): count cards in `pool` that fix `color`:
- Land & not basic: let `fix = arch[name].fixing (if non-empty) else colors(card) (if any) else color_identity`.
  If `fix.length ≥ 4` → counts (universal). Else counts if `fix.contains(color)` AND `fix` overlaps `allowed`.
- Non-land: counts if `arch[name].fixing` contains `color`.

`colorPenalty(card, allowed, weight, pool, arch)` (`kn`) → `{penalty, offPips, fixing, offColors}`:
```
op = offPips(card, allowed); if op==0 → {0,0,0,[]}
offCols = offColors(card, allowed)
fixing  = min over offCols of fixersFor(col, allowed, pool, arch)   (0 if none/inf)
c = (op==1 ? 0.4 : 1)              # single off-pip is softer
d = max(0, 1 - fixing/3)           # available fixing reduces penalty
penalty = weight * c * d
```

---

## 4. Color-weight & deck-color helpers

`colorWeights(nonland, ratings)` (`aW`) → `Double[5]` indexed by `eW`:
for each card, `q = max(0, ratingFallback(card) - QJ)`; if q>0, add q to each of its `colors` slots.

`openColors(weights, poolSize)` (`oW`) → `{inColor: Int[5], topColors:[i,j], numPlayerColors}`:
`r` = argmax slot, `n` = best slot ≠ r. `inColor[r]=1 if weights[r]>po`, and `inColor[n]=1` too if
also `weights[n]>po`. If `poolSize > nX` force both `inColor[r]=inColor[n]=1`.
`numPlayerColors` = count of slots with weight > po.

`deckColors(nonland, ratings)` (`wa`) → colors sorted by weight (used as the "current colors" `w`):
1. Tally mono-colored cards' weight (`rating ?? 2.5`, or 1 if no ratings) per color.
2. `top2` = two highest. 3. Second pass over multicolor cards that touch `top2`: add their weight to
   each relevant color (only `top2` colors if the card `fitsColors(top2)`, else all its colors).
4. Return colors sorted by accumulated weight, descending.

`deckArchetypeColors(nonland, ratings)` (`s_`) → `{primary, secondary}` color-string identity, e.g.
`"WU"`, `"WU splash B"`, `"WUB"`. Same first two passes as `wa` (but skips adding off-`top2` weight for
bombs), then:
- `i,l` = top-2 colors; `d,p` = 3rd/4th; `c,u,f` their weights.
- If only one color → `{i, null}`. Compute third/fourth color support `b,g` (sum of weights of cards
  touching the splash color among `{i,l}`-pool).
- If `d` and `b ≥ max(6, c*0.4)` → `{primary:"i l d", secondary:"i l"}` (real 3-color).
- Else if `d` and (`b≥3.5` or a `d`-color bomb exists) and `pool.length≥19` → `{primary:"il splash d"}`.
- Else assemble optional splashes: include `d`/`p` if its support ≥3.5, or weight ≥ half of `c`, or
  it has a ≥BOMB card. `{primary:"il", secondary: splashString or null}`.

`archetypeScores(nonland, arch, archColors, ratings)` (`ZU`) → list of
`{name, enablers, payoffs, total}` sorted by total desc. For each non-land in pool:
- accumulate color weights `i[color] += rating ?? 2.5`.
- if it has archetype tags: per tag bump `enablers` or `payoffs`.
- else (untagged but colored): remember `{colors, quality}` for color-boost.
Then `top2 = two highest weight colors`. For each archetype in `archColors`:
- **colorBoost** from untagged cards: for each remembered card overlapping the archetype's colors `f`,
  `colorBoost += (overlapCount/cardColorCount) * clamp((quality-1.5)/4, 0.25, 0.75)`.
- If `top2.size≥2` and `poolNonland≥3`: let `g` = how many of archetype's first-2 colors are in `top2`;
  `h = min(2, 0.5 + (poolNonland-3)*0.3)`; `colorBoost += g==2 ? h : g==1 ? h*0.3 : 0`.
- `total = enablers + payoffs + colorBoost`.

`archetypeColorsFor(nonland, arch, archName, archColors)` (`QU`): prefer colors appearing on ≥2
tagged cards of that archetype in pool (`C2`); if <2 such, fall back to `archColors[archName]`; else `C2`.

`hasBombSeparation(scores)` (`mX`): `scores[0].total ≥ 4 && scores[0].total - scores[1].total ≥ 2`.

`splashBombs(nonland, deckCols, ratings)` (`fX`): off-color ≥BOMB cards worth splashing.
Returns `[]` if pool has <12 on-`deckCols` nonland cards (and pool non-empty); else the non-land cards
not castable in `deckCols` and with every color off-`deckCols` and `rating ≥ BOMB`.

---

## 5. Fallback scorer `aX` / `oX` (no archetype data)

Used when `arch` is null/empty (the common case). `oX` scores every booster card with `aX`, then
attaches a summary to the argmax. `aX(card, pool, ratings)`:

```
o = ratingFallback(card)                 # base, reason "rating X.XX"
i = colorWeights(pool, ratings)
{inColor:l, topColors:c, numPlayerColors:d} = openColors(i, pool.length)
u = sum(l)                               # # of committed colors (0/1/2)
p = colorVector(card.colors)             # 0/1 per eW slot
f = sum(p)                               # card's color count
b = # of card colors that are committed-off ; g = "on-color" flag
  (g starts true if f>0; any off pip clears it; if f==0, g = (u==2))
h = 0   # the color adjustment

if u == 2:                               # deck has 2 locked colors
    if g:  h = +JU                       reason "+2.0 on-color"
    else:  h = -max(0, b-1)              reason "-X off-color (b pips)" or "0 splashable off-color"
elif f == 0:                             # colorless card
    if d > 1: h = min(po/Os, max(i)/Os)  reason "+X colorless follows top color"
elif f == 1:                             # mono-color card, deck not locked
    let idx = card's color slot
    h = min(po/Os, i[idx]/Os)
    if d==1: h /= tX
    if d==1 and idx==c[1] and i[idx]>0: h = max(eX*po/Os, h)
    reason "+X on-color spec"
elif 2 <= f <= 3:                        # multicolor
    on  = Σ min(i[s], po) over card's colors
    off = Σ min(i[s], po) over non-card colors
    h = (on - off)/Os - rX               reason "±X multicolor spec"

total = o + h ; rawRating = o ; deckContext = {null,null}
```

`oX` summary: if exactly 2 inColor and argmax is on-color → "Best on-color card available (deck: XX,
rating R)"; else "Highest-rated card in pack (rating R); colors still open."

---

## 6. The main scorer `jm`

`archColors` is built once per pool by `Bm(nonland, arch)` (`archetype → colors appearing on ≥3 of its
cards, plus any color appearing ≥30% as often as the top one`). Pass it as `archColors`.

Accumulator pattern: keep running `u` (total) and `prev`; helper `emit(reason)` pushes
`reason` with point delta `round((u-prev)*100)/100` and sets `prev=u`. Let `nonland = pool.filter(!isLand)`,
`h = nonland.length`, `g = rating(card)`, `isBomb(x) = ratingOrDefault(x) ≥ BOMB`, `hasArch = arch?.size>0`.

### 6.1 Early exits & base

- **Basic land** → return sentinel `{total:-1, rawRating:0, reasons:["Basic land: always available, never draft"], reasonPoints:[-1], deckContext:{null,null}}`.
- **Base quality**: `isLand?` → `v = (g≥BOMB ? g : 2)` and `emit("Land")`. Else
  `v = g ?? 2.5`; emit `"Card quality: g/5"` or `"Card quality: unrated"`; add rarity bonus `cX[rarity]`
  if >0, emit `"Rarity bonus (rarity)"`. `u += v`. `rawRating = v`.

### 6.2 Bomb anchoring (only if a ≥BOMB card already in pool, card is non-land, `hasArch`)

Find the highest-rated bomb in pool. `E = (its rating≥4.3 ? 0.3 : 0.15)`. Let its archetypes `st` and
the colors of those archetypes (via `archColors`, else the bomb's own colors) form set `kt`.
- If card's colors all ∈ `kt`: `u += E`, emit "Bomb anchor: on-color".
- If card shares an archetype with the bomb: `u += E`, emit "Bomb anchor: on-archetype (X)".

### 6.3 EARLY branch — `h < ac` (fewer than 4 nonland picks)

`rt = hasArch ? archetypeScores(...) : []`.
- If a bomb in pool & non-land card: if pool already spans ≥3 colors and card has colors none of which
  are in pool's colors and `v < 4.4` → `u -= 0.5`, emit "4th color; already drafting N colors".
- If land: compute fixing colors `Y = arch.fixing or color_identity`. If a pool bomb exists and ≥2 bomb
  colors: full fixing for bomb → `u += 2.5` ("Fixing (Y): supports bomb"), or partial → `u += 0.5`.
  Else if land's colors are entirely off the colors already drafted (and not universal) →
  `u -= max(0, 1 - h*0.15)` ("Off-color land …").
- `deckContext`: from top archetype score, else from a pool bomb's archetype, else `deckArchetypeColors`.
- **Return** here (early picks don't run the full archetype/curve/removal machinery).

### 6.4 MAIN branch — `h ≥ ac`

Synergy weight ramp:
```
S = isAnyBomb ? 0.25 : 0.05
R = h<=6 ? S : min(1, S + (h-6)/28)           # grows with pool size
U = forcedArch ? [{name:forcedArch, enablers:10, payoffs:10, total:20}]
              : hasArch ? archetypeScores(...) : []
x = U[0]                                       # leading archetype
B = hasBombSeparation(U)
W = h>=g2 && (x.total ?? 0) >= iX              # late & a real archetype
G = h < lX                                     # "early-ish / colors flexible"
T = !G && (U[0].total ?? 0) >= 2
j = B || W || T                                # "committed to an archetype"
D = hasArch && (U.length>0 || h<g2)            # use archetype-aware path
k = (x && (j || x.total>=2)) ? archetypeColorsFor(...,x.name,...) : []
if j && (k.length>0 || h>=5): R = max(R, 0.5)
w = k.length>0 ? k : (h>=3 ? deckColors(nonland).take(2) : [])   # "current colors"
```

`A` (splash colors supporting bombs): if `!D` and `w.length≥2`, collect off-`w` colors of every
≥BOMB non-land in pool.

`P` (secondary archetype): if `j && G && U[1]` and U[1]'s colors differ appropriately from `k`, `P=U[1]`.

Then one of two sub-paths:

**(A) `D` true — archetype-aware** (tagged sets only):
Let `committed = (j ? U[:1] : U[:2])`, `q = (j ? 3.5 : 2.5)` (off-color weight), `E = arch[card.name]`,
`$ = E.archetypes' names`, `fixing = E.fixing`, `splashable = E.splashable`, `dt = card colors`,
`kt = (no current colors or fitsColors(card, w))`.
- **Land** path: large block computing fixing value. Outcomes & deltas:
  - Universal (`fix.length≥4`): `+1.5R+1` (no duals yet) / `+2.5R+1.5` (bomb-splash support) / `+0.3R`.
  - On-color dual that the splash needs: `+5+2R` ("Need fixing for …"). Other on-color: `+1.5R+1` /
    `+2.5R+1.5` / `+0.3R`. Partial ("wasted color"): `-2R`. Wrong colors: `-3R`. Untagged colorless land: `+0.2R`.
  - No leading archetype: `+0.6R` (has fixing colors) or `+0.3R`.
- **Non-land** path: determine on-archetype membership `ut` (card's archetypes ∩ committed) and a
  bomb-driven secondary `lt`. If on-archetype (`Bt`/`zt` non-empty):
  `weight = (j?2:1.5)`, scaled by `archScore/topArchScore` when not committed; `u += weight*R`,
  emit "On-archetype (X)". If committed & rare/mythic, `+0.6`/`+0.3` ("Won't see again"). Enabler/payoff
  balance nudges `+0.3R` ("Need enablers/payoffs"). Dual-role `+0.5R`. `+Fixing` `+0.2R`.
  - Else off-archetype but splashable bomb → splash logic via `colorPenalty` (penalty waived if ≤1 off-pip).
  - Secondary-deck bomb → `+2R`; plain secondary-deck card → 0.
  - Splashable card, not castable: bomb → `+1R` minus penalty; non-bomb → `-penalty*R`.
  - "Open to archetype signals" `+0.2R` (early, no commit). "Flexible on-color" `+0.1R`. Off-color → `-penalty*R`.
  - No archetype tags: with leading archetype & castable `+1R`; off → penalty; else "No archetype; flexible"
    `+0.3R`; on-color untagged `+0.5R`. `+Fixing` `+0.2R`.

**(B) `D` false — color-only** (the common path for untagged sets, but still inside `jm` not `aX` when
`h≥ac` and the caller passed empty arch — note: callers usually route to `aX` instead; this branch runs
when arch exists but produced no usable archetypes):
`rt = card colors`, `q = deckColors(nonland).take(2)`.
- **Land**: fixing value vs `Y = q ∪ A`: full match → `+2R(+slack)` / splash-enabling `+4+1.5R`;
  partial `+0.3R`; wrong `-1.5R`; colorless `+0.3R`.
- Colorless non-land: `+0.5R` "fits any deck".
- On-color (fitsColors(q) or all colors∈q): `+2R`. Partially on-color: `+0.5R`. Splash-supporting bomb
  color: `+1R`. ≥BOMB off-color: splash logic (waive if ≤1 off-pip). Colors still open (`q<2`): `+0.3R`.
  Else off-color: `-penalty*R`.

### 6.5 Removal (applies in MAIN branch, after color logic)

If `removal.contains(card.name.lower)`:
- `pt` = card is castable in `w` (or supports a bomb splash). If `h≥3` and have current colors and not
  castable → "Off-color removal; not playing it" (0). Else:
  - `q` = count of **on-color** removal already in pool; `E=6`; `Y=min(h/23,1)`; `shortfall = E*Y - q`.
  - If `removalFlag`: `q<E ? +1R "Removal (q/6)" : "Removal (have q; enough)"`.
  - Else: `q≥9 → -1R` (too much); `shortfall>2 → +2R*max(min(shortfall/6,1)*Y, 0.3)` ("Need removal");
    `shortfall>0 → +1R*Y`; else "enough".

### 6.6 Curve (MAIN branch)

If non-land, `isPermanent`, have current colors `w`, and (colorless or fitsColors(w)):
```
bucket = cmcBucket(cmc); target = iW[bucket]*min(h/23,1)
have   = # pool nonland in same bucket and on-color
deficit = target - have
if deficit > 0.5:  u += min(1.5, min(deficit/max(iW[bucket],1),1) * (h/23 clamp) * 3.5) * R   "Fills curve"
elif have > target+2.5: u -= 0.4R   "Overcrowded curve"
```

### 6.7 Tail adjustments (both branches, end of `jm`)

- **Hate draft**: `v<2 && price≥$5` → `+0.6/$20+, +0.4/$10+, +0.2/$5+`, emit "Hate draft ($X)".
- **Legend copies**: if `isLegendaryCreature(card)`, count same-named copies already in `pool`:
  `≥3 → -6` ("4th+ copy … Unplayable"), `≥2 → -4` ("3rd copy; deck caps at 2").
- **Return**: `total = round(max(0,u)*100)/100`, `rawRating = v`, reasons/points,
  `deckContext = (hasArch & U[0].total>0) ? {primary:U[0].name, secondary:H} : deckArchetypeColors(nonland)`
  where `H` is the secondary archetype name when not committed / `P` within threshold.

---

## 7. Porting notes / gotchas

- **Determinism**: everything here is deterministic. The only randomness (`Math.random` for picking
  hint wording) lives in `xs`'s text layer, *not* in scoring. Port scoring with no RNG.
- **Rounding**: the JS rounds totals/point-deltas to 2 decimals (`round(x*100)/100`). Replicate
  exactly, or score-matching tests against the bundle will drift.
- **Name keys everywhere**: never compare raw names; always `nameKey`. Removal uses plain `.lower`
  (not `nameKey`) — match that quirk.
- **Two scorers, one entry**: callers choose `aX`/`oX` when `arch` is empty, `jm` otherwise. Since only
  4 sets have arch data, `aX` is the default path — port it first and most carefully.
- **`v` (rawRating) vs `g` (raw map rating)**: `g` may be null; `v` is the floored/defaulted value used
  for base points and bomb tests inside `jm`. Keep them distinct.
- **Suggested Kotlin shape**: pure `object Scorer { fun score(...): CardScore }`, `data class CardScore`,
  `data class DeckContext`, helpers as private funs. No mutable global state; `arch`/`ratings`/`removal`
  are immutable inputs loaded once per set.
- **Validation harness**: drive a booster through the web app, capture its `aiScores` Map
  (`xs` output) from devtools, assert Kotlin totals match to 2 dp. Do this for one untagged set (aX path)
  and one tagged set (jm path).
