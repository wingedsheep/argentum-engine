package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Strength of the Harvest // Haven of the Harvest (MH3).
 *
 * Front: "Enchant creature. Enchanted creature gets +1/+1 for each creature and/or enchantment you control."
 * Back: "This land enters tapped. {T}: Add {G} or {W}."
 */
class StrengthOfTheHarvestScenarioTest : ScenarioTestBase() {

    init {
        context("Strength of the Harvest — the Aura front") {

            test("counts each creature and/or enchantment you control once, itself included, ignoring the opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Strength of the Harvest")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Spirited Companion") // enchantment creature: counts once
                    .withCardOnBattlefield(1, "Intangible Virtue")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Intangible Virtue")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.state.getBattlefield(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
                game.castSpell(1, "Strength of the Harvest", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Strength of the Harvest") shouldBe true
                // Bears, Spirited Companion, Intangible Virtue, Strength of the Harvest = +4/+4
                game.state.projectedState.getPower(bears) shouldBe 6
                game.state.projectedState.getToughness(bears) shouldBe 6
            }
        }

        context("Haven of the Harvest — the land back") {

            test("played as a land, it enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Strength of the Harvest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

                val land = game.findPermanent("Haven of the Harvest")!!
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
