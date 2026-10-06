package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A characteristic-defining ability functions in every zone (CR 604.3), so a `*`/`*` creature card
 * in a library, hand or graveyard has the power its CDA computes — not its printed placeholder 0.
 * "You" in the CDA is the card's owner when it has no controller (CR 108.4a), whoever is asking.
 */
class OffBattlefieldCharacteristicStatsTest : FunSpec({

    // "Power and toughness are each equal to the number of creatures you control", toughness +1.
    val counter = card("CDA Counter") {
        manaCost = "{3}"
        typeLine = "Creature — Horror"
        dynamicStats(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(),
            toughnessOffset = 1
        )
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + counter)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.matches(entity: EntityId, filter: GameObjectFilter, asker: EntityId) =
        services.predicateEvaluator.matches(
            state, state.projectedState, entity, filter, PredicateContext(controllerId = asker)
        )

    test("a library card's power is its CDA evaluated against its owner's battlefield") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val inLibrary = d.putCardOnTopOfLibrary(me, counter.name)
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.matches(inLibrary, GameObjectFilter.Creature.powerAtMost(2), me) shouldBe true
        d.matches(inLibrary, GameObjectFilter.Creature.powerAtMost(1), me) shouldBe false
        d.matches(inLibrary, GameObjectFilter.Creature.powerAtLeast(2), me) shouldBe true

        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.matches(inLibrary, GameObjectFilter.Creature.powerAtMost(2), me) shouldBe false
    }

    test("the CDA reads the owner's board even when an opponent's effect asks") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val inHand = d.putCardInHand(me, counter.name)
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        // Owner controls no creatures: 0/1, whoever evaluates the filter.
        d.matches(inHand, GameObjectFilter.Creature.powerAtMost(0), opp) shouldBe true
        d.matches(inHand, GameObjectFilter.Creature.powerAtLeast(1), opp) shouldBe false
    }

    test("a DynamicWithOffset toughness keeps its offset off the battlefield") {
        val d = driver(); val me = d.activePlayer!!
        val inGraveyard = d.putCardInGraveyard(me, counter.name)
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.matches(inGraveyard, GameObjectFilter.Creature.toughnessAtLeast(3), me) shouldBe true
        d.matches(inGraveyard, GameObjectFilter.Creature.toughnessAtLeast(4), me) shouldBe false
    }

    test("an EntityProperty power read of a graveyard card evaluates its CDA from the card's side") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val inGraveyard = d.putCardInGraveyard(me, counter.name)
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val power = DynamicAmount.EntityProperty(EffectTarget.SpecificEntity(inGraveyard), EntityNumericProperty.Power)
        // Evaluated from the opponent's context, still counts the owner's one creature.
        d.services.predicateEvaluator.amounts.evaluate(d.state, power, EffectContext(sourceId = null, controllerId = opp)) shouldBe 1
    }
})
