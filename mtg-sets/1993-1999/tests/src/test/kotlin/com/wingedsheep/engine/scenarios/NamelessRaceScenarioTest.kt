package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Nameless Race (DRK #50).
 *
 * {3}{B} Creature, trample, power and toughness each equal to the life paid as it entered.
 * "As this creature enters, pay any amount of life. The amount you pay can't be more than the total
 *  number of white nontoken permanents your opponents control plus the total number of white cards
 *  in their graveyards."
 *
 * The interesting property is that a choice made during resolution has to survive into layer
 * projection for as long as the permanent lives — so the tests read P/T back through the projector,
 * not through the resolution that made the choice.
 */
class NamelessRaceScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Nameless Race") {

            test("power and toughness equal the life paid, and the life is actually paid") {
                // Two white nontoken permanents opposite plus one white card in the graveyard =
                // a ceiling of 3.
                val game = scenario()
                    .withPlayers("Necromancer", "Whitemage")
                    .withCardInHand(1, "Nameless Race")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInGraveyard(2, "Disenchant")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Nameless Race").error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("ceiling is 2 white nontoken permanents + 1 white card in the graveyard") {
                    decision.maxValue shouldBe 3
                }

                game.chooseNumber(3)
                game.resolveStack()

                val race = game.findPermanent("Nameless Race")!!
                val projected = projector.project(game.state)
                withClue("P/T are read back from the recorded choice during projection") {
                    projected.getPower(race) shouldBe 3
                    projected.getToughness(race) shouldBe 3
                }
                withClue("and the life was actually paid") {
                    game.getLifeTotal(1) shouldBe 17
                }
            }

            test("paying nothing makes it a 0/0, and it dies to state-based actions") {
                val game = scenario()
                    .withPlayers("Necromancer", "Whitemage")
                    .withCardInHand(1, "Nameless Race")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Nameless Race").error shouldBe null
                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(0)
                game.resolveStack()

                withClue("a 0/0 with no counters is binned by CR 704.5f") {
                    game.findPermanent("Nameless Race").shouldBeNull()
                }
                withClue("no life was paid") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("with no white cards opposite there is no choice to make") {
                // The ceiling resolves to 0, so the card records 0 without prompting at all.
                val game = scenario()
                    .withPlayers("Necromancer", "Goblin")
                    .withCardInHand(1, "Nameless Race")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Nameless Race").error shouldBe null
                game.resolveStack()

                withClue("no prompt — there is exactly one legal answer") {
                    game.state.pendingDecision.shouldBeNull()
                }
                withClue("and it entered as a 0/0, so it is already gone") {
                    game.findPermanent("Nameless Race").shouldBeNull()
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
