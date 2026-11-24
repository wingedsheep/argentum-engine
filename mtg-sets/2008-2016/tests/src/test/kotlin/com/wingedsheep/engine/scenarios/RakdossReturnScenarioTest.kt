package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rakdos's Return (RTR #188) — "Rakdos's Return deals X damage to target opponent or planeswalker.
 * That player or that planeswalker's controller discards X cards."
 *
 * The discard lands on the targeted opponent, or on the targeted planeswalker's controller, and that
 * player chooses the cards from their own hand.
 */
class RakdossReturnScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Rakdos's Return")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Forest")
        .withCardInHand(2, "Forest")
        .withCardInHand(2, "Island")
        .withCardInHand(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.cast(target: ChosenTarget, x: Int) {
        val spell = findCardsInHand(1, "Rakdos's Return").first()
        execute(CastSpell(player1Id, spell, listOf(target), x)).error shouldBe null
        resolveStack()
    }

    init {
        test("a targeted opponent takes X damage and discards X cards of their choice") {
            val game = setup().build()
            val life = game.getLifeTotal(2)
            val toss = game.state.getHand(game.player2Id).take(2)

            game.cast(ChosenTarget.Player(game.player2Id), x = 2)

            game.getLifeTotal(2) shouldBe life - 2
            val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent chooses from their own hand") { select.playerId shouldBe game.player2Id }
            game.selectCards(toss)

            game.handSize(2) shouldBe 1
            game.state.getGraveyard(game.player2Id) shouldContainAll toss
            withClue("the caster discards nothing") { game.handSize(1) shouldBe 1 }
        }

        test("a targeted planeswalker's controller discards X cards") {
            val game = setup().withCardOnBattlefield(2, "Garruk Wildspeaker").build()
            val garruk = game.findPermanent("Garruk Wildspeaker")!!
            game.state = game.state.updateEntity(garruk) {
                it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
            }
            val life = game.getLifeTotal(2)
            val toss = game.state.getHand(game.player2Id).take(2)

            game.cast(ChosenTarget.Permanent(garruk), x = 2)

            withClue("damage goes to the planeswalker, not the player") {
                game.getLifeTotal(2) shouldBe life
                game.state.getEntity(garruk)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
            }
            val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the planeswalker's controller chooses") { select.playerId shouldBe game.player2Id }
            game.selectCards(toss)

            game.handSize(2) shouldBe 1
            game.state.getGraveyard(game.player2Id) shouldContainAll toss
            game.handSize(1) shouldBe 1
        }
    }
}
