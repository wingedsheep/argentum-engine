package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Render Inert ({2}{B} Sorcery): "Remove up to five counters from target permanent. Draw a card."
 */
class RenderInertScenarioTest : ScenarioTestBase() {

    private fun addCounters(game: TestGame, id: EntityId, vararg counters: Pair<CounterType, Int>) {
        game.state = game.state.updateEntity(id) { container ->
            var comp = container.get<CountersComponent>() ?: CountersComponent()
            counters.forEach { (type, amount) -> comp = comp.withAdded(type, amount) }
            container.with(comp)
        }
    }

    private fun totalCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0

    private fun baseGame(targetOnBattlefield: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Render Inert")
        .withCardOnBattlefield(2, targetOnBattlefield)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("removes at most five counters in total across kinds, then draws a card") {
            val game = baseGame("Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!
            addCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE to 4, CounterType.STUN to 3)
            val handBefore = game.handSize(1)

            game.castSpell(1, "Render Inert", bears).error shouldBe null
            game.resolveStack()

            var prompts = 0
            while (game.getPendingDecision() is ChooseNumberDecision) {
                val d = game.getPendingDecision() as ChooseNumberDecision
                game.chooseNumber(d.maxValue).error shouldBe null
                if (prompts++ > 5) error("budget not enforced")
            }

            withClue("Five of seven counters removed; two remain") {
                totalCounters(game, bears) shouldBe 2
            }
            withClue("Render Inert left hand, one card drawn") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("can target a noncreature permanent and remove fewer than five") {
            val game = baseGame("Forest")
            val forest = game.findPermanent("Forest")!!
            addCounters(game, forest, CounterType.CHARGE to 3)
            val handBefore = game.handSize(1)

            game.castSpell(1, "Render Inert", forest).error shouldBe null
            game.resolveStack()

            val d = game.getPendingDecision() as ChooseNumberDecision
            d.maxValue shouldBe 3
            game.chooseNumber(1).error shouldBe null

            totalCounters(game, forest) shouldBe 2
            game.handSize(1) shouldBe handBefore
        }

        test("a permanent without counters still resolves and draws") {
            val game = baseGame("Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!
            val handBefore = game.handSize(1)

            game.castSpell(1, "Render Inert", bears).error shouldBe null
            game.resolveStack()

            (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
            game.handSize(1) shouldBe handBefore
        }
    }
}
