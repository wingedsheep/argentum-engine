package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.DesertOfTheGlorified
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Desert of the Glorified (HOU) — Land — Desert. Enters tapped, taps for {B}, cycling {1}{B}.
 */
class DesertOfTheGlorifiedScenarioTest : ScenarioTestBase() {
    init {
        test("enters the battlefield tapped") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Desert of the Glorified")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val card = game.findCardsInHand(1, "Desert of the Glorified").single()
            game.execute(PlayLand(game.player1Id, card)).error shouldBe null
            val desert = game.findPermanent("Desert of the Glorified")!!
            game.state.getEntity(desert)?.has<TappedComponent>() shouldBe true
        }
        test("taps for {B}") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Desert of the Glorified")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val desert = game.findPermanent("Desert of the Glorified")!!
            game.execute(
                ActivateAbility(game.player1Id, desert, DesertOfTheGlorified.activatedAbilities.single().id)
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black shouldBe 1
        }
        test("cycles for {1}{B} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Desert of the Glorified").withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Desert of the Glorified").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Desert of the Glorified") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
