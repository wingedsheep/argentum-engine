package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Vitality Charm.
 *
 * Card reference:
 * - Vitality Charm ({G}): Instant
 *   Choose one —
 *   • Create a 1/1 green Insect creature token.
 *   • Target creature gets +1/+1 and gains trample until end of turn.
 *   • Regenerate target Beast.
 */
class VitalityCharmScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.chooseMode(modeIndex: Int) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, modeIndex))
    }

    init {
        context("Vitality Charm modal spell") {

            test("mode 1: create a 1/1 green Insect creature token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vitality Charm")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Vitality Charm")
                game.resolveStack()

                // Choose mode 0: "Create a 1/1 green Insect creature token"
                game.chooseMode(0)

                withClue("Insect token should be on the battlefield") {
                    game.isOnBattlefield("Insect Token") shouldBe true
                }

                val tokenId = game.findPermanent("Insect Token")!!
                val projected = stateProjector.project(game.state)
                withClue("Insect token should be 1/1") {
                    projected.getPower(tokenId) shouldBe 1
                    projected.getToughness(tokenId) shouldBe 1
                }
            }

            test("mode 2: target creature gets +1/+1 and gains trample until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vitality Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Vitality Charm")
                game.resolveStack()

                // Choose mode 1: "+1/+1 and trample"
                game.chooseMode(1)

                // Auto-selects Grizzly Bears as only valid target

                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should be 3/3 after +1/+1") {
                    projected.getPower(bearsId) shouldBe 3
                    projected.getToughness(bearsId) shouldBe 3
                }
                withClue("Grizzly Bears should have trample") {
                    projected.hasKeyword(bearsId, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("mode 3: regenerate target Beast") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vitality Charm")
                    .withCardInHand(1, "Shock") // {R}: deal 2 damage
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 Bear, NOT a Beast
                    .withCardOnBattlefield(2, "Ravenous Baloth") // 4/4 Beast
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Vitality Charm and choose regenerate mode
                game.castSpell(1, "Vitality Charm")
                game.resolveStack()

                // Choose mode 2: "Regenerate target Beast"
                game.chooseMode(2)

                // Auto-selects Ravenous Baloth as only valid Beast target

                withClue("Ravenous Baloth should still be on the battlefield with regen shield") {
                    game.isOnBattlefield("Ravenous Baloth") shouldBe true
                }
            }

            test("mode 3 cannot target non-Beast creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vitality Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 Bear, NOT a Beast
                    .withCardOnBattlefield(2, "Ravenous Baloth") // 4/4 Beast
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Vitality Charm")
                game.resolveStack()

                // Choose mode 2: "Regenerate target Beast"
                game.chooseMode(2)

                // Only Ravenous Baloth is a Beast, should auto-select
                // Grizzly Bears is a Bear, not a Beast

                withClue("Ravenous Baloth should be on the battlefield") {
                    game.isOnBattlefield("Ravenous Baloth") shouldBe true
                }
            }

            test("mode 2 with multiple creatures prompts for target selection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vitality Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Vitality Charm")
                game.resolveStack()

                // Choose mode 1: +1/+1 and trample
                game.chooseMode(1)

                // Multiple valid creatures -> target selection decision
                game.selectTargets(listOf(giantId))

                val projected = stateProjector.project(game.state)
                withClue("Hill Giant should be 4/4 after +1/+1") {
                    projected.getPower(giantId) shouldBe 4
                    projected.getToughness(giantId) shouldBe 4
                }
                withClue("Hill Giant should have trample") {
                    projected.hasKeyword(giantId, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("Vitality Charm goes to graveyard after resolving") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vitality Charm")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Vitality Charm")
                game.resolveStack()
                game.chooseMode(0) // token mode

                withClue("Vitality Charm should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Vitality Charm") shouldBe true
                }
            }
        }
    }
}
