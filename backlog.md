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
- [x] com.wingedsheep.gameserver.scenarios.ExhaustionScenarioTest > Exhaustion prevents untapping of creatures and lands > component is consumed and permanents untap normally in subsequent turns FAILED
- [x] Final strike doesn't work. The UI allows me to target the opponent, but not to select a creature to sacrifice.
- [ ] Tidal surge doesn't allow me to select creatures to tap in the UI.
- [ ] UI doesn't allow me to select a target for Capricious Sorcerer's activated ability
- [ ] Ingenious thief card says "Look at opponent's hand" but it allows you to select any player as a target
- [ ] Some cards can only target yourself or the opponent, but the UI still allows you to select the target
- [ ] Last chance doesn't work yet. It should show a badge to indicate that the player loses next turn, and make the opponent skip their next turn.
- [ ] Spitting earth card doesn't deal any damage, even though I have mountains in play
- [ ] Disable analytics for local development
- [x] Breath of life doesn't work. It can't target a creature in my graveyard.
- [ ] First return to tournament standings before starting next game

## Improvements

- [ ] Don't move cards when no hand
- [ ] Timeline, in which phase, and whose turn is it
- [ ] Button to accept spells on stack and pass to another phase of the turn

## Features

- [x] Add persistence. If the game server restarts, keep games that are in progress. Maybe Redis or PostgreSQL?
- [ ] Add spectator mode for sealed 
- [ ] Matchmaking
