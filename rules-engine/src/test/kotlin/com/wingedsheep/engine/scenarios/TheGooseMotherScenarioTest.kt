package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The Goose Mother {X}{G}{U} Legendary Creature — Bird Hydra 2/2 — Wilds of Eldraine #204.
 *
 * "Flying. The Goose Mother enters with X +1/+1 counters on it. When The Goose Mother enters,
 *  create half X Food tokens, rounded up. Whenever The Goose Mother attacks, you may sacrifice a
 *  Food. If you do, draw a card."
 *
 * The counters ride an enters-with replacement while the Food rides a separate enters trigger, and
 * both read [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastX] so the two resolutions agree
 * on the same announced X — including the corner where there is no announced X at all because the
 * creature was put onto the battlefield rather than cast. The attack ability's `feasibility` gate is
 * the other half: with no Food the "you may sacrifice a Food" question must not be asked.
 */
class TheGooseMotherScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Player 1 with the Goose Mother in hand and six lands — enough for X=4. */
    private fun castingGame(): TestGame = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "The Goose Mother")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withLandsOnBattlefield(1, "Island", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("The Goose Mother") {

            test("X=4 enters as a 6/6 with four +1/+1 counters and creates two Food") {
                val game = castingGame()

                game.castXSpell(1, "The Goose Mother", xValue = 4).error shouldBe null
                game.resolveStack()

                val goose = game.findPermanent("The Goose Mother").shouldNotBeNull()
                game.plusOneCounters(goose) shouldBe 4
                game.state.projectedState.getPower(goose) shouldBe 6
                game.state.projectedState.getToughness(goose) shouldBe 6
                withClue("half of four, rounded up") {
                    game.findPermanents("Food").size shouldBe 2
                }
            }

            test("X=3 makes three counters but still two Food — half X rounds up") {
                val game = castingGame()

                game.castXSpell(1, "The Goose Mother", xValue = 3).error shouldBe null
                game.resolveStack()

                val goose = game.findPermanent("The Goose Mother").shouldNotBeNull()
                game.plusOneCounters(goose) shouldBe 3
                game.state.projectedState.getPower(goose) shouldBe 5
                withClue("half of three, rounded up") {
                    game.findPermanents("Food").size shouldBe 2
                }
            }

            test("X=0 is a plain 2/2 with no counters and no Food") {
                val game = castingGame()

                game.castXSpell(1, "The Goose Mother", xValue = 0).error shouldBe null
                game.resolveStack()

                val goose = game.findPermanent("The Goose Mother").shouldNotBeNull()
                game.plusOneCounters(goose) shouldBe 0
                game.state.projectedState.getPower(goose) shouldBe 2
                game.state.projectedState.getToughness(goose) shouldBe 2
                game.findPermanents("Food").size shouldBe 0
            }

            test("attacking with a Food offers the sacrifice, and taking it draws a card") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "The Goose Mother", summoningSickness = false)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("The Goose Mother" to 2)).error shouldBe null
                game.resolveStack()

                withClue("a Food is available, so the optional sacrifice is offered") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                game.isOnBattlefield("Food") shouldBe false
                withClue("'if you do' draws in the same resolution") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("attacking with no Food never asks the unanswerable question") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "The Goose Mother", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("The Goose Mother" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the feasibility gate suppresses the prompt entirely") {
                    game.hasPendingDecision() shouldBe false
                }
                game.handSize(1) shouldBe 0
            }

            test("put onto the battlefield without being cast, X is zero") {
                // Thunderous Debut's bargained mode produces this for real: no announced X, so
                // neither the enters-with replacement nor the Food trigger has anything to read.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Thunderous Debut")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "The Goose Mother")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goose = game.state.getLibrary(game.player1Id).single()

                game.castSpellBargained(1, "Thunderous Debut", "Food").error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(goose))
                game.resolveStack()

                game.isOnBattlefield("The Goose Mother") shouldBe true
                game.plusOneCounters(goose) shouldBe 0
                game.state.projectedState.getPower(goose) shouldBe 2
                game.state.projectedState.getToughness(goose) shouldBe 2
                withClue("half of zero Food — and the only Food paid for the bargain") {
                    game.findPermanents("Food").size shouldBe 0
                }
            }
        }
    }
}
