package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.HopeTender
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hope Tender (HOU) — {1}, {T}: untap target land; {1}, {T}, Exert: untap two target lands.
 */
class HopeTenderScenarioTest : ScenarioTestBase() {
    private val untapOneId = HopeTender.activatedAbilities[0].id
    private val untapTwoId = HopeTender.activatedAbilities[1].id

    init {
        test("{1}, {T}: untaps target land") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Hope Tender")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val tender = game.findPermanent("Hope Tender")!!
            val mountain = game.findPermanent("Mountain")!!
            game.execute(
                ActivateAbility(game.player1Id, tender, untapOneId, listOf(ChosenTarget.Permanent(mountain)))
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(mountain)?.has<TappedComponent>() shouldBe false
            game.state.getEntity(tender)?.has<ExertedComponent>() shouldBe false
        }

        test("{1}, {T}, Exert: untaps two target lands and exerts the Tender") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Hope Tender")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardOnBattlefield(1, "Island", tapped = true)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val tender = game.findPermanent("Hope Tender")!!
            val mountain = game.findPermanent("Mountain")!!
            val island = game.findPermanent("Island")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, tender, untapTwoId,
                    listOf(ChosenTarget.Permanent(mountain), ChosenTarget.Permanent(island))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(mountain)?.has<TappedComponent>() shouldBe false
            game.state.getEntity(island)?.has<TappedComponent>() shouldBe false
            game.state.getEntity(tender)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(tender)?.has<ExertedComponent>() shouldBe true
        }
    }
}
