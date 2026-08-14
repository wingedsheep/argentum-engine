package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToStackResponse

/**
 * Something is on the stack and the AI holds priority. Does it ever answer?
 *
 * Phase 2's 48 positions never put anything on the stack, so the AI was never once asked to
 * respond — and Phase 4b shipped its budget tiers without the "real counterspell window" CRITICAL
 * trigger precisely because there was no signal that could have told us whether adding it helped.
 * This category is that signal.
 *
 * Half positive, half negative, for the same reason [HoldingInstantsPuzzles] is: a category made
 * only of "counter it" scores 100% for an agent that counters everything it sees.
 */
object StackResponsePuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "respond-01",
            category = PuzzleCategory.STACK_RESPONSE,
            expectation = "Counter the Serra Angel",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Plains", 5)
                    .withCardInHand(2, "Serra Angel")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Counterspell")
                    .build()
                    .also { it.castSpell(2, "Serra Angel") }
                    .advanceToStackResponse(1)
            },
            check = {
                shouldCast("Counterspell")
                shouldTarget("Serra Angel")
            },
        ),

        AiPuzzle(
            id = "respond-02",
            category = PuzzleCategory.STACK_RESPONSE,
            expectation = "Do not spend the only Counterspell on a 2/2 with seven lands still open",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Forest", 7)
                    .withCardInHand(2, "Grizzly Bears")
                    // A grip that plainly still holds something worth the counter.
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Counterspell")
                    .build()
                    .also { it.castSpell(2, "Grizzly Bears") }
                    .advanceToStackResponse(1)
            },
            check = { shouldNotCast("Counterspell") },
        ),

        AiPuzzle(
            id = "respond-03",
            category = PuzzleCategory.STACK_RESPONSE,
            expectation = "Counter the Wrath that would take three creatures with it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withCardInHand(2, "Wrath of God")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Counterspell")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
                    .also { it.castSpell(2, "Wrath of God") }
                    .advanceToStackResponse(1)
            },
            // The one counter whose value is measured in *board*, not cards — three creatures for
            // one card is the position a card-advantage-weighted evaluator should find easiest.
            check = { shouldCast("Counterspell") },
        ),

        AiPuzzle(
            id = "respond-04",
            category = PuzzleCategory.STACK_RESPONSE,
            expectation = "Counter the Murder pointed at our Serra Angel",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Counterspell")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .build()
                    .also { it.castSpell(2, "Murder", it.findPermanent("Serra Angel")) }
                    .advanceToStackResponse(1)
            },
            // Requires reading what the spell on the stack is *aimed at*. Nothing in `:ai` does
            // that today: the Murder itself is one card, and only its target says what it costs us.
            check = {
                shouldCast("Counterspell")
                shouldTarget("Murder")
            },
        ),

        AiPuzzle(
            id = "respond-05",
            category = PuzzleCategory.STACK_RESPONSE,
            expectation = "Regenerate the Troll in response to the Wrath",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withCardInHand(2, "Wrath of God")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Troll Ascetic")
                    .build()
                    .also { it.castSpell(2, "Wrath of God") }
                    .advanceToStackResponse(1)
            },
            // A regeneration shield is bought *before* the destruction, which is exactly the
            // ordering a one-ply evaluator has no way to see: at the moment of the activation the
            // board is unchanged and two mana are gone.
            check = { shouldActivate("Troll Ascetic") },
        ),

        AiPuzzle(
            id = "respond-06",
            category = PuzzleCategory.STACK_RESPONSE,
            expectation = "Negate the sorcery — Essence Scatter cannot touch it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withCardInHand(2, "Wrath of God")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Negate")
                    .withCardInHand(1, "Essence Scatter")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .build()
                    .also { it.castSpell(2, "Wrath of God") }
                    .advanceToStackResponse(1)
            },
            // Both counters cost {1}{U} and only one is legal here. The enumerator does the
            // legality half; the puzzle asks whether the AI then casts the survivor or passes.
            check = { shouldCast("Negate") },
        ),
    )
}
