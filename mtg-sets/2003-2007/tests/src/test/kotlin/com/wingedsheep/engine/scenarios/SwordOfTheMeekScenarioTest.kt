package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sword of the Meek (FUT #165) — "Whenever a 1/1 creature you control enters, you may return this
 * card from your graveyard to the battlefield, then attach it to that creature."
 *
 * The trigger lives in the graveyard, fires only for a 1/1, and returns the Sword already attached.
 */
class SwordOfTheMeekScenarioTest : ScenarioTestBase() {

    init {
        context("Sword of the Meek") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, "Sword of the Meek")
                .withCardInHand(1, "Memnite")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("a 1/1 entering lets the Sword return from the graveyard attached to it") {
                val game = board()
                game.castSpell(1, "Memnite").error shouldBe null
                game.resolveStack()

                withClue("the graveyard trigger asks whether to return the Sword") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val sword = game.findPermanent("Sword of the Meek")!!
                val memnite = game.findPermanent("Memnite")!!
                withClue("the Sword is back and equips Memnite (+1/+2)") {
                    game.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe memnite
                    game.state.projectedState.getPower(memnite) shouldBe 2
                    game.state.projectedState.getToughness(memnite) shouldBe 3
                }
            }

            test("declining leaves the Sword in the graveyard") {
                val game = board()
                game.castSpell(1, "Memnite").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Sword of the Meek") shouldBe true
            }

            test("a creature that isn't 1/1 doesn't trigger it") {
                val game = board()
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Sword of the Meek") shouldBe true
            }
        }
    }
}
