package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Animate Dead (LEA): return a creature card from any graveyard under your control with the Aura
 * attached, giving it -1/-0; the creature is sacrificed when the Aura leaves; and nothing comes back
 * if the card leaves the graveyard while the enters trigger is on the stack.
 */
class AnimateDeadScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Animate Dead")
        .withCardInHand(1, "Disenchant")
        .withCardInGraveyard(2, "Hill Giant")
        .withCardOnBattlefield(2, "Tormod's Crypt")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castAnimateDead() {
        val giant = state.getGraveyard(player2Id).single()
        val aura = findCardsInHand(1, "Animate Dead").single()
        execute(CastSpell(player1Id, aura, listOf(ChosenTarget.Card(giant, player2Id, Zone.GRAVEYARD)))).error shouldBe null
    }

    init {
        test("returns the opponent's creature card under your control, enchanted, with -1/-0") {
            val game = board()
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castAnimateDead()
            game.resolveStack()

            val aura = game.findPermanent("Animate Dead")!!
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe giant
            game.state.projectedState.getPower(giant) shouldBe 2
            game.state.projectedState.getToughness(giant) shouldBe 3
        }

        test("when Animate Dead leaves the battlefield the creature is sacrificed") {
            val game = board()
            game.castAnimateDead()
            game.resolveStack()

            game.castSpell(1, "Disenchant", game.findPermanent("Animate Dead")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Animate Dead") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("the card leaving the graveyard in response sends Animate Dead to the graveyard and returns nothing") {
            val game = board()
            game.castAnimateDead()
            game.passPriority()
            game.passPriority()
            game.isOnBattlefield("Animate Dead") shouldBe true

            game.passPriority()
            game.execute(ActivateAbility(
                playerId = game.player2Id,
                sourceId = game.findPermanent("Tormod's Crypt")!!,
                abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player2Id))
            )).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInGraveyard(1, "Animate Dead") shouldBe true
        }
    }
}
