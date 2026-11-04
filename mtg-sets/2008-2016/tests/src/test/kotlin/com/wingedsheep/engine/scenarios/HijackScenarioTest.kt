package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HijackScenarioTest : ScenarioTestBase() {
    init {
        context("Hijack") {
            for (permanent in listOf("Grizzly Bears", "Sol Ring")) {
                test("steals and untaps $permanent with haste until cleanup") {
                    val game = scenario()
                        .withPlayers("Player1", "Player2")
                        .withCardInHand(1, "Hijack")
                        .withLandsOnBattlefield(1, "Mountain", 3)
                        .withCardOnBattlefield(2, permanent, tapped = true)
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()
                    val target = game.findPermanent(permanent)!!

                    game.castSpell(1, "Hijack", target).error shouldBe null
                    game.resolveStack()

                    game.state.projectedState.getController(target) shouldBe game.player1Id
                    game.state.getEntity(target)!!.has<TappedComponent>() shouldBe false
                    game.state.projectedState.hasKeyword(target, Keyword.HASTE) shouldBe true

                    game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                    game.state.projectedState.getController(target) shouldBe game.player2Id
                    game.state.projectedState.hasKeyword(target, Keyword.HASTE) shouldBe false
                }
            }

            test("can target an untapped creature you already control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hijack")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Hijack", target).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getController(target) shouldBe game.player1Id
                game.state.projectedState.hasKeyword(target, Keyword.HASTE) shouldBe true
            }

            test("cannot target a land that is neither an artifact nor a creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hijack")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hijack", game.findPermanent("Forest")!!).error shouldNotBe null
            }
        }
    }
}
