package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Momir Basic Vanguard format (Phase 1 engine core).
 *
 * Exercises the avatar's command-zone activated ability end to end through the real
 * [com.wingedsheep.engine.core.ActionProcessor]: enumeration, X-cost affordability, the
 * random-creature-by-mana-value token effect, determinism, the empty-pool no-op, sorcery-speed
 * gating, and the once-each-turn restriction.
 */
class MomirBasicScenarioTest : ScenarioTestBase() {

    private val avatar = "Momir Vig, Simic Visionary"

    /** The avatar's command-zone activated ability for player 1, augmented with X + a discard. */
    private fun com.wingedsheep.engine.support.ScenarioTestBase.TestGame.momirAction(
        xValue: Int,
        discard: Boolean = true,
    ): ActivateAbility {
        val base = getLegalActions(1)
            .map { it.action }
            .filterIsInstance<ActivateAbility>()
            .firstOrNull { state.getEntity(it.sourceId)?.get<CardComponent>()?.name == avatar }
            ?: error("Momir avatar ability not offered")
        val discardId = state.getZone(player1Id, Zone.HAND).firstOrNull()
        return base.copy(
            xValue = xValue,
            costPayment = if (discard && discardId != null) {
                AdditionalCostPayment(discardedCards = listOf(discardId))
            } else null
        )
    }

    init {
        test("avatar ability is offered at sorcery speed with X affordability = mana / 3") {
            val game = scenario()
                .withPlayers()
                .withFormat(Format.MomirBasic(eligibleCreatureNames = listOf("Savannah Lions")))
                .withCardInCommandZone(1, avatar)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardsInHand(1, "Mountain", 2)
                .build()

            val action = game.getLegalActions(1).firstOrNull {
                val a = it.action
                a is ActivateAbility &&
                    game.state.getEntity(a.sourceId)?.get<CardComponent>()?.name == avatar
            }
            action.shouldNotBeNull()
            action.hasXCost.shouldBeTrue()
            // {X}{X}{X} with 6 available mana ⇒ X up to 2 (3 mana per point of X).
            action.maxAffordableX shouldBe 2
        }

        test("activating with X=1 creates a token copy of a mana-value-1 creature") {
            val game = scenario()
                .withPlayers()
                .withFormat(Format.MomirBasic(eligibleCreatureNames = listOf("Savannah Lions")))
                .withCardInCommandZone(1, avatar)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardsInHand(1, "Mountain", 2)
                .build()

            game.execute(game.momirAction(xValue = 1)).error shouldBe null
            game.resolveStack()

            val tokens = game.findPermanents("Savannah Lions")
            tokens shouldHaveSize 1
            val token = game.state.getEntity(tokens.first())!!
            token.has<TokenComponent>().shouldBeTrue()
            token.get<ControllerComponent>()?.playerId shouldBe game.player1Id
        }

        test("the random copy is filtered to the chosen mana value") {
            // Pool has a mana-value-1 and a mana-value-4 creature; X=1 must only ever pick the 1.
            val game = scenario()
                .withPlayers()
                .withFormat(
                    Format.MomirBasic(
                        eligibleCreatureNames = listOf("Savannah Lions", "Phantom Warrior")
                    )
                )
                .withCardInCommandZone(1, avatar)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardsInHand(1, "Mountain", 2)
                .build()

            game.execute(game.momirAction(xValue = 1)).error shouldBe null
            game.resolveStack()

            game.findPermanents("Savannah Lions") shouldHaveSize 1
            game.findPermanents("Phantom Warrior") shouldHaveSize 0
        }

        test("no creature of the chosen mana value: no token, but the cost is still paid") {
            val game = scenario()
                .withPlayers()
                .withFormat(Format.MomirBasic(eligibleCreatureNames = listOf("Savannah Lions")))
                .withCardInCommandZone(1, avatar)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardsInHand(1, "Mountain", 2)
                .build()

            val handBefore = game.state.getZone(game.player1Id, Zone.HAND).size
            // X=2: pool's only creature is mana value 1, so nothing is created.
            game.execute(game.momirAction(xValue = 2)).error shouldBe null
            game.resolveStack()

            game.findPermanents("Savannah Lions") shouldHaveSize 0
            // Cost paid: a card was discarded (CR 608.2g — the ability still resolves, doing nothing).
            game.state.getZone(game.player1Id, Zone.HAND).size shouldBe handBefore - 1
            game.isInGraveyard(1, "Mountain").shouldBeTrue()
        }

        test("the random pick is deterministic: same seed + same pool picks the same creature") {
            fun runWithSeed(seed: Long): String {
                val game = scenario()
                    .withPlayers()
                    .withFormat(
                        Format.MomirBasic(
                            // Both mana value 1, so the choice is genuinely between two candidates.
                            eligibleCreatureNames = listOf("Savannah Lions", "Goblin Guide")
                        )
                    )
                    .withCardInCommandZone(1, avatar)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardsInHand(1, "Mountain", 2)
                    .withRngSeed(seed)
                    .build()
                game.execute(game.momirAction(xValue = 1))
                game.resolveStack()
                return game.state.getBattlefield()
                    .mapNotNull { game.state.getEntity(it) }
                    .filter { it.has<TokenComponent>() }
                    .mapNotNull { it.get<CardComponent>()?.name }
                    .single()
            }

            val first = runWithSeed(424242L)
            val second = runWithSeed(424242L)
            second shouldBe first
            listOf("Savannah Lions", "Goblin Guide") shouldContain first
        }

        test("the avatar ability can be activated only once each turn") {
            val game = scenario()
                .withPlayers()
                .withFormat(Format.MomirBasic(eligibleCreatureNames = listOf("Savannah Lions")))
                .withCardInCommandZone(1, avatar)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardsInHand(1, "Mountain", 2)
                .build()

            game.execute(game.momirAction(xValue = 1)).error shouldBe null
            game.resolveStack()

            // Second activation this turn is no longer offered (OncePerTurn).
            val offeredAgain = game.getLegalActions(1).any {
                val a = it.action
                a is ActivateAbility &&
                    game.state.getEntity(a.sourceId)?.get<CardComponent>()?.name == avatar
            }
            offeredAgain shouldBe false
        }

        test("the avatar ability is sorcery-speed only (not offered outside the main phase)") {
            val game = scenario()
                .withPlayers()
                .withFormat(Format.MomirBasic(eligibleCreatureNames = listOf("Savannah Lions")))
                .withCardInCommandZone(1, avatar)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardsInHand(1, "Mountain", 2)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()

            val offered = game.getLegalActions(1).any {
                val a = it.action
                a is ActivateAbility &&
                    game.state.getEntity(a.sourceId)?.get<CardComponent>()?.name == avatar
            }
            offered shouldBe false
        }
    }
}
