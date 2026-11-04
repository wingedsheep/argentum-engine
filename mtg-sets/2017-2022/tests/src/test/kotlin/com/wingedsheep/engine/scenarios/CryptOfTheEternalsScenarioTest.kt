package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.CryptOfTheEternals
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Crypt of the Eternals (HOU) — Land.
 * "When this land enters, you gain 1 life. {T}: Add {C}. {1}, {T}: Add {U}, {B}, or {R}."
 */
class CryptOfTheEternalsScenarioTest : ScenarioTestBase() {
    init {
        test("entering gains its controller 1 life") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Crypt of the Eternals")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val crypt = game.findCardsInHand(1, "Crypt of the Eternals").single()
            game.execute(PlayLand(game.player1Id, crypt)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
        }

        test("taps for {C}") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Crypt of the Eternals")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val crypt = game.findPermanent("Crypt of the Eternals")!!
            game.execute(
                ActivateAbility(game.player1Id, crypt, CryptOfTheEternals.activatedAbilities[0].id)
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.colorless shouldBe 1
        }

        test("{1}, {T} filters into black mana") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Crypt of the Eternals")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val crypt = game.findPermanent("Crypt of the Eternals")!!
            game.execute(
                ActivateAbility(game.player1Id, crypt, CryptOfTheEternals.activatedAbilities[2].id)
            ).error shouldBe null
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            pool.black shouldBe 1
            pool.white shouldBe 0
        }
    }
}
