package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Queen Allenal of Ruadach (DMU #210, {G}{W}{W}, * / * Legendary Elf Noble).
 *
 *   Queen Allenal of Ruadach's power and toughness are each equal to the number of creatures you control.
 *   If one or more creature tokens would be created under your control, those tokens plus a 1/1
 *   white Soldier creature token are created instead.
 *
 * Pins: (1) one Soldier per creation event of creature tokens, and it doesn't loop (CR 614.5);
 * (2) the Soldier is a 1/1 white creature; (3) the token filter — a noncreature Treasure adds
 * nothing; (4) her P/T counts every creature you control, herself included.
 */
class QueenAllenalOfRuadachScenarioTest : ScenarioTestBase() {

    init {
        context("Queen Allenal of Ruadach") {

            test("two creature tokens become two tokens plus exactly one white 1/1 Soldier") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Queen Allenal of Ruadach")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Raise the Alarm")
                game.resolveStack()

                withClue("Raise the Alarm makes its two Soldiers") {
                    game.findPermanents("Soldier Token").size shouldBe 2
                }
                withClue("Queen Allenal adds exactly one Soldier — once per event, and it doesn't loop") {
                    game.findPermanents("Soldier").size shouldBe 1
                }

                val soldier = game.state.getEntity(game.findPermanents("Soldier").single())
                    ?.get<CardComponent>()!!
                withClue("The added Soldier is a white 1/1") {
                    soldier.colors shouldBe setOf(Color.WHITE)
                    soldier.baseStats?.basePower shouldBe 1
                    soldier.baseStats?.baseToughness shouldBe 1
                }

                val queen = game.findPermanent("Queen Allenal of Ruadach")!!
                withClue("Her P/T counts the four creatures you control, herself included") {
                    game.state.projectedState.getPower(queen) shouldBe 4
                    game.state.projectedState.getToughness(queen) shouldBe 4
                }
            }

            test("a noncreature token adds no Soldier") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Queen Allenal of Ruadach")
                    .withCardInHand(1, "Strike It Rich")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Strike It Rich")
                game.resolveStack()

                withClue("The Treasure is created") {
                    game.findPermanents("Treasure").size shouldBe 1
                }
                withClue("No Soldier — the replacement only covers creature tokens") {
                    game.findPermanents("Soldier").size shouldBe 0
                }

                val queen = game.findPermanent("Queen Allenal of Ruadach")!!
                withClue("Alone, she is a 1/1") {
                    game.state.projectedState.getPower(queen) shouldBe 1
                    game.state.projectedState.getToughness(queen) shouldBe 1
                }
            }
        }
    }
}
