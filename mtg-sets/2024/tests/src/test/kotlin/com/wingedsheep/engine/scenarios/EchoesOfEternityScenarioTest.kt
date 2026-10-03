package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Echoes of Eternity (MH3) — "If a triggered ability of a colorless spell you control or another
 * colorless permanent you control triggers, that ability triggers an additional time. Whenever you
 * cast a colorless spell, copy it. You may choose new targets for the copy. (A copy of a permanent
 * spell becomes a token.)"
 */
class EchoesOfEternityScenarioTest : ScenarioTestBase() {

    /** Resolve the whole stack, accepting the default order for any simultaneous triggers. */
    private fun TestGame.drainStack() {
        var guard = 0
        while (guard++ < 40) {
            when (val decision = getPendingDecision()) {
                is OrderObjectsDecision ->
                    submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
                null -> if (state.stack.isEmpty()) return else resolveStack()
                else -> error("Unexpected decision $decision")
            }
        }
        error("Stack did not drain")
    }

    init {
        context("Echoes of Eternity") {
            test("casting a colorless permanent spell copies it into a token; its own trigger is not doubled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Echoes of Eternity")
                    .withCardInHand(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ornithopter").error shouldBe null
                game.drainStack()

                withClue("The original plus exactly one token copy (Echoes doesn't double its own trigger)") {
                    game.findPermanents("Ornithopter") shouldHaveSize 2
                }
            }

            test("a colorless spell's cast trigger triggers an additional time, and the spell is copied") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Echoes of Eternity")
                    .withCardInHand(1, "Writhing Chrysalis")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Writhing Chrysalis").error shouldBe null
                game.drainStack()

                withClue("Devoid makes Chrysalis colorless: its 'create two Spawn' cast trigger fires twice") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 4
                }
                withClue("Original Chrysalis plus a token copy; the copy isn't cast, so no extra Spawn") {
                    game.findPermanents("Writhing Chrysalis") shouldHaveSize 2
                }
            }

            test("another colorless permanent's triggered ability triggers an additional time") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Echoes of Eternity")
                    .withCardOnBattlefield(1, "Glaring Fleshraker")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Ornithopter")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // A colored spell: no copy and no Fleshraker cast trigger.
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.drainStack()
                game.findPermanents("Grizzly Bears") shouldHaveSize 1
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0

                // Now a colorless spell: Fleshraker's cast trigger (colorless permanent) doubles.
                game.castSpell(1, "Ornithopter").error shouldBe null
                game.drainStack()
                withClue("Fleshraker's Spawn trigger fires twice") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                }
                game.findPermanents("Ornithopter") shouldHaveSize 2
            }

            test("an opponent's colorless spell is neither copied nor doubled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Echoes of Eternity")
                    .withCardInHand(2, "Ornithopter")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Ornithopter").error shouldBe null
                game.drainStack()

                game.findPermanents("Ornithopter") shouldHaveSize 1
            }
        }
    }
}
