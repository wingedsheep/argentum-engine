package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Avacyn, Guardian Angel (M15 #3) — {2}{W}{W}{W} Legendary Creature — Angel 5/4.
 *
 *   Flying, vigilance
 *   {1}{W}: Prevent all damage that would be dealt to another target creature this turn by sources
 *   of the color of your choice.
 *   {5}{W}{W}: Prevent all damage that would be dealt to target player or planeswalker this turn
 *   by sources of the color of your choice.
 *
 * The color is chosen as each ability resolves and the shield keeps it for the rest of the turn;
 * a source's color is checked when its damage would be dealt, so an opponent's Lightning Bolt cast
 * after the ability resolved is covered when red was chosen and not otherwise.
 */
class AvacynGuardianAngelScenarioTest : ScenarioTestBase() {

    private fun board(lands: Int): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Avacyn, Guardian Angel")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", lands)
        .withLandsOnBattlefield(2, "Mountain", 1)
        .withCardInHand(2, "Lightning Bolt")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Player 2 (active) passes, Player 1 activates Avacyn and picks [color]; priority returns to Player 2. */
    private fun TestGame.activate(abilityIndex: Int, target: ChosenTarget, color: Color) {
        passPriority().error shouldBe null
        val avacyn = findPermanent("Avacyn, Guardian Angel")!!
        val abilityId = cardRegistry.getCard("Avacyn, Guardian Angel")!!.activatedAbilities[abilityIndex].id
        val result = execute(ActivateAbility(playerId = player1Id, sourceId = avacyn, abilityId = abilityId, targets = listOf(target)))
        withClue("activation: ${result.error}") { result.error shouldBe null }
        if (hasPendingDecision() && getPendingDecision() !is ChooseColorDecision) submitManaSourcesAutoPay()
        resolveStack()
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(ColorChosenResponse(decision.id, color)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.boltAndResolve(targetId: EntityId) {
        castSpell(2, "Lightning Bolt", targetId).error shouldBe null
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Avacyn, Guardian Angel") {

            test("{1}{W}: red chosen, a later Lightning Bolt to the creature is prevented") {
                val game = board(lands = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.activate(0, ChosenTarget.Permanent(bears), Color.RED)

                game.boltAndResolve(bears)
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("{1}{W}: another color chosen, the Bolt still kills the creature") {
                val game = board(lands = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.activate(0, ChosenTarget.Permanent(bears), Color.BLUE)

                game.boltAndResolve(bears)
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("{1}{W} targets another creature, never Avacyn herself") {
                val game = board(lands = 2)
                game.passPriority().error shouldBe null
                val avacyn = game.findPermanent("Avacyn, Guardian Angel")!!
                val abilityId = cardRegistry.getCard("Avacyn, Guardian Angel")!!.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = avacyn, abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(avacyn)))
                ).error shouldNotBe null
            }

            test("{5}{W}{W}: red chosen, a Lightning Bolt to the targeted player is prevented") {
                val game = board(lands = 7)
                game.activate(1, ChosenTarget.Player(game.player1Id), Color.RED)

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
