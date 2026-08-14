package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToStackResponse

/**
 * The response window as a resource, not as a counterspell slot.
 *
 * [StackResponsePuzzles] asks whether the AI ever answers a spell. This asks the question underneath
 * it: a spell on the stack is a *deadline*, and the plays it makes correct are ones that are wrong
 * at every other moment. A tap ability on a creature that is about to be destroyed is free. Removal
 * aimed at a creature that has an Aura halfway to the battlefield is a two-for-one. Neither play
 * exists a step earlier or a step later.
 *
 * The four positives are paired with three negatives on the same window, because "always respond"
 * is as wrong as "never respond" and a category made only of positives cannot tell them apart. All
 * three negatives are Giant Growth, which makes 03/04/06/07 a decision tree in four positions:
 * pump loses to *destroy*, pump beats *damage*, pump is wasted when the creature was already
 * surviving, and there is nothing on the stack that threatens a creature at all.
 */
object LastChancePuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "lastchance-01",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "The Sorcerer is dead either way — tap it for the ping before Murder resolves",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInHand(2, "Murder")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .build()
                    .also { it.castSpell(2, "Murder", it.findPermanent("Prodigal Sorcerer")) }
                    .advanceToStackResponse(1)
            },
            // Costs literally nothing: the tap symbol is paid by a permanent that will not be here
            // to untap. activate-01 is the same ability with a free choice; here the deadline is
            // the reason to use it, and an evaluator that prices "tapped" as a small penalty on a
            // permanent it still believes it owns will read this as a downgrade.
            check = {
                shouldActivate("Prodigal Sorcerer")
                shouldTarget("Llanowar Elves")
            },
        ),

        AiPuzzle(
            id = "lastchance-02",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "Kill the Bears with the Aura still on the stack — Unholy Strength dies with it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withCardInHand(2, "Unholy Strength")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .build()
                    .also { it.castSpell(2, "Unholy Strength", it.findPermanent("Grizzly Bears")) }
                    .advanceToStackResponse(1)
            },
            // One card for two, and only in this window — after the Aura resolves the same Murder
            // trades one-for-one against a 4/3, and before it was cast there was nothing but a 2/2
            // worth holding removal against. Reading it requires knowing what the spell on the
            // stack is *aimed at*, the same gap respond-04 probes from the counterspell side.
            check = {
                shouldCast("Murder")
                shouldTarget("Grizzly Bears")
            },
        ),

        AiPuzzle(
            id = "lastchance-03",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "Giant Growth does not save anything from Murder — a 5/5 is destroyed exactly as fast as a 2/2",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
                    .also { it.castSpell(2, "Murder", it.findPermanent("Grizzly Bears")) }
                    .advanceToStackResponse(1)
            },
            check = { shouldNotCast("Giant Growth") },
        ),

        AiPuzzle(
            id = "lastchance-04",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "Giant Growth in response to the Bolt — the 2/2 becomes a 5/5 and lives",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
                    .also { it.castSpell(2, "Lightning Bolt", it.findPermanent("Grizzly Bears")) }
                    .advanceToStackResponse(1)
            },
            // The positive control for 03: identical hand, identical creature, and the only
            // difference is that this removal spell deals damage. Toughness beats damage and
            // nothing beats destruction — three mana of difference the AI has to read off the
            // spell, not off the board.
            check = {
                shouldCast("Giant Growth")
                shouldTarget("Grizzly Bears")
            },
        ),

        AiPuzzle(
            id = "lastchance-05",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "Unsummon our own Serra Angel in response to Murder — a five-drop back in hand beats a dead one",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInHand(2, "Murder")
                    // A second legal target, so the puzzle asks about polarity and not just about
                    // whether Unsummon gets cast at all: bouncing the opposing 2/2 is the tempo
                    // reflex, and it throws the Angel away to save nothing.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Unsummon")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .build()
                    .also { it.castSpell(2, "Murder", it.findPermanent("Serra Angel")) }
                    .advanceToStackResponse(1)
            },
            check = {
                shouldCast("Unsummon")
                shouldTarget("Serra Angel")
            },
        ),

        AiPuzzle(
            id = "lastchance-06",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "The 6/4 walks off a Bolt — do not spend Giant Growth on a creature that is not dying",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    // Three damage against four toughness. The negative control for 04: the same
                    // spell in the same window, and the only thing that changed is that the trick
                    // is no longer needed.
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .build()
                    .also { it.castSpell(2, "Lightning Bolt", it.findPermanent("Craw Wurm")) }
                    .advanceToStackResponse(1)
            },
            check = { shouldNotCast("Giant Growth") },
        ),

        AiPuzzle(
            id = "lastchance-07",
            category = PuzzleCategory.LAST_CHANCE,
            expectation = "A draw trigger is not a deadline — hold Giant Growth on their main phase",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInHand(2, "Elvish Visionary")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
                    .also {
                        it.castSpell(2, "Elvish Visionary")
                        // One pass each resolves the creature, leaving its ETB on the stack.
                        it.passPriority()
                        it.passPriority()
                    }
                    .advanceToStackResponse(1)
            },
            // The third negative, and the one a real game hits constantly: 06 says the deadline has
            // to reach the creature, this says there has to *be* one. An ability on the stack
            // carries no card, so a policy that only reads spells sees an unreadable object and
            // hands the trick the full response bonus — which is how a pump ends up cast on the
            // opponent's main phase, guaranteed to wear off before any combat.
            check = { shouldNotCast("Giant Growth") },
        ),
    )
}
