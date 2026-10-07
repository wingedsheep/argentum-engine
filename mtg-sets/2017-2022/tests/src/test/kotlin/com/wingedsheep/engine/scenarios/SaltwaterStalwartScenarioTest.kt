package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe

class SaltwaterStalwartScenarioTest : ScenarioTestBase() {
    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Saltwater Stalwart", summoningSickness = false)
        .withCardOnBattlefield(1, "Viridian Longbow")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        for (recipient in 1..2) {
            test("combat damage can make player $recipient draw, including the damaged opponent") {
                val game = setup()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Saltwater Stalwart" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                game.getLifeTotal(2) shouldBe 18
                game.hasPendingDecision() shouldBe true
                val target = if (recipient == 1) game.player1Id else game.player2Id
                game.selectTargets(listOf(target)).error shouldBe null
                game.resolveStack()
                game.state.getHand(target).size shouldBe 1
                val other = if (recipient == 1) game.player2Id else game.player1Id
                game.state.getHand(other).size shouldBe 0
            }
        }

        for (damageRecipient in 1..2) {
            test("noncombat damage to player $damageRecipient triggers only for an opponent") {
                val game = setup()
                val creature = game.findPermanent("Saltwater Stalwart")!!
                val longbow = game.findPermanent("Viridian Longbow")!!
                val definition = cardRegistry.getCard("Viridian Longbow")!!
                game.execute(ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = longbow,
                    abilityId = definition.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(creature))
                )).error shouldBe null
                game.resolveStack()
                val damaged = if (damageRecipient == 1) game.player1Id else game.player2Id
                game.execute(ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = creature,
                    abilityId = definition.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id,
                    targets = listOf(ChosenTarget.Player(damaged))
                )).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(damageRecipient) shouldBe 19
                game.hasPendingDecision() shouldBe (damageRecipient == 2)
                if (damageRecipient == 2) {
                    game.selectTargets(listOf(game.player1Id)).error shouldBe null
                    game.resolveStack()
                }
                game.state.getHand(game.player1Id).size shouldBe if (damageRecipient == 2) 1 else 0
                game.state.getHand(game.player2Id).size shouldBe 0
                game.state.stack.isEmpty() shouldBe true
            }
        }
    }
}
