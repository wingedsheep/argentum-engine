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
 * Scenario tests for Baron Strucker, HYDRA Overlord (MSH #88) — {2}{B} Legendary Creature —
 * Human Villain 2/2.
 *
 * "Villain spells you cast cost {1} less to cast."
 * "Whenever another Villain you control enters, you may have it connive. **Do this only once each
 *  turn.**"
 *
 * The rider is `TriggeredAbility.effectOncePerTurn`, not the trigger cap. Per CR 603.2h the ability
 * "triggers only if its source's controller has not yet taken the indicated action that turn", so
 * until something has connived every Villain entering triggers it and the controller gets to pick
 * *which* one connives; once one has, the ability stops triggering for the turn. With `oncePerTurn`
 * a declined first Villain would burn the turn's only fire and that choice would be gone. The
 * primitive itself is covered by [EffectOncePerTurnTest]; this file covers the card.
 */
class BaronStruckerScenarioTest : ScenarioTestBase() {

    init {
        /** Baron Strucker out, two Villains to cast, and a nonland card to discard to connive. */
        fun board(): TestGame = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Baron Strucker, HYDRA Overlord")
            .withCardsInHand(1, "Agents of HYDRA", 2)
            .withCardInHand(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Hill Giant")
            .withLandsOnBattlefield(1, "Swamp", 6)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        /** How many Villains on the battlefield carry a +1/+1 counter (i.e. connived). */
        fun connivedCount(game: TestGame): Int =
            game.findAllPermanents("Agents of HYDRA").count { id ->
                (game.state.getEntity(id)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) > 0
            }

        /** Discard the Grizzly Bears (a nonland, so the connive also places a counter). */
        fun discardTheBears(game: TestGame) {
            val bears = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            game.selectCards(listOf(bears))
            game.resolveStack()
        }

        context("Baron Strucker — the connive trigger") {

            test("the second Villain still triggers after the first is declined") {
                val game = board()

                game.castSpell(1, "Agents of HYDRA").error shouldBe null
                game.resolveStack()
                withClue("the first Villain entering offers the connive") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)
                game.resolveStack()

                // A trigger cap would have consumed the turn's only fire on that decline; the effect
                // cap did not, so the second Villain is still offered.
                game.castSpell(1, "Agents of HYDRA").error shouldBe null
                game.resolveStack()
                withClue("declining did not spend the once-each-turn budget") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()
                discardTheBears(game)

                withClue("the discarded nonland put a +1/+1 counter on the conniving Villain") {
                    connivedCount(game) shouldBe 1
                }
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("only one Villain may connive each turn") {
                val game = board()

                game.castSpell(1, "Agents of HYDRA").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()
                discardTheBears(game)
                connivedCount(game) shouldBe 1

                val graveyardAfterFirst = game.graveyardSize(1)

                game.castSpell(1, "Agents of HYDRA").error shouldBe null
                game.resolveStack()

                withClue("the budget is spent, so the second Villain raises no question") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no second connive happened") {
                    connivedCount(game) shouldBe 1
                    game.graveyardSize(1) shouldBe graveyardAfterFirst
                }
            }

            test("Baron Strucker's own entry does not trigger his ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Baron Strucker, HYDRA Overlord")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Baron Strucker, HYDRA Overlord").error shouldBe null
                game.resolveStack()

                withClue("'another Villain' excludes the source (TriggerBinding.OTHER)") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }

        context("Baron Strucker — the Villain discount") {

            test("a Villain spell costs {1} less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Baron Strucker, HYDRA Overlord")
                    .withCardInHand(1, "Agents of HYDRA")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("{1}{B} Agents of HYDRA is castable off a single Swamp") {
                    game.castSpell(1, "Agents of HYDRA").error shouldBe null
                }
                game.resolveStack()
                // Decline the connive so the assertion is about the cost, not the payoff.
                if (game.hasPendingDecision()) {
                    game.answerYesNo(false)
                    game.resolveStack()
                }
                game.isOnBattlefield("Agents of HYDRA") shouldBe true
            }
        }
    }
}
