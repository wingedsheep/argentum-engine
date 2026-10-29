package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "If an opponent controls more creatures / lands than you" asks about each opponent on their own
 * (Garruk, Unleashed's −2; Weathered Wayfarer), not the opponents' combined total — in a
 * multiplayer game two small boards must not add up to "more than you".
 */
class OpponentControlsMoreConditionTest : FunSpec({

    val bear = card("Opponent Count Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun threeSeats(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bear))
        driver.initMultiplayer(decks = List(3) { Deck.of("Forest" to 40) })
        return driver
    }

    fun holds(driver: GameTestDriver) = PredicateEvaluator(cardRegistry = null).conditions.evaluate(
        driver.state,
        Conditions.OpponentControlsMoreCreatures,
        EffectContext(sourceId = null, controllerId = driver.player1),
    )

    test("two opponents whose combined creatures outnumber yours don't satisfy it") {
        val driver = threeSeats()
        val (you, first, second) = driver.state.activePlayers
        repeat(3) { driver.putCreatureOnBattlefield(you, "Opponent Count Bear") }
        repeat(2) { driver.putCreatureOnBattlefield(first, "Opponent Count Bear") }
        repeat(2) { driver.putCreatureOnBattlefield(second, "Opponent Count Bear") }

        holds(driver) shouldBe false
    }

    test("a single opponent with more creatures than you satisfies it") {
        val driver = threeSeats()
        val (you, first, _) = driver.state.activePlayers
        repeat(2) { driver.putCreatureOnBattlefield(you, "Opponent Count Bear") }
        repeat(3) { driver.putCreatureOnBattlefield(first, "Opponent Count Bear") }

        holds(driver) shouldBe true
    }
})
