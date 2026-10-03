package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Revitalizing Repast // Old-Growth Grove (MH3).
 *
 * Front: "Put a +1/+1 counter on target creature. It gains indestructible until end of turn."
 * Back: "This land enters tapped. {T}: Add {B} or {G}."
 */
class RevitalizingRepastScenarioTest : ScenarioTestBase() {

    init {
        context("Revitalizing Repast — the instant front") {

            test("puts a +1/+1 counter on the target and it survives a destroy effect") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Revitalizing Repast")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Revitalizing Repast", bears).error shouldBe null
                game.resolveStack()

                game.state.getEntity(bears)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
                projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true

                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("the hybrid pip can be paid with green mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Revitalizing Repast")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Revitalizing Repast", bears).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }

        context("Old-Growth Grove — the land back") {

            test("played as a land, it enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Revitalizing Repast")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

                val land = game.findPermanent("Old-Growth Grove")!!
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
