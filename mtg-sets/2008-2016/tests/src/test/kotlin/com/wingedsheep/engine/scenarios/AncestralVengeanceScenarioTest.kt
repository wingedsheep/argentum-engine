package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ancestral Vengeance (FRF #61) — {B}{B} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, put a +1/+1 counter on target creature you control.
 *   Enchanted creature gets -1/-1.
 *
 * The Aura's enchant target and the enters trigger's target are chosen separately: the Aura goes on an
 * opponent's creature, the counter on one of yours.
 */
class AncestralVengeanceScenarioTest : ScenarioTestBase() {

    init {
        context("Ancestral Vengeance") {

            test("shrinks the enchanted creature and puts a +1/+1 counter on a creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ancestral Vengeance")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Ancestral Vengeance", giant).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("enchanted Hill Giant gets -1/-1") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
                withClue("the chosen creature you control got the +1/+1 counter") {
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
                withClue("the other creature is untouched") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }

            test("can't put the counter on an opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ancestral Vengeance")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Ancestral Vengeance", giant).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("Hill Giant is not a creature you control") {
                    game.selectTargets(listOf(giant)).error shouldNotBe null
                }
            }
        }
    }
}
