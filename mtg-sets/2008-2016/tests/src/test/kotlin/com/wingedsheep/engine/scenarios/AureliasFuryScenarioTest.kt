package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aurelia's Fury (GTC #144, {X}{R}{W}, Instant).
 *
 *   Aurelia's Fury deals X damage divided as you choose among any number of targets. Tap each
 *   creature dealt damage this way. Players dealt damage this way can't cast noncreature spells
 *   this turn.
 *
 * One collection of "dealt damage this way" feeds both riders: its creatures are tapped and its
 * players are locked out of noncreature spells. Cast on the opponent's main phase so the
 * restriction is visible in their sorcery-speed legal actions once priority returns to them.
 */
class AureliasFuryScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Aurelia's Fury")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, "Centaur Courser")  // 3/3
        .withCardOnBattlefield(2, "Grizzly Bears")    // untargeted
        .withCardInHand(2, "Grizzly Bears")
        .withCardInHand(2, "Divination")
        .withLandsOnBattlefield(2, "Island", 3)
        .withLandsOnBattlefield(2, "Forest", 2)
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Aurelia's Fury") {

            test("taps the damaged creature and stops the damaged player casting noncreature spells") {
                val game = board()
                val fury = game.findCardsInHand(1, "Aurelia's Fury").single()
                val courser = game.findPermanent("Centaur Courser")!!
                val bearsOnBoard = game.findPermanent("Grizzly Bears")!!
                val opponent = game.player2Id

                game.execute(
                    CastSpell(
                        game.player1Id, fury,
                        targets = listOf(ChosenTarget.Permanent(courser), ChosenTarget.Player(opponent)),
                        xValue = 2,
                        damageDistribution = mapOf(courser to 1, opponent to 1),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                withClue("the damaged Courser survives 1 damage and is tapped") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.state.getEntity(courser)?.has<TappedComponent>() shouldBe true
                }
                withClue("a creature the division didn't reach stays untapped") {
                    game.state.getEntity(bearsOnBoard)?.has<TappedComponent>() shouldBe false
                }

                val bearsInHand = game.findCardsInHand(2, "Grizzly Bears").single()
                val divination = game.findCardsInHand(2, "Divination").single()
                val actions = game.getLegalActions(2)
                withClue("creature spells are still allowed") {
                    actions.any { (it.action as? CastSpell)?.cardId == bearsInHand } shouldBe true
                }
                withClue("noncreature spells are not") {
                    actions.any { (it.action as? CastSpell)?.cardId == divination } shouldBe false
                }
            }

            test("a player the division didn't reach can still cast noncreature spells") {
                val game = board()
                val fury = game.findCardsInHand(1, "Aurelia's Fury").single()
                val courser = game.findPermanent("Centaur Courser")!!

                game.execute(
                    CastSpell(
                        game.player1Id, fury,
                        targets = listOf(ChosenTarget.Permanent(courser)),
                        xValue = 2,
                        damageDistribution = mapOf(courser to 2),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
                game.state.getEntity(courser)?.has<TappedComponent>() shouldBe true

                val divination = game.findCardsInHand(2, "Divination").single()
                game.getLegalActions(2).any { (it.action as? CastSpell)?.cardId == divination } shouldBe true
            }
        }
    }
}
