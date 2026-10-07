package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class ScionSummonerScenarioTest : ScenarioTestBase() {
    init {
        test("devoid Summoner creates a colorless Scion that can immediately sacrifice for mana") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Scion Summoner")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Scion Summoner").error shouldBe null
            game.resolveStack()

            val summoner = game.findPermanent("Scion Summoner")!!
            game.state.projectedState.getColors(summoner) shouldBe emptySet()
            game.state.projectedState.getPower(summoner) shouldBe 2
            game.state.projectedState.getToughness(summoner) shouldBe 2
            val tokens = game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                .filter { game.state.projectedState.hasSubtype(it, "Scion") }
            tokens.size shouldBe 1
            val token = tokens.single()
            game.state.projectedState.getPower(token) shouldBe 1
            game.state.projectedState.getToughness(token) shouldBe 1
            game.state.projectedState.getColors(token) shouldBe emptySet()
            game.state.projectedState.hasSubtype(token, "Eldrazi") shouldBe true

            val ability = game.state.grantedActivatedAbilities.single { it.entityId == token }.ability
            game.execute(ActivateAbility(game.player1Id, token, ability.id)).error shouldBe null
            // Mana abilities resolve immediately, including while the token has summoning sickness.
            game.state.stack.size shouldBe 0
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
            game.state.getZone(game.player1Id, Zone.BATTLEFIELD).contains(token) shouldBe false
            game.state.getZone(game.player1Id, Zone.BATTLEFIELD).contains(summoner) shouldBe true
        }
    }
}
