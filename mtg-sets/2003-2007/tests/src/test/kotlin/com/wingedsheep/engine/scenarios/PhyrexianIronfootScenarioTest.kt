package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class PhyrexianIronfootScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Phyrexian Ironfoot")!!.activatedAbilities.first().id

    init {
        test("stays tapped during its controller's untap step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Phyrexian Ironfoot", tapped = true)
                .withCardOnBattlefield(1, "Island", tapped = true)
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.findPermanent("Phyrexian Ironfoot")!!)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(game.findPermanent("Island")!!)?.has<TappedComponent>() shouldBe false
        }

        test("one snow land and one ordinary land pay to untap it outside upkeep") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Phyrexian Ironfoot", tapped = true)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ironfoot = game.findPermanent("Phyrexian Ironfoot")!!

            game.execute(ActivateAbility(game.player1Id, ironfoot, abilityId)).error shouldBe null
            game.state.getEntity(ironfoot)?.has<TappedComponent>() shouldBe true
            game.resolveStack()
            game.state.getEntity(ironfoot)?.has<TappedComponent>() shouldBe false
            game.state.getEntity(game.findPermanent("Island")!!)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(game.findPermanent("Snow-Covered Island")!!)?.has<TappedComponent>() shouldBe true
        }

        test("two ordinary lands cannot pay the snow activation cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Phyrexian Ironfoot", tapped = true)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ironfoot = game.findPermanent("Phyrexian Ironfoot")!!

            game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                .none { it.isAffordable } shouldBe true
            (game.execute(ActivateAbility(game.player1Id, ironfoot, abilityId)).error != null) shouldBe true
            game.state.getEntity(ironfoot)?.has<TappedComponent>() shouldBe true
        }
    }
}
