package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silkwrap (DTK #38) — {1}{W} Enchantment.
 *
 * "When this enchantment enters, exile target creature with mana value 3 or less an opponent
 * controls until this enchantment leaves the battlefield."
 *
 * Pins the target set (an opponent's creature with mana value 3 or less — not a bigger one, not
 * your own) and that the creature comes back under its owner's control when Silkwrap leaves.
 */
class SilkwrapScenarioTest : ScenarioTestBase() {

    init {
        context("Silkwrap") {

            test("exiles an opponent's creature with mana value 3 or less, and only such a creature is targetable") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Silkwrap")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Silkwrap").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the 6-mana Craw Wurm and your own Bears are not legal targets") {
                    decision.legalTargets[0] shouldBe listOf(courser)
                }
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Centaur Courser") shouldBe true
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }

            test("the exiled creature returns under its owner's control when Silkwrap leaves") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Silkwrap")
                    .withCardInHand(1, "Naturalize")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Silkwrap").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()
                game.isInExile(2, "Centaur Courser") shouldBe true

                val silkwrap = game.findPermanent("Silkwrap")!!
                game.castSpell(1, "Naturalize", silkwrap).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Silkwrap") shouldBe true
                game.isInExile(2, "Centaur Courser") shouldBe false
                val returned = game.findPermanent("Centaur Courser")!!
                withClue("it returns under its owner's (Bob's) control") {
                    game.state.getEntity(returned)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }
            }
        }
    }
}
