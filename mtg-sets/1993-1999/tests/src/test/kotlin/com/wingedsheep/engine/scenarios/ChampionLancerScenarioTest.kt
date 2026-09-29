package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class ChampionLancerScenarioTest : ScenarioTestBase() {
    init {
        test("prevents incoming creature combat damage while dealing its own damage normally") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Champion Lancer", summoningSickness = false)
                .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                .withActivePlayer(2)
                .build()

            val lancer = game.findPermanent("Champion Lancer")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Champion Lancer" to listOf("Craw Wurm"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isOnBattlefield("Champion Lancer") shouldBe true
            (game.state.getEntity(lancer)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 3
            game.getLifeTotal(1) shouldBe 20
        }

        test("also prevents noncombat damage from a creature ability") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Champion Lancer")
                .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lancer = game.findPermanent("Champion Lancer")!!
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player2Id,
                    sourceId = sorcerer,
                    abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id,
                    targets = listOf(ChosenTarget.Permanent(lancer))
                )
            ).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Champion Lancer") shouldBe true
            (game.state.getEntity(lancer)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }

        test("does not prevent lethal damage from a noncreature spell") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Champion Lancer")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Lightning Bolt", game.findPermanent("Champion Lancer")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Champion Lancer") shouldBe false
        }
    }
}
