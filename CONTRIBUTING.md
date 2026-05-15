# Contributing to Argentum Engine

Argentum is a hobby project, but it's an open one. Contributions are welcome —
PRs, ideas, bug reports, forks, custom sets, agents trained against the gym.
This document is the short version of "what makes a contribution likely to get
merged" and "how I work on it myself, in case that's useful."

Most day-to-day discussion happens in the [Discord](https://discord.gg/dy6eSRPWzu):

- `#dev` — contribution chat, "would you accept a PR for X?", review pings,
  the short version of this guide is pinned there.
- `#showcase` — forks, custom sets, agents, anything you've built on top.
- General channels for play, bug reports, and feature ideas.

If anything below is unclear, that's the fastest place to ask.

## TL;DR

- **PRs are welcome** — no need to ask first.
- **Quality over quantity.** A few carefully-built, well-tested cards beat a
  large batch of agentic-pipeline PRs that need rework.
- **Ideas, suggestions, bug reports** — open an issue or post in Discord. No
  permission needed.
- **Forks, custom sets, AI agents against the gym** — go ahead, and please
  share what you make in `#showcase`.

## What makes a good PR

Argentum is small enough that I read every PR personally. The things I look
for, roughly in order:

1. **Faithful to the card's rules text.** Compare against Scryfall (including
   rulings and oracle errata). Cards that "mostly work" but quietly skip a
   clause are the most common reason PRs get sent back.
2. **Composes existing primitives.** The engine has a deliberate set of
   building blocks — `Effects.*`, `EffectPatterns.*`, conditions, dynamic
   amounts, target filters, replacement and triggered abilities. New
   single-purpose effects that duplicate something already in the SDK are the
   second most common reason PRs get sent back. Please read
   [`docs/architecture-principles.md`](docs/architecture-principles.md) before
   adding a new `Effect` type or executor.
3. **Tested.** A scenario test that exercises the card on a real board is
   worth more than a unit test of the executor in isolation. The
   `generate-scenario` skill produces a starting point; a manual playthrough
   in the UI catches the rest.
4. **UX is considered.** A correctly-implemented card with no visible trigger,
   no obvious targeting prompt, or a missing log line is still a broken
   experience. Trace the player flow through the web client before declaring
   the card done.
5. **Scoped sensibly.** Bundling several cards in one PR is fine *if* each
   one is built entirely from primitives already in `Effects.*` /
   `EffectPatterns.*` — those are quick to review together. If a card
   introduces a new effect, executor, condition, or other engine-level
   primitive, please keep it to **one new-effect card per PR** and include
   tests for the new primitive itself (not just the card that uses it).
   That way a regression in the new primitive can be caught and reverted
   without touching the unrelated cards.

## The pipeline I use for new cards

This is the workflow that has produced the cards already in the repo. You
don't have to follow it exactly, but the steps map well onto what reviewers
check.

1. **Implement** — run the `add-card` skill with the card name and set code.
   It handles Scryfall lookup, oracle errata, set registration, and a starter
   scenario test.
2. **Review** — run the `review-changes` skill against the diff. It catches
   most of the "this should compose an existing primitive" cases before they
   land in a PR.
3. **Re-read the principles doc.**
   [`docs/architecture-principles.md`](docs/architecture-principles.md) is
   short, and a quick re-read is the best way to notice when an
   implementation is fighting the engine rather than reusing it.
4. **Generate a scenario** — run the `generate-scenario` skill with a brief
   description of what you want to test (the interesting interaction, not
   just the happy path).
5. **Play the scenario manually** — start the server and client (`just server`
   / `just client`), load the scenario, and click through it. Check:
   - Is the triggered ability visible on the stack with a readable label?
   - Are targeting prompts unambiguous?
   - Does the opponent see what they're supposed to see (and not see what
     they're not)?
   - Does the log read sensibly?

If a step in the pipeline turns up something the previous steps missed, that's
usually a signal that the card needs another pass before opening the PR — not
that it's ready to merge with a TODO.

## Things to avoid

These come up often enough to be worth calling out:

- **One-off effects.** If you find yourself writing a new `Effect` + executor
  that resembles `Destroy`, `Mill`, `Scry`, or any of the existing
  `EffectPatterns`, stop and see whether you can compose what's there
  instead. New effect types are sometimes the right answer, but the bar is
  high.
- **Bypassing the projection layer.** Battlefield filtering by type,
  subtype, color, keyword, or P/T must go through projected state
  (`matchesWithProjection`, `projected.isCreature(...)`). The base state
  doesn't see continuous effects. This is in
  [`CLAUDE.md`](CLAUDE.md) and the architecture doc — it's the single most
  common correctness bug.
- **Silent mutations.** Every state change should emit a `GameEvent` so
  triggers and the client can react. If you find yourself wanting to update
  state without an event, that's usually a sign something is wrong.
- **AI-generated card batches.** I've had contributors push large numbers of
  agent-generated cards that didn't hold up under review. I won't merge
  those. If you're using AI assistance, that's fine — but you, the human,
  should have read every line, played the card, and convinced yourself it's
  right before opening the PR.

## Ideas, bugs, and discussion

You don't need permission to:

- File a bug report — GitHub issue, or post in the relevant Discord channel.
- Suggest a feature, mechanic, or refactor in `#dev`.
- Ask "would you accept a PR that does X?" in `#dev` — happy to give a
  yes/no/maybe before you spend time on it.

The Discord is genuinely the best place for back-and-forth; GitHub issues are
better for things that need a paper trail (reproducible bugs, concrete
proposals).

## Forks and downstream projects

Argentum is MIT licensed (see `LICENSE`). You are welcome to:

- Fork it and take it in a different direction.
- Build a custom set on top of `mtg-sdk` / `mtg-sets`.
- Train an agent against `gym` / `gym-server` / `gym-trainer`.
- Use any part of it as a learning resource for ECS, rules engines, or
  immutable game state.

If you build something cool, please share it in `#showcase` on Discord — it's
the most fun part of running this project.

## Code of conduct

Be decent. This is a hobby project, reviews happen on hobby-project
timescales, and the goal is for everyone involved to enjoy themselves.
