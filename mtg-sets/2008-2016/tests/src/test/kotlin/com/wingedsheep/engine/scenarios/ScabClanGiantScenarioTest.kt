package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Scab-Clan Giant (DGM #101) — {4}{R}{G} 4/5, "When this creature enters, it
 * fights target creature an opponent controls chosen at random."
 *
 * The engine draws the target as the trigger goes on the stack: nobody is asked, the draw follows
 * the game's seeded RNG, and only legal targets (an opponent's creature without hexproof) are in it.
 */
class ScabClanGiantScenarioTest : ScenarioTestBase() {

    private fun cast(seed: Long, opponentCreatures: List<String>, ownCreatures: List<String> = emptyList()): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withRngSeed(seed)
            .withCardInHand(1, "Scab-Clan Giant")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponentCreatures.forEach { builder.withCardOnBattlefield(2, it) }
        ownCreatures.forEach { builder.withCardOnBattlefield(1, it) }
        val game = builder.build()
        game.castSpell(1, "Scab-Clan Giant").error shouldBe null
        game.resolveStack()
        withClue("the random target is drawn, never asked for") { game.hasPendingDecision() shouldBe false }
        game.state.stack.isEmpty() shouldBe true
        return game
    }

    init {
        context("Scab-Clan Giant") {

            test("fights one opponent's creature, drawn at random across seeds") {
                val fought = (1L..12L).map { seed ->
                    val game = cast(seed, listOf("Grizzly Bears", "Hill Giant"))
                    val dead = listOf("Grizzly Bears", "Hill Giant").filter { game.isInGraveyard(2, it) }
                    withClue("seed $seed: exactly one opponent's creature fought") { dead.size shouldBe 1 }
                    game.findPermanent("Scab-Clan Giant") shouldNotBe null
                    dead.single()
                }
                withClue("both candidates come up across seeds") {
                    fought.toSet() shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Hill Giant")
                }
            }

            test("a hexproof creature and its controller's own creatures are never drawn") {
                (1L..8L).forEach { seed ->
                    val game = cast(seed, listOf("Gladecover Scout", "Grizzly Bears"), ownCreatures = listOf("Hill Giant"))
                    withClue("seed $seed") {
                        game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                        game.findPermanent("Gladecover Scout") shouldNotBe null
                        game.findPermanent("Hill Giant") shouldNotBe null
                    }
                }
            }

            test("with no legal target the trigger is removed and nothing fights") {
                val game = cast(1L, listOf("Gladecover Scout"))
                game.findPermanent("Gladecover Scout") shouldNotBe null
                game.findPermanent("Scab-Clan Giant") shouldNotBe null
            }
        }
    }
}
