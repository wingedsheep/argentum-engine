# Argentum Engine

<img src="assets/argentum-engine-white.jpeg" alt="Argentum Engine" width="400px">

*Before the oil. Before the corruption. There was only perfection.*

A Magic: The Gathering rules engine and online play platform.

---

## Overview

Argentum Engine is a modular MTG implementation consisting of:

- **Rules Engine** — A deterministic Kotlin library implementing MTG comprehensive rules
- **Game Server** — Spring Boot backend for online multiplayer
- **Web Client** — Browser-based UI

## Tech Stack

- Kotlin 2.2
- Spring Boot 4.x
- WebGL (frontend)
- Keycloak (OAuth/authentication)

## Features

### Drafting

<img src="assets/drafting.png" alt="Drafting" width="900px">

Host booster drafts with up to 8 players. Create a draft lobby, invite friends, and build your deck from freshly opened packs.

### Play

<img src="assets/play.png" alt="Play" width="900px">

Play Magic against friends with fully implemented MTG rules. The engine automatically handles the stack, priority, combat, triggers, and state-based actions—so you can focus on the game.

**[Play now at magic.wingedsheep.com](https://magic.wingedesheep.com)**

## Architecture

```
argentum/
├── rules-engine/               # Core rules, zones, actions, keywords
├── sets/
│   └── portal/                 # First supported set
│       └── card-scripts/       # Per-card ability scripts
├── server/                     # Game server & matchmaking
└── client/                     # Web frontend
```

## Rules Engine

The rules engine is a standalone library with no server dependencies. It models the complete game state immutably and
exposes a pure functional API:

### Features

- Full turn structure (phases, steps, priority)
- Stack and spell resolution
- Combat (attackers, blockers, damage assignment)
- Triggered and activated abilities
- Keywords (flying, trample, deathtouch, etc.)
- State-based actions
- Targeting and legality checks

### Card Scripts

Cards are defined as scripts that compose core abilities:

## Gameplay Platform

### Server

- OAuth login via Keycloak (Google account)
- Create games with invite links
- Deck builder from available cards
- Booster draft with friends

### Client

- WebGL-based card rendering
- Real-time game state sync
- Card images

## Roadmap

1. **Phase 1** — Core rules engine with Portal set
2. **Phase 2** — Server infrastructure and matchmaking
3. **Phase 3** — Web client MVP
4. **Phase 4** — Draft mode
5. **Phase 5** — Additional sets

## Why "Argentum"?

Argentum was a plane of mathematical perfection, created by the planeswalker Karn. Every angle intentional, every law
absolute. It was governed by rules so elegant they seemed inevitable.

That's what a rules engine should be.
