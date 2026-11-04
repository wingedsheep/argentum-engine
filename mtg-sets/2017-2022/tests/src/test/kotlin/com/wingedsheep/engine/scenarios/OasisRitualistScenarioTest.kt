package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.OasisRitualist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Oasis Ritualist (HOU) — {T}: one mana of any color; {T}, Exert: two mana of any one color.
 */
class OasisRitualistScenarioTest : ScenarioTestBase() {
    private val oneManaId = OasisRitualist.activatedAbilities[0].id
    private val twoManaId = OasisRitualist.activatedAbilities[1].id

    init {
        test("{T}: adds one mana of the chosen color") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Oasis Ritualist")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val ritualist = game.findPermanent("Oasis Ritualist")!!
            game.execute(
                ActivateAbility(game.player1Id, ritualist, oneManaId, manaColorChoice = Color.BLUE)
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.blue shouldBe 1
            game.state.getEntity(ritualist)?.has<ExertedComponent>() shouldBe false
        }

        test("{T}, Exert: adds two mana of one chosen color and exerts the Ritualist") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Oasis Ritualist")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val ritualist = game.findPermanent("Oasis Ritualist")!!
            game.execute(
                ActivateAbility(game.player1Id, ritualist, twoManaId, manaColorChoice = Color.RED)
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 2
            game.state.getEntity(ritualist)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ritualist)?.has<ExertedComponent>() shouldBe true
        }
    }
}
