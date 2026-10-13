package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Legate Lanius, Caesar's Ace (PIP) — Decimate: "each opponent sacrifices a tenth of the creatures
 * they control of their choice, rounded up", and "Whenever an opponent sacrifices a creature, put a
 * +1/+1 counter on Legate Lanius."
 *
 * Pins the per-opponent count (`ForEachPlayer` rebinding `Player.You`), the round-up (1 → 1,
 * 11 → 2), that the caster's own creatures are untouched, and that Decimate's own sacrifices feed
 * the per-creature counter trigger.
 */
class LegateLaniusCaesarsAceScenarioTest : ScenarioTestBase() {

    private val legionary = card("Lanius Test Legionary") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 80) {
            when (val d = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections))
                null -> resolveStack()
                else ->
                    if (d::class.simpleName?.contains("Mana") == true) submitManaSourcesAutoPay()
                    else error("Unexpected decision: ${d::class.simpleName}")
            }
        }
    }

    private fun castLanius(yourCreatures: Int, opponentCreatures: Int): TestGame {
        var builder = scenario()
            .withPlayers("You", "Opponent")
            .withCardInHand(1, "Legate Lanius, Caesar's Ace")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(yourCreatures) { builder = builder.withCardOnBattlefield(1, legionary.name) }
        repeat(opponentCreatures) { builder = builder.withCardOnBattlefield(2, legionary.name) }
        val game = builder.build()
        game.castSpell(1, "Legate Lanius, Caesar's Ace").error shouldBe null
        game.resolveAll()
        return game
    }

    init {
        cardRegistry.register(legionary)

        test("eleven creatures: the opponent sacrifices two (a tenth, rounded up) and Lanius gets two counters") {
            val game = castLanius(yourCreatures = 3, opponentCreatures = 11)

            withClue("opponent sacrificed ceil(11 / 10) = 2") {
                game.findCardsInGraveyard(2, legionary.name).size shouldBe 2
            }
            withClue("the caster isn't an opponent — none of your creatures are sacrificed") {
                game.findCardsInGraveyard(1, legionary.name).size shouldBe 0
            }
            val lanius = game.findPermanent("Legate Lanius, Caesar's Ace")!!
            withClue("one +1/+1 counter per creature an opponent sacrificed") {
                game.plusOneCounters(lanius) shouldBe 2
            }
            game.state.projectedState.getPower(lanius) shouldBe 4
        }

        test("a single creature still means one sacrifice — the tenth rounds up") {
            val game = castLanius(yourCreatures = 0, opponentCreatures = 1)

            game.findCardsInGraveyard(2, legionary.name).size shouldBe 1
            game.plusOneCounters(game.findPermanent("Legate Lanius, Caesar's Ace")!!) shouldBe 1
        }

        test("ten creatures is exactly one sacrifice") {
            val game = castLanius(yourCreatures = 0, opponentCreatures = 10)

            game.findCardsInGraveyard(2, legionary.name).size shouldBe 1
            game.plusOneCounters(game.findPermanent("Legate Lanius, Caesar's Ace")!!) shouldBe 1
        }
    }
}
