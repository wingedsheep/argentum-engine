package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Gulping Scraptrap (ONE #96) — {4}{B} 4/4 Creature — Phyrexian Horror.
 *
 * "When this creature enters or dies, proliferate."
 */
class GulpingScraptrapScenarioTest : ScenarioTestBase() {

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

    private fun drain(game: TestGame, choice: List<EntityId>): Int {
        var prompts = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) { game.selectCards(choice); prompts++ } else game.resolveStack()
        }
        return prompts
    }

    init {
        cardRegistry.register(slay)

        test("when it enters, its controller proliferates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gulping Scraptrap")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)

            game.castSpell(1, "Gulping Scraptrap").error shouldBe null
            val prompts = drain(game, listOf(bears))

            (game.findPermanent("Gulping Scraptrap") != null) shouldBe true
            prompts shouldBe 1
            counters(game, bears) shouldBe 2
        }

        test("when it dies, its controller proliferates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Gulping Scraptrap")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Slay")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val trap = game.findPermanent("Gulping Scraptrap")!!

            game.castSpell(1, "Test Slay", trap).error shouldBe null
            val prompts = drain(game, listOf(bears))

            game.isInGraveyard(1, "Gulping Scraptrap") shouldBe true
            prompts shouldBe 1
            counters(game, bears) shouldBe 2
        }
    }
}
