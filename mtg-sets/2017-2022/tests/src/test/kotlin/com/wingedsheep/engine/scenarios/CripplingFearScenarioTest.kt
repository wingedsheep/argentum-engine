package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Crippling Fear — "Choose a creature type. Creatures that aren't of the chosen type get -3/-3
 * until end of turn."
 *
 * The type is chosen on resolution; every creature not of that type (on both sides) shrinks,
 * creatures of the type and changelings are spared, and a creature entering afterwards is not
 * affected (the set is locked in as the spell resolves).
 */
class CripplingFearScenarioTest : ScenarioTestBase() {
    init {
        test("creatures not of the chosen type get -3/-3; the chosen type and changelings are spared") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Crippling Fear")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Changeling Berserker")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Crippling Fear").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Bear")))

            withClue("non-Bears with toughness 3 or less die — on both sides") {
                game.isInGraveyard(1, "Raging Goblin") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("the chosen type is untouched") {
                val bears = game.findPermanent("Grizzly Bears")
                bears shouldNotBe null
                game.state.projectedState.getPower(bears!!) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
            withClue("a changeling is every creature type, so it is spared") {
                val berserker = game.findPermanent("Changeling Berserker")!!
                game.state.projectedState.getPower(berserker) shouldBe 5
                game.state.projectedState.getToughness(berserker) shouldBe 3
            }
            withClue("a larger non-Bear survives as a 3/1") {
                val wurm = game.findPermanent("Craw Wurm")!!
                game.state.projectedState.getPower(wurm) shouldBe 3
                game.state.projectedState.getToughness(wurm) shouldBe 1
            }
        }

        test("a creature entering after resolution is not affected") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Crippling Fear")
                .withCardInHand(1, "Raging Goblin")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Craw Wurm")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Crippling Fear").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Bear")))

            game.castSpell(1, "Raging Goblin").error shouldBe null
            game.resolveStack()

            val goblin = game.findPermanent("Raging Goblin")!!
            game.state.projectedState.getPower(goblin) shouldBe 1
            game.state.projectedState.getToughness(goblin) shouldBe 1
            game.state.projectedState.getToughness(game.findPermanent("Craw Wurm")!!) shouldBe 1
        }
    }
}
