package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.DesertOfTheIndomitable
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Desert of the Indomitable (HOU) — Land — Desert. Enters tapped, taps for {G}, cycling {1}{G}.
 */
class DesertOfTheIndomitableScenarioTest : ScenarioTestBase() {
    init {
        test("enters the battlefield tapped") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Desert of the Indomitable")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val card = game.findCardsInHand(1, "Desert of the Indomitable").single()
            game.execute(PlayLand(game.player1Id, card)).error shouldBe null
            val desert = game.findPermanent("Desert of the Indomitable")!!
            game.state.getEntity(desert)?.has<TappedComponent>() shouldBe true
        }
        test("taps for {G}") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Desert of the Indomitable")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val desert = game.findPermanent("Desert of the Indomitable")!!
            game.execute(
                ActivateAbility(game.player1Id, desert, DesertOfTheIndomitable.activatedAbilities.single().id)
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.green shouldBe 1
        }
        test("cycles for {1}{G} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Desert of the Indomitable").withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Desert of the Indomitable").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Desert of the Indomitable") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
