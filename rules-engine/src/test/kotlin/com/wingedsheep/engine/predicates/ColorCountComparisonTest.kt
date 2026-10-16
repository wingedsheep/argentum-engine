package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CardNumericProperty.COLOR_COUNT] — the number of colors an object has (CR 105.2) — read by
 * `CompareNumericProperty` ("a creature card that's exactly that many colors plus one", Evolving
 * Door), by battlefield aggregation, and the colors an [EntitySnapshot] freezes so a departed
 * permanent's color count is its last-known one (CR 608.2h).
 */
class ColorCountComparisonTest : FunSpec({

    val goldDuo = card("Gold Duo") {
        manaCost = "{R}{W}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(goldDuo))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.matches(entity: EntityId, filter: GameObjectFilter, context: PredicateContext) =
        services.predicateEvaluator.matches(state, state.projectedState, entity, filter, context)

    fun GameTestDriver.evaluate(amount: DynamicAmount, context: EffectContext) =
        services.predicateEvaluator.amounts.evaluate(state, amount, context)

    test("a dynamic exact color count matches only objects with that many colors") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val duo = d.putCreatureOnBattlefield(me, "Gold Duo")
        val context = PredicateContext(controllerId = me)
        val two = GameObjectFilter.Creature.exactlyColors(DynamicAmounts.colorCountOf(EffectTarget.SpecificEntity(bears)) + 1)

        d.matches(duo, two, context) shouldBe true
        d.matches(bears, two, context) shouldBe false
    }

    test("off the battlefield the printed colors are counted") {
        val d = driver(); val me = d.activePlayer!!
        val inGraveyard = d.putCardInGraveyard(me, "Gold Duo")
        d.matches(inGraveyard, GameObjectFilter.Creature.exactlyColors(DynamicAmount.Fixed(2)), PredicateContext(controllerId = me)) shouldBe true
    }

    test("battlefield aggregation sums color counts") {
        val d = driver(); val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Gold Duo")
        val total = DynamicAmount.AggregateBattlefield(
            Player.You, GameObjectFilter.Creature, Aggregation.SUM, CardNumericProperty.COLOR_COUNT
        )
        d.evaluate(total, EffectContext(sourceId = null, controllerId = me)) shouldBe 3
    }

    test("snapshots freeze the permanent's colors") {
        val d = driver(); val me = d.activePlayer!!
        val duo = d.putCreatureOnBattlefield(me, "Gold Duo")
        EntitySnapshot.fromProjection(duo, d.state).colors shouldBe setOf("RED", "WHITE")
        captureEntitySnapshots(listOf(duo), d.state).single().colors shouldBe setOf("RED", "WHITE")
    }

    test("a permanent that left the battlefield keeps its colors in its departure snapshot") {
        val d = driver(); val me = d.activePlayer!!
        val duo = d.putCreatureOnBattlefield(me, "Gold Duo")
        val moved = d.services.zones.moveToZone(d.state, duo, Zone.GRAVEYARD).state
        moved.getEntity(duo)?.get<LastKnownPermanentComponent>()?.snapshot?.colors shouldBe setOf("RED", "WHITE")
    }

    test("a sacrificed object that no longer exists is counted from its cost-time snapshot") {
        val d = driver(); val me = d.activePlayer!!
        val gone = EntityId.generate()
        val context = EffectContext(
            sourceId = null,
            controllerId = me,
            sacrificedPermanents = listOf(EntitySnapshot(entityId = gone, colors = setOf("RED", "WHITE"))),
        )
        withClue("the token ceased to exist, so only last-known information can answer") {
            d.evaluate(DynamicAmounts.colorCountOf(EffectTarget.SacrificedAsCost(0)), context) shouldBe 2
        }
    }
})
