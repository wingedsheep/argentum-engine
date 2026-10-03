package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bridgeworks Battle // Tanglespan Bridgeworks (MH3).
 *
 * Front: "Target creature you control gets +2/+2 until end of turn. It fights up to one target
 * creature you don't control." Back: "As this land enters, you may pay 3 life. If you don't, it
 * enters tapped. {T}: Add {G}."
 */
class BridgeworksBattleScenarioTest : ScenarioTestBase() {

    private fun battleGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Bridgeworks Battle")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Bridgeworks Battle")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Bridgeworks Battle — pump then fight") {

            test("the pumped Bears fight and kill the opponent's Hill Giant, surviving") {
                val game = battleGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val card = game.state.getHand(game.player1Id).single()
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Bears are 4/4 until end of turn, deal 4 to the 3/3 Giant and take 3") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.getEntity(bears)!!.get<DamageComponent>()?.amount shouldBe 3
                }
                game.isInGraveyard(1, "Bridgeworks Battle") shouldBe true
            }

            test("with only your creature targeted, it just gets +2/+2") {
                val game = battleGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val card = game.state.getHand(game.player1Id).single()
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
                game.isOnBattlefield("Hill Giant") shouldBe true
                (game.state.getEntity(giant)!!.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }

            test("your own creature can't be the fight target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bridgeworks Battle")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val card = game.state.getHand(game.player1Id).single()
                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                    )
                )
                (result.error != null) shouldBe true
            }
        }

        context("Tanglespan Bridgeworks — the land back") {

            test("played as a land, paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Tanglespan Bridgeworks")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Tanglespan Bridgeworks")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
