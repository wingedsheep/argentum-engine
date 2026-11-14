package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
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
 * count must come from its cost-payment snapshot (2014-04-26 ruling) — a Hill Giant carrying a
 * +1/+1 counter makes Bob discard 4, its last-known power, not its printed 3 — and the targeted
 * player chooses which cards they discard.
 */
class TormentedThoughtsScenarioTest : ScenarioTestBase() {

    init {
        test("target player discards cards equal to the sacrificed creature's last-known power, choosing them") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Tormented Thoughts")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Swamp")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) {
                it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
            withClue("setup: the countered Hill Giant has power 4, above its printed 3") {
                game.state.projectedState.getPower(giant) shouldBe 4
            }
            val handBefore = game.handSize(2)

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Tormented Thoughts").single(),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    additionalCostPayment = AdditionalCostPayment(
                        sacrificedPermanents = listOf(giant)
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
            withClue("discard count equals the sacrificed Hill Giant's last-known power (4), not its printed 3") {
                decision.minSelections shouldBe 4
                decision.maxSelections shouldBe 4
            }
            val keep = game.findCardsInHand(2, "Swamp").single()
            game.selectCards(decision.options.filter { it != keep }).error shouldBe null

            game.handSize(2) shouldBe handBefore - 4
            game.findCardsInHand(2, "Swamp").single() shouldBe keep
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Tormented Thoughts") shouldBe true
        }
    }
}
