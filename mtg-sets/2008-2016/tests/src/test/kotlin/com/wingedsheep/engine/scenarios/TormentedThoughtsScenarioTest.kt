package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tormented Thoughts (JOU #86, reprinted ORI #122) — {2}{B} Sorcery.
 *   As an additional cost to cast this spell, sacrifice a creature.
 *   Target player discards a number of cards equal to the sacrificed creature's power.
 *
 * Pins the cost-linked amount: the creature is gone before the spell resolves, so the discard
 * count must come from its cost-payment snapshot (2014-04-26 ruling), and the targeted player
 * chooses which cards they discard.
 */
class TormentedThoughtsScenarioTest : ScenarioTestBase() {

    init {
        test("target player discards cards equal to the sacrificed creature's power, choosing them") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Tormented Thoughts")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val handBefore = game.handSize(2)

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Tormented Thoughts").single(),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    additionalCostPayment = AdditionalCostPayment(
                        sacrificedPermanents = listOf(game.findPermanent("Hill Giant")!!)
                    ),
                )
            ).error shouldBe null

            withClue("Hill Giant is sacrificed as the spell is cast") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Bob chooses the cards he discards") {
                decision.playerId shouldBe game.player2Id
            }
            withClue("discard count equals the sacrificed Hill Giant's last-known power (3)") {
                decision.minSelections shouldBe 3
                decision.maxSelections shouldBe 3
            }
            val keep = game.findCardsInHand(2, "Swamp").single()
            game.selectCards(decision.options.filter { it != keep }).error shouldBe null

            game.handSize(2) shouldBe handBefore - 3
            game.findCardsInHand(2, "Swamp").single() shouldBe keep
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Tormented Thoughts") shouldBe true
        }
    }
}
