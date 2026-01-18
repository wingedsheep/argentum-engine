I'd like to create a magic the gathering game engine, and a graphical user interface so that I can play online against my friends.

# General

- Modular design (everything is easy to extend, new sets with new rules can be added easily)
- Kotlin 2.2
- Spring boot 4.x library

# Rules engine

A programmatic engine written in Kotlin that can be used to play games and simulate the rules.

- An engine written in Kotlin that contains the rules for MTG.
- A core that contains foundational functions like
  - Casting spells
  - Drawing cards
  - Attacking and blocking
  - Placing counters
  - Targeting 
  - Prompting the player when there are choices
  - Move cards between playing areas like hand, play, graveyard, exile or deck
  - Shuffling
  - Tapping
  - Losing and gaining life
  - Turns and phases within the turns: 
    - Beginning Phase (Untap, Upkeep, Draw)
    - Pre-Combat Main Phase
    - Combat Phase (Begin, Attackers, Blockers, Damage, End)
    - Post-Combat Main Phase 
    - Ending Phase (End, Cleanup)
  - Choosing who starts the game / coinflip
  - Mulligan
  - Winning or losing the game
- A layer on top of the core with common abilities like
  - Trample
  - Flying
  - Scry
  - Deathtouch
  - Lifegain
- Scripts that can be programmed for individual cards
  - For more specific rules programmable per card
  - Can use existing abilities and rules
- What more
  - Each instance of a card should have a unique id (so it can be targeted)
  - Modular system for adding sets. New cards don't have to touch existing core code.
  - The first set we are going to implement is portal. Since it is quite a simple set it can be used as a good basis that we can extend later.
    - Should be able to convert scryfall json to card scripts

Example scryfall data:

```json
{
  "name": "Alabaster Dragon",
  "mana_cost": "{4}{W}{W}",
  "cmc": 6.0,
  "type_line": "Creature — Dragon",
  "oracle_text": "Flying\nWhen this creature dies, shuffle it into its owner's library.",
  "power": "4",
  "toughness": "4",
  "colors": [
    "W"
  ],
  "color_identity": [
    "W"
  ],
  "keywords": [
    "Flying"
  ],
  "oracle_id": "2392a41a-59d3-4749-be94-4d9df0af9c4c"
}
```

# Gameplay engine

An engine with a graphical user interface that can be used to play against friends.
This engine could be like a copy of MTG arena. 

It should run in the browser using WebGL (for a smooth experience), and have a backend that connects to the rules engine to run the games.

Backend:

- Players can login using keycloak (oauth with Google account) and start a game.
- Uses the rules engine to simulate games
- Can start a game, and invite a friend to join using a join link (unique url?)
- Can create a deck from available cards
- Booster draft against friends

Frontend: 

- Graphical user interface that mimics MTG arena.
- Cards are displayed graphically. Images can come from links to a cms or links to scryfall.
