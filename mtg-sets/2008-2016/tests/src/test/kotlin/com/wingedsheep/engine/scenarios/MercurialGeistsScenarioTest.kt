package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mercurial Geists (EMN #186) — {2}{U}{R} 1/3 Spirit with flying and
 * "Whenever you cast an instant or sorcery spell, this creature gets +3/+0 until end of turn."
 */
class MercurialGeistsScenarioTest : ScenarioTestBase() {

    init {
        context("Mercurial Geists") {

            test("casting an instant pumps it +3/+0") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Mercurial Geists")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                val geists = game.findPermanent("Mercurial Geists")!!
                game.getLifeTotal(2) shouldBe 17
                game.state.projectedState.getPower(geists) shouldBe 4
                game.state.projectedState.getToughness(geists) shouldBe 3
                game.state.projectedState.hasKeyword(geists, Keyword.FLYING) shouldBe true
            }

            test("casting a creature spell doesn't pump it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Mercurial Geists")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val geists = game.findPermanent("Mercurial Geists")!!
                game.state.projectedState.getPower(geists) shouldBe 1
            }
        }
    }
}
