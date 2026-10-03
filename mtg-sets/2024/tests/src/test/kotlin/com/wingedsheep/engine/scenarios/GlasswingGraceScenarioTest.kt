package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Glasswing Grace // Age-Graced Chapel (MH3).
 *
 * Front: "Enchant creature. Enchanted creature gets +2/+2 and has flying and lifelink."
 * Back: "This land enters tapped. {T}: Add {W} or {B}."
 */
class GlasswingGraceScenarioTest : ScenarioTestBase() {

    init {
        context("Glasswing Grace — the Aura front") {

            test("enchanted creature gets +2/+2, flying and lifelink; hybrid pips paid with B") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Glasswing Grace")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Glasswing Grace", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Glasswing Grace") shouldBe true
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
                projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }
        }

        context("Age-Graced Chapel — the land back") {

            test("played as a land, it enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Glasswing Grace")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

                val land = game.findPermanent("Age-Graced Chapel")!!
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
