package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Front Porch Sentries (HOB #67) — {1}{B} Creature — Goblin Soldier 2/2.
 * "When this creature dies, target creature an opponent controls gets -1/-1 until end of turn."
 *
 * A death trigger that still needs a target, with the target restricted to creatures an opponent
 * controls. The -1/-1 is enough to finish off an x/1, which is asserted through state-based
 * actions rather than by reading the modifier back.
 */
class FrontPorchSentriesScenarioTest : ScenarioTestBase() {

    init {
        context("Front Porch Sentries") {

            test("dying shrinks a targeted opponent creature, killing an x/1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Front Porch Sentries")
                    // Savannah Lions is a 1/1 here — the -1/-1 makes it a 0/0.
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentries = game.findPermanent("Front Porch Sentries")!!
                val lions = game.findPermanent("Savannah Lions")!!

                // Kill the Sentries with a Bolt so its dies trigger fires.
                game.castSpell(1, "Lightning Bolt", targetId = sentries).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the Sentries died") {
                    game.findPermanent("Front Porch Sentries") shouldBe null
                    game.isInGraveyard(1, "Front Porch Sentries") shouldBe true
                }

                val decision = game.getPendingDecision()
                withClue("the dies trigger asks for its target") {
                    (decision is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(lions)).error shouldBe null
                game.resolveStack()

                withClue("-1/-1 made the 1/1 a 0/0, which dies to state-based actions") {
                    game.findPermanent("Savannah Lions") shouldBe null
                    game.isInGraveyard(2, "Savannah Lions") shouldBe true
                }
            }

            test("only creatures an opponent controls are legal targets for the trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Front Porch Sentries")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentries = game.findPermanent("Front Porch Sentries")!!
                val ownCourser = game.findPermanent("Centaur Courser")!!
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpell(1, "Lightning Bolt", targetId = sentries).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets[0].orEmpty()
                withClue("the opponent's creature is offered") { legal shouldContain lions }
                withClue("your own creature is not — the filter is 'an opponent controls'") {
                    legal shouldNotContain ownCourser
                }
            }
        }
    }
}
