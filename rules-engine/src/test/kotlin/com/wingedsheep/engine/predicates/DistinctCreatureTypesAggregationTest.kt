package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Aggregation.DISTINCT_CREATURE_TYPES] — "the number of creature types among <group>" (CR 205.3m),
 * the count behind Star Trek's Federation ability word ([DynamicAmounts.federation]).
 */
class DistinctCreatureTypesAggregationTest : FunSpec({

    val officer = card("Test Human Officer") {
        manaCost = "{W}"; typeLine = "Creature — Human Officer"; power = 1; toughness = 1
    }
    val scientist = card("Test Human Scientist") {
        manaCost = "{U}"; typeLine = "Creature — Human Scientist"; power = 1; toughness = 1
    }
    val drone = card("Test Borg Drone") {
        manaCost = "{B}"; typeLine = "Artifact Creature — Borg Drone Warrior"; power = 1; toughness = 1
    }
    val shapeshifter = card("Test Shapeshifter") {
        manaCost = "{1}"; typeLine = "Creature — Shapeshifter"; power = 1; toughness = 1
        keywords(Keyword.CHANGELING)
    }
    val forestWall = card("Test Forest Wall") {
        manaCost = "{G}"; typeLine = "Land Creature — Forest Wall"; power = 0; toughness = 3
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(officer, scientist, drone, shapeshifter, forestWall))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.evaluate(amount: DynamicAmount) =
        services.predicateEvaluator.amounts.evaluate(state, amount, EffectContext(sourceId = null, controllerId = activePlayer!!))

    test("shared creature types count once and non-creature subtypes never count") {
        val d = driver(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Test Human Officer")
        d.putCreatureOnBattlefield(me, "Test Human Scientist")
        d.putCreatureOnBattlefield(me, "Test Forest Wall")
        // Human, Officer, Scientist, Wall — Forest is a land type.
        d.evaluate(DynamicAmounts.creatureTypesAmong()) shouldBe 4
    }

    test("federation ignores Borg creatures and opponents' creatures") {
        val d = driver(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Test Human Officer")
        d.putCreatureOnBattlefield(me, "Test Borg Drone")
        d.putCreatureOnBattlefield(d.getOpponent(me), "Test Human Scientist")
        d.evaluate(DynamicAmounts.federation()) shouldBe 2
        d.evaluate(DynamicAmounts.creatureTypesAmong()) shouldBe 5
    }

    test("a changeling contributes every creature type") {
        val d = driver(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Test Shapeshifter")
        d.evaluate(DynamicAmounts.creatureTypesAmong()) shouldBe Subtype.ALL_CREATURE_TYPES.size
        // A changeling is a Borg too, so Federation never counts it.
        d.evaluate(DynamicAmounts.federation()) shouldBe 0
    }

    test("off the battlefield the printed creature types are counted") {
        val d = driver(); val me = d.activePlayer!!
        d.putCardInGraveyard(me, "Test Human Officer")
        d.putCardInGraveyard(me, "Test Borg Drone")
        val amount = DynamicAmount.AggregateZone(
            Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature, Aggregation.DISTINCT_CREATURE_TYPES
        )
        d.evaluate(amount) shouldBe 5
    }
})
