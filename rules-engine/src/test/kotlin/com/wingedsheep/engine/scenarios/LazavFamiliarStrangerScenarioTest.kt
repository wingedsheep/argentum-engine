package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lazav, Familiar Stranger (OTJ #216, {1}{U}{B} 1/4 Shapeshifter).
 *
 *   Whenever you commit a crime, put a +1/+1 counter on Lazav. Then you may exile a card from a
 *   graveyard. If a creature card was exiled this way, you may have Lazav become a copy of that
 *   card until end of turn. This ability triggers only once each turn.
 *
 * Exercises the new `EachPermanentBecomesCopyOfTarget(sourceFromAnyZone = true)` path — Lazav
 * copies a creature card sitting in exile, not on the battlefield.
 */
class LazavFamiliarStrangerScenarioTest : ScenarioTestBase() {

    init {
        test("commit a crime: +1/+1 counter, exile a creature from a graveyard, become a copy of it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lazav, Familiar Stranger")
                .withCardInGraveyard(2, "Skeletal Snake") // {1}{B} 2/1 creature, in opponent's graveyard
                .withCardInHand(1, "Lightning Bolt")        // the crime: target the opponent
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lazav = game.findPermanents("Lazav, Familiar Stranger").first()

            // Commit a crime by targeting the opponent.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            game.resolveStack()

            // The trigger added a +1/+1 counter and now asks which graveyard card to exile.
            withClue("Lazav got a +1/+1 counter from the crime trigger") {
                game.state.getEntity(lazav)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
            val snakeInGrave = game.findCardsInGraveyard(2, "Skeletal Snake").first()
            game.selectCards(listOf(snakeInGrave))

            // A creature card was exiled — accept the "become a copy" option.
            game.answerYesNo(true)
            game.resolveStack()

            // Lazav is now a copy of Skeletal Snake (base 2/1), keeping its +1/+1 counter → 3/2.
            withClue("Lazav copied Skeletal Snake's name") {
                game.state.getEntity(lazav)?.get<CardComponent>()?.name shouldBe "Skeletal Snake"
            }
            withClue("Copy keeps Lazav's +1/+1 counter: 2/1 base + 1/1 = 3/2") {
                game.state.projectedState.getPower(lazav) shouldBe 3
                game.state.projectedState.getToughness(lazav) shouldBe 2
            }
        }

        test("the ability triggers only once each turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lazav, Familiar Stranger")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lazav = game.findPermanents("Lazav, Familiar Stranger").first()

            // First crime: trigger fires. Graveyards are empty, so there's nothing to exile and the
            // trigger just adds the +1/+1 counter.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectCards(emptyList()) // decline if prompted
            game.resolveStack()
            game.state.getEntity(lazav)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

            // Second crime same turn: oncePerTurn means no second trigger, so no second counter.
            game.castSpellTargetingPlayer(1, "Shock", 2)
            game.resolveStack()
            withClue("oncePerTurn: the second crime does not add another counter") {
                game.state.getEntity(lazav)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }
    }
}
