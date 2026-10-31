package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wintermoor Commander (ELD #205) — {W}{B} Creature — Human Knight 2 / *.
 *
 * "Deathtouch. Wintermoor Commander's toughness is equal to the number of Knights you control.
 * Whenever this creature attacks, another target Knight you control gains indestructible until
 * end of turn."
 */
class WintermoorCommanderScenarioTest : ScenarioTestBase() {

    init {
        context("Wintermoor Commander") {

            test("toughness counts the Knights you control, itself included") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wintermoor Commander")
                    .withCardOnBattlefield(1, "Knight of the Keep")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Knight of the Keep")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val commander = game.findPermanent("Wintermoor Commander")!!
                val projected = game.state.projectedState
                projected.getPower(commander) shouldBe 2
                projected.getToughness(commander) shouldBe 2
                projected.hasKeyword(commander, Keyword.DEATHTOUCH) shouldBe true
            }

            test("attacking makes another target Knight you control indestructible") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wintermoor Commander")
                    .withCardOnBattlefield(1, "Knight of the Keep")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Belle of the Brawl")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val commander = game.findPermanent("Wintermoor Commander")!!
                val keep = game.findPermanent("Knight of the Keep")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wintermoor Commander" to 2)).error shouldBe null

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactly listOf(keep)
                game.selectTargets(listOf(keep)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(keep, Keyword.INDESTRUCTIBLE) shouldBe true
                game.state.projectedState.hasKeyword(commander, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }
    }
}
