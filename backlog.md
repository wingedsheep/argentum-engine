# Backlog

## Bugs 

- [x] Ancestral memories doesn't do anything
- [x] Phantom warrior can be blocked
- [x] Wicked pact should allow me to target two creatures in the UI, but only allows me to target one
- [x] Drawing with an empty library doesn't lose the game
- [x] Don't auto pass when opponents spell is on the stack
- [x] King's Assassin can choose a target, but the target is not destroyed
- [x] Mulligan and deck search style is not correct (not portal)
- [x] Winds of change doesn't do anything
- [x] Sorcerous sight doesn't show opponent's hand
- [x] Renewing Dawn doesn't work
- [x] Command of unsummoning only returns one card to hand
- [ ] Final strike doesn't work. The UI allows me to target the opponent, but not to select a creature to sacrifice.
- [ ] Tidal surge doesn't allow me to select creatures to tap in the UI.
- [ ] UI doesn't allow me to select a target for Capricious Sorcerer's activated ability
- [ ] Ingenious thief card says "Look at opponent's hand" but it allows you to select any player as a target
- [ ] Some cards can only target yourself or the opponent, but the UI still allows you to select the target

## Features

- [x] Add persistence. If the game server restarts, keep games that are in progress. Maybe Redis or PostgreSQL?
- [ ] When playing sealed with more than 2 people and you are not currently playing, it might be possible to watch other people play a game.