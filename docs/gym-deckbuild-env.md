# Deckbuild environment (sealed)

A second kind of gym environment, alongside the game env: instead of *playing* Magic, the agent
*builds a deck* from an opened sealed pool. It exists so we can test an agent's **deckbuilding
judgment** — colour/curve/card-evaluation — as a first-class capability, and then feed the deck it
built into a game env to see how it actually performs.

> This is a sibling of the game env documented in `gym-self-play-testing.md`. Both implement the
> same `GymEnv` interface (`observe` / `step` / `fork`) and return a discriminated `Observation`
> (`type: "Game"` or `type: "Deckbuild"`), so one driver loop works for either.

---

## Why it's its own environment (not a pre-game phase)

Deckbuilding doesn't touch the engine's `(GameState, GameAction)` model — there is no game yet: no
stack, no priority, no turns, no zones-in-play. Its observation space (a card pool), action space
(add / remove / finalize), and terminal condition (a legal deck) are all different from a game's.
Forcing it in as a "pending decision" on a `GameState` that doesn't exist would be the wrong
abstraction. So it's a distinct `GymEnv` implementation (`DeckbuildEnvironment`), and the wire
`Observation` became a sealed union to carry both shapes.

---

## API

### Create

```
POST /envs/deckbuild
{ "setCode": "BLB", "boosterCount": 6, "targetSize": 40 }
   → { "envId": "...", "observation": { "type": "Deckbuild", ... } }
```

`setCode` must be sealed-supported in the server's booster generator. The response observation lists
the opened pool.

### Observation (`type: "Deckbuild"`)

- `pool` — each distinct opened card as a `PoolCardView` (`name`, `manaCost`, `manaValue`,
  `colors`, `types`, `subtypes`, `pt`, `oracleText`, and `remaining` = copies still available to add).
- `basics` — the basic lands you may add without limit (one entry per land name).
- `selected` — the current decklist (`cardName → count`); `selectedCount` is its total.
- `targetSize` — minimum legal size; `FINALIZE` unlocks once `selectedCount` reaches it.
- `deckScore` — intrinsic quality estimate, **only populated once `terminated`** (see below).
- `legalActions`, `terminated`, `stateDigest`, `agentToAct` — the common `Observation` fields.

### Drive it with the same step loop

`legalActions` enumerates the build moves; `POST /envs/{id}/step {actionId}` applies one and returns
the next observation. Kinds:

| kind | meaning |
|------|---------|
| `ADD_CARD` | add one copy of a pool card (offered only while `remaining > 0`) |
| `ADD_BASIC` | add one basic land (unlimited) |
| `REMOVE_CARD` | remove one copy of a selected card |
| `FINALIZE` | commit the deck (offered only once `selectedCount ≥ targetSize`) |

On `FINALIZE` the env is `terminated`, `agentToAct` becomes null, and `selected` is the finished
decklist. `fork` works (branch a build for search); snapshot/restore are game-only.

### Then play what you built

The env is deliberately **decoupled** from play. Take the terminal `selected` map and start a game:

```
POST /envs  { "players": [ { "deck": { "type": "Explicit", "cards": <selected> } }, … ] }
```

---

## Reward: delayed, and across two environments

This is the subtle part, and it's intentional.

`deckScore` is an **intrinsic** estimate (summed `LimitedCardRater` ratings of the non-land cards —
calibrated to 17Lands data). It's a cheap, immediate proxy: useful for smoke-testing and for shaping,
but it is **not ground truth**. A deck's real value is **how often it wins**, and you can't know that
at finalize time — you only learn it by *playing games with the deck*, which happens in a **separate
game env, after the deckbuild env has already terminated**.

So the reward is both **delayed** (it arrives after a whole game, or many) and **cross-environment**
(the env that earns the reward is not the env that made the decisions). The deckbuild env cannot
return it.

### The gym is reward-agnostic — you supply the reward

The environment never imposes a reward. It emits observations, the finalized deck, and an optional
intrinsic score. **Your training loop owns the reward signal and the credit assignment.** That means
you are free to supply your own reward/penalty based on whether the deck actually wins — and that is
the intended way to evaluate deckbuilding properly. Concretely:

```
1. build:    open a deckbuild env → step ADD/REMOVE → FINALIZE
             → record the action trajectory τ and the finished deck D
2. evaluate: play N games in game envs with D
                - vs a fixed baseline (e.g. the heuristic build from the SAME pool — buildHeuristicSealedDeck),
                - or a mirror, or a rotating opponent pool
             → win_rate = wins / N        # this is YOUR reward, computed however you like
3. assign:   return R = win_rate (optionally blended with intrinsic: R = w·win_rate + (1-w)·deckScore/scale)
             as the terminal return for the whole build trajectory τ
4. learn:    credit-assign R across τ's add/remove/finalize steps (REINFORCE / advantage / your method)
```

Steps 1, 3 and 4 are yours; the gym just provides the deckbuild env (step 1) and the game envs
(step 2). Nothing in the contract assumes the intrinsic score is the reward — ignore `deckScore`
entirely if you only trust win-rate, or blend the two to cut early-training variance.

### Why split it this way

Because the reward boundary genuinely crosses environments, modelling build and play as **one** env
would force that env to also run games to score itself — coupling deckbuilding to the entire game
engine and to an opponent policy, and collapsing two very different decision processes into one. Two
envs joined by a reward your loop computes keeps each single-purpose: the deckbuild env is fast and
pure (no engine), the game env is unchanged, and you can swap the evaluation (baseline, opponent
pool, number of games, shaping) without touching either env.

### Practical notes

- **Variance:** a single game is a noisy label (mulligans, draws, who's on the play). Average over
  many games and fix the opponent(s); the heuristic build from the same pool is a strong,
  reproducible baseline to play against.
- **Shaping / curriculum:** blend `deckScore` early (dense, immediate) and anneal toward pure
  win-rate (sparse, true) as training stabilises.
- **Search:** `fork` a partially-built env to explore alternative completions (MCTS over card
  choices); `stateDigest` dedupes equivalent partial builds.
- **Mana base:** the env doesn't enforce a sensible land count — a deck that never casts its spells
  will simply lose, and the win-rate reward will reflect that. That's the point.

---

## Status / limits

- Sealed only. Draft (pick-1-of-N across passed packs) would be a third `GymEnv` type under the same
  surface — not yet built.
- `setCode` is required (no random-set deckbuild yet).
- The intrinsic `deckScore` ignores lands and counts only card quality, not curve or colour
  consistency — treat it as a rough prior, not a verdict.
