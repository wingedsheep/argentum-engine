package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.FerventPaincaster
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fervent Paincaster (HOU) — {T}: 1 damage to target player or planeswalker;
 * {T}, Exert: 1 damage to target creature.
 */
class FerventPaincasterScenarioTest : ScenarioTestBase() {
    private val pingPlayerId = FerventPaincaster.activatedAbilities[0].id
    private val pingCreatureId = FerventPaincaster.activatedAbilities[1].id

    init {
        test("{T}: deals 1 damage to target player") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Fervent Paincaster")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val paincaster = game.findPermanent("Fervent Paincaster")!!
            game.execute(
                ActivateAbility(game.player1Id, paincaster, pingPlayerId, listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
            game.state.getEntity(paincaster)?.has<ExertedComponent>() shouldBe false
        }

        test("{T}, Exert: deals 1 damage to target creature and exerts the Paincaster") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Fervent Paincaster")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val paincaster = game.findPermanent("Fervent Paincaster")!!
            val elves = game.findPermanent("Llanowar Elves")!!
            game.execute(
                ActivateAbility(game.player1Id, paincaster, pingCreatureId, listOf(ChosenTarget.Permanent(elves)))
            ).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            game.state.getEntity(paincaster)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(paincaster)?.has<ExertedComponent>() shouldBe true
        }
    }
}
