package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [DynamicAmount.DistinctNamesInCollections] — the number of different card names across the
 * named pipeline collections ("if you reveal three cards with different names").
 */
class DistinctNamesInCollectionsTest : FunSpec({
    val amounts = PredicateEvaluator(cardRegistry = null).amounts
    val player = EntityId.generate()

    fun card(name: String) = ComponentContainer.EMPTY.with(
        CardComponent(cardDefinitionId = name, name = name, manaCost = ManaCost.parse("{1}"), typeLine = TypeLine.creature())
    )

    val bearA = EntityId.generate()
    val bearB = EntityId.generate()
    val giant = EntityId.generate()
    val wurm = EntityId.generate()
    val state = GameState(turnOrder = listOf(player))
        .withEntity(bearA, card("Grizzly Bears"))
        .withEntity(bearB, card("Grizzly Bears"))
        .withEntity(giant, card("Hill Giant"))
        .withEntity(wurm, card("Craw Wurm"))

    fun count(vararg collections: Pair<String, List<EntityId>>, names: List<String> = collections.map { it.first }) =
        amounts.evaluate(
            state,
            DynamicAmount.DistinctNamesInCollections(names),
            EffectContext(null, player, pipeline = PipelineState(storedCollections = mapOf(*collections)))
        )

    test("three differently named cards count three") {
        count("found" to listOf(bearA, giant, wurm)) shouldBe 3
    }
    test("two copies of one card count once") {
        count("found" to listOf(bearA, bearB, giant)) shouldBe 2
    }
    test("the union of several collections is de-duplicated by name") {
        count("a" to listOf(bearA, giant), "b" to listOf(bearB, wurm)) shouldBe 3
    }
    test("an empty or missing collection counts zero") {
        count("found" to emptyList()) shouldBe 0
        count(names = listOf("missing")) shouldBe 0
    }
})
