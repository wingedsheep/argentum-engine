package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thorn Mammoth (ELD #323) — {5}{G}{G} Creature — Elephant 6/6.
 *
 * "Trample. Whenever this creature or another creature you control enters, this creature fights up
 * to one target creature you don't control."
 */
class ThornMammothScenarioTest : ScenarioTestBase() {

    init {
        context("Thorn Mammoth") {

            test("entering itself, it fights a creature you don't control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Thorn Mammoth")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Thorn Mammoth").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                val mammoth = game.findPermanent("Thorn Mammoth")!!
                game.state.getEntity(mammoth)!!.get<DamageComponent>()!!.amount shouldBe 3
            }

            test("another creature you control entering makes the Mammoth fight") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thorn Mammoth")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                val mammoth = game.findPermanent("Thorn Mammoth")!!
                game.state.getEntity(mammoth)!!.get<DamageComponent>()!!.amount shouldBe 3
            }
        }
    }
}
