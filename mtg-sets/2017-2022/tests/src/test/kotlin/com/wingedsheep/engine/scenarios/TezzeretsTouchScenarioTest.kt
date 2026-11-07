package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tezzeret's Touch (AER #138): "Enchant artifact. Enchanted artifact is a creature with base power
 * and toughness 5/5 in addition to its other types. When enchanted artifact is put into a graveyard,
 * return that card to its owner's hand."
 *
 * Pins the animation (a noncreature artifact becomes a 5/5 artifact creature; an artifact creature's
 * own base P/T is overwritten to 5/5, per the 2017-02-09 ruling) and the graveyard trigger, which
 * looks back at the Aura's host after both have left the battlefield and returns the artifact card.
 */
class TezzeretsTouchScenarioTest : ScenarioTestBase() {

    init {
        context("Tezzeret's Touch") {

            test("a noncreature artifact becomes a 5/5 artifact creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tezzeret's Touch")
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val stone = game.findPermanent("Mind Stone")!!

                game.castSpell(1, "Tezzeret's Touch", stone).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.isCreature(stone) shouldBe true
                projected.hasType(stone, "ARTIFACT") shouldBe true
                projected.getPower(stone) shouldBe 5
                projected.getToughness(stone) shouldBe 5
            }

            test("an artifact creature's base power and toughness become 5/5") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardAttachedTo(1, "Tezzeret's Touch", "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val thopter = game.findPermanent("Ornithopter")!!

                val projected = game.state.projectedState
                projected.getPower(thopter) shouldBe 5
                projected.getToughness(thopter) shouldBe 5
            }

            test("when the enchanted artifact is put into a graveyard, the card returns to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withCardAttachedTo(1, "Tezzeret's Touch", "Mind Stone")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val stone = game.findPermanent("Mind Stone")!!

                game.castSpell(2, "Murder", stone).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Mind Stone") shouldBe true
                game.isInGraveyard(1, "Mind Stone") shouldBe false
                game.isInGraveyard(1, "Tezzeret's Touch") shouldBe true
            }
        }
    }
}
