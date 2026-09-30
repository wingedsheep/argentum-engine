package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Blightbelly Rat (ONE #85) — {1}{B} 2/2 Creature — Phyrexian Rat.
 *
 * "Toxic 1. When this creature dies, proliferate."
 */
class BlightbellyRatScenarioTest : ScenarioTestBase() {

    private val slay = card("Test Slay") {
        manaCost = "{B}"
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        cardRegistry.register(slay)

        test("when it dies, its controller proliferates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Slay")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val rat = game.findPermanent("Blightbelly Rat")!!

            game.castSpell(1, "Test Slay", rat).error shouldBe null
            var prompts = 0
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) { game.selectCards(listOf(bears)); prompts++ } else game.resolveStack()
            }

            game.isInGraveyard(1, "Blightbelly Rat") shouldBe true
            prompts shouldBe 1
            counters(game, bears) shouldBe 2
        }

        test("has toxic 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .build()
            val rat = game.findPermanent("Blightbelly Rat")!!
            game.state.projectedState.hasKeyword(rat, "TOXIC_1") shouldBe true
            game.state.projectedState.hasKeyword(rat, Keyword.FLYING) shouldBe false
        }
    }
}
