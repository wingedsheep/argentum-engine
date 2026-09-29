package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Kaladesh // Aetherwing, Golden-Scale Flagship.
 *
 * Front: entering makes a 1/1 flying Thopter artifact token. Back: a flying Vehicle whose power is
 * the number of artifacts you control — itself included.
 */
class InvasionOfKaladeshScenarioTest : ScenarioTestBase() {

    init {
        test("front: entering creates a 1/1 flying Thopter artifact creature token") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Kaladesh")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Kaladesh").error shouldBe null
            game.resolveStack()

            game.findPermanent("Invasion of Kaladesh") shouldNotBe null
            val thopter = game.findPermanent("Thopter")
            thopter shouldNotBe null
            val projected = game.state.projectedState
            projected.getPower(thopter!!) shouldBe 1
            projected.getToughness(thopter) shouldBe 1
            projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            projected.hasType(thopter, "ARTIFACT") shouldBe true
            projected.isCreature(thopter) shouldBe true
        }

        test("back: Aetherwing's power counts the artifacts you control, itself included") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Kaladesh")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Kaladesh")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val aetherwing = game.findPermanent("Aetherwing, Golden-Scale Flagship")
            aetherwing shouldNotBe null
            val projected = game.state.projectedState
            withClue("Aetherwing + Ornithopter = 2 artifacts") {
                projected.getPower(aetherwing!!) shouldBe 2
            }
            projected.getToughness(aetherwing!!) shouldBe 4
            projected.hasKeyword(aetherwing, Keyword.FLYING) shouldBe true
            withClue("an uncrewed Vehicle isn't a creature") {
                projected.isCreature(aetherwing) shouldBe false
            }
        }
    }
}
