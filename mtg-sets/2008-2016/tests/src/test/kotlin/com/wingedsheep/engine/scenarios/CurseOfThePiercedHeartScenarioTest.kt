package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Curse of the Pierced Heart (ISD #138) — {1}{R} Enchantment — Aura Curse.
 *
 * "Enchant player. At the beginning of enchanted player's upkeep, this Aura deals 1 damage to that
 * player or a planeswalker that player controls."
 *
 * Pins that the damage goes to the enchanted player (not the Curse's controller), that the Curse's
 * controller may redirect it to one of the enchanted player's planeswalkers (and only theirs), and
 * that declining the planeswalker sends the damage to the player.
 */
class CurseOfThePiercedHeartScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun board(opponentPlaneswalker: Boolean, ownPlaneswalker: Boolean = false): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Curse of the Pierced Heart")
            .apply { if (opponentPlaneswalker) withCardOnBattlefield(2, "Liliana Vess") }
            .apply { if (ownPlaneswalker) withCardOnBattlefield(1, "Liliana Vess") }
            .apply { repeat(3) { withCardInLibrary(1, "Mountain") } }
            .apply { repeat(3) { withCardInLibrary(2, "Mountain") } }
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()
        val curse = game.findPermanent("Curse of the Pierced Heart")!!
        game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            .updateEntity(game.player2Id) { player ->
                val existing = player.get<AttachmentsComponent>()?.attachedIds.orEmpty()
                player.with(AttachmentsComponent(existing + curse))
            }
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("deals 1 damage to the enchanted player with no prompt when they control no planeswalker") {
            val game = board(opponentPlaneswalker = false, ownPlaneswalker = true)

            withClue("the Curse's controller's own planeswalker is not a legal recipient") {
                game.hasPendingDecision() shouldBe false
            }
            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
        }

        test("the Curse's controller may send the damage to the enchanted player's planeswalker") {
            val game = board(opponentPlaneswalker = true)
            val liliana = game.findPermanent("Liliana Vess")!!

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.playerId shouldBe game.player1Id
            decision.legalTargets[0] shouldBe listOf(liliana)
            withClue("the player remains a choice: the planeswalker may be declined") {
                decision.targetRequirements.single().minTargets shouldBe 0
            }
            game.selectTargets(listOf(liliana)).error shouldBe null
            game.resolveStack()

            game.loyalty(liliana) shouldBe 4
            game.getLifeTotal(2) shouldBe 20
        }

        test("declining the planeswalker deals the damage to the enchanted player") {
            val game = board(opponentPlaneswalker = true)
            val liliana = game.findPermanent("Liliana Vess")!!

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(emptyList()).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.loyalty(liliana) shouldBe 5
        }
    }
}
