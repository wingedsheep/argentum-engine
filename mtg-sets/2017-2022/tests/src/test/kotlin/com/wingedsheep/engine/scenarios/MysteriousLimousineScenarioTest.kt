package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mysterious Limousine (SNC #22) — {3}{W}{W} Artifact — Vehicle, 4/4
 *
 *   Whenever this Vehicle enters or attacks, exile up to one other target creature until this
 *   Vehicle leaves the battlefield. If a creature is put into exile this way, return each other
 *   card exiled with this Vehicle to the battlefield under its owner's control.
 *   Crew 2
 *
 * Proves the swap: a fresh exile returns the previously exiled card (under its owner's control)
 * and keeps the new one linked; choosing no target returns nothing; and the Vehicle leaving
 * returns whatever is still exiled with it.
 */
class MysteriousLimousineScenarioTest : ScenarioTestBase() {

    init {
        context("Mysterious Limousine") {

            test("entering exiles the target, and it returns to its owner when the Limousine leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mysterious Limousine")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Mysterious Limousine").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                val limo = game.findPermanent("Mysterious Limousine")!!
                withClue("the Giant is exiled with the Limousine") {
                    game.state.getExile(game.player2Id).contains(giant) shouldBe true
                    game.state.getEntity(limo)?.get<LinkedExileComponent>()?.exiledIds shouldBe listOf(giant)
                }

                game.castSpell(1, "Disenchant", limo).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mysterious Limousine") shouldBe false
                withClue("the Giant returns under its owner's control") {
                    val returned = game.findPermanent("Hill Giant")!!
                    game.state.getEntity(returned)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                }
            }

            test("attacking exiles a new creature and returns the previously exiled one to its owner") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mysterious Limousine", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInExile(2, "Savannah Lions")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val limo = game.findPermanent("Mysterious Limousine")!!
                val crew = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.state.getExile(game.player2Id).single()
                game.state = game.state.updateEntity(limo) { it.with(LinkedExileComponent(listOf(lions))) }

                game.execute(CrewVehicle(game.player1Id, limo, listOf(crew))).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mysterious Limousine" to 2)).error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the Bears are exiled and now the only card linked to the Limousine") {
                    game.state.getExile(game.player2Id).contains(bears) shouldBe true
                    game.state.getEntity(limo)?.get<LinkedExileComponent>()?.exiledIds
                        ?.filter { it in game.state.getExile(game.player2Id) } shouldBe listOf(bears)
                }
                withClue("the Lions came back to the battlefield under their owner's control") {
                    val returned = game.findPermanent("Savannah Lions")!!
                    game.state.getEntity(returned)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                }
            }

            test("attacking with no target exiles nothing and returns nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mysterious Limousine", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInExile(2, "Savannah Lions")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val limo = game.findPermanent("Mysterious Limousine")!!
                val crew = game.findPermanent("Hill Giant")!!
                val lions = game.state.getExile(game.player2Id).single()
                game.state = game.state.updateEntity(limo) { it.with(LinkedExileComponent(listOf(lions))) }

                game.execute(CrewVehicle(game.player1Id, limo, listOf(crew))).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mysterious Limousine" to 2)).error shouldBe null
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("the Lions stay exiled with the Limousine") {
                    game.state.getExile(game.player2Id).contains(lions) shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
            }
        }
    }
}
