package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Throwing Knife (ORI #241) — {2} Artifact — Equipment.
 *
 *   Equipped creature gets +2/+0.
 *   Whenever equipped creature attacks, you may sacrifice this Equipment. If you do, this
 *   Equipment deals 2 damage to any target.
 *   Equip {2}
 *
 * Pins the target-on-trigger / sacrifice-on-resolution split: the target is chosen as the trigger
 * goes on the stack, the "you may sacrifice" gate is asked on resolution, and the damage is dealt
 * by the already-sacrificed Knife (last-known information). Declining keeps the Knife and its bonus.
 */
class ThrowingKnifeScenarioTest : ScenarioTestBase() {

    init {
        fun game() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Throwing Knife", "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .apply { repeat(5) { withCardInLibrary(1, "Forest") } }
            .apply { repeat(5) { withCardInLibrary(2, "Mountain") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Throwing Knife") {

            test("equipped creature gets +2/+0") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }

            test("sacrificing the Knife deals 2 damage to the target chosen when it triggered") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val opponentLife = game.getLifeTotal(2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("the target is chosen as the trigger goes on the stack") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("the Knife was sacrificed and, as the source, dealt 2 damage") {
                    game.isInGraveyard(1, "Throwing Knife") shouldBe true
                    game.getLifeTotal(2) shouldBe opponentLife - 2
                }
                withClue("the Bears lose the +2/+0 once the Knife is gone") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }

            test("it can target a creature before blockers are declared") {
                val game = game()
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (3/3) took 2 damage from the sacrificed Knife") {
                    game.isInGraveyard(1, "Throwing Knife") shouldBe true
                    game.findPermanent("Hill Giant") shouldBe giant
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }

            test("declining keeps the Knife, deals no damage, and keeps the bonus") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val opponentLife = game.getLifeTotal(2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("nothing was sacrificed and no damage was dealt") {
                    game.isInGraveyard(1, "Throwing Knife") shouldBe false
                    (game.findPermanent("Throwing Knife") != null) shouldBe true
                    game.getLifeTotal(2) shouldBe opponentLife
                    game.state.projectedState.getPower(bears) shouldBe 4
                }
            }
        }
    }
}
