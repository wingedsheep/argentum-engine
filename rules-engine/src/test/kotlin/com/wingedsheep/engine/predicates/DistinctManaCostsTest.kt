package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFace
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.values.Aggregation.DISTINCT_MANA_COSTS] — "for each different
 * mana cost among nonland cards in your graveyard" (Embodiment of Agonies). Its rulings: costs
 * compare by symbol, not mana value; {X} differs from {0}; a hybrid symbol is its own symbol; a
 * card with no mana cost never counts but a {0} does; a split card's cost is its halves combined.
 */
class DistinctManaCostsTest : FunSpec({

    val you = EntityId.generate()

    // Fire // Ice — {1}{R} and {1}{U}, so {2}{U}{R} in the graveyard (CR 709.4b).
    val fireIce = card("Fire // Ice") {
        layout = CardLayout.SPLIT
        face("Fire") { manaCost = "{1}{R}"; typeLine = "Instant" }
        face("Ice") { manaCost = "{1}{U}"; typeLine = "Instant" }
    }
    val registry = CardRegistry().apply { register(fireIce) }

    data class Card(
        val cost: String,
        val name: String = cost,
        val type: CardType = CardType.SORCERY,
        val faceDown: Boolean = false,
    )

    fun graveyardOf(vararg cards: Card): GameState {
        var state = GameState().withEntity(you, ComponentContainer())
        cards.forEach { c ->
            val id = EntityId.generate()
            var container = ComponentContainer()
                .with(
                    CardComponent(
                        cardDefinitionId = c.name,
                        name = c.name,
                        manaCost = ManaCost.parse(c.cost),
                        typeLine = TypeLine(cardTypes = setOf(c.type)),
                        ownerId = you
                    )
                )
                .with(OwnerComponent(you))
                .with(ControllerComponent(you))
            if (c.faceDown) container = container.with(FaceDownComponent)
            state = state.withEntity(id, container).addToZone(ZoneKey(you, Zone.GRAVEYARD), id)
        }
        return state
    }

    fun GameState.distinctCosts(evaluator: PredicateEvaluator = PredicateEvaluator(cardRegistry = registry)) =
        evaluator.amounts.evaluate(
            this,
            DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Nonland).distinctManaCosts(),
            EffectContext(sourceId = null, controllerId = you)
        )

    test("costs with the same mana value but different symbols are different") {
        graveyardOf(Card("{2}{B}"), Card("{1}{B}{B}")).distinctCosts() shouldBe 2
    }

    test("the same cost twice counts once, whatever order its symbols are written in") {
        graveyardOf(Card("{1}{B}", "A"), Card("{1}{B}", "B"), Card("{B}{1}", "C")).distinctCosts() shouldBe 1
    }

    test("{X} and {0} are different costs") {
        graveyardOf(Card("{X}"), Card("{0}")).distinctCosts() shouldBe 2
    }

    test("a hybrid symbol is neither of its halves") {
        graveyardOf(Card("{B/R}"), Card("{B}"), Card("{R}")).distinctCosts() shouldBe 3
    }

    test("a card with no mana cost never counts, a {0} card does, and lands are filtered out") {
        graveyardOf(Card("", "Ancestral Vision"), Card("{0}"), Card("", "Forest", CardType.LAND))
            .distinctCosts() shouldBe 1
    }

    test("a split card's cost is its two halves combined") {
        graveyardOf(Card("{1}{R}", "Fire // Ice", CardType.INSTANT), Card("{2}{U}{R}", "Ral's Outburst"))
            .distinctCosts() shouldBe 1
        graveyardOf(Card("{1}{R}", "Fire // Ice", CardType.INSTANT), Card("{1}{R}", "Shock"))
            .distinctCosts() shouldBe 2
    }

    test("a Room in the graveyard has both halves' costs, whatever was unlocked when it left") {
        val state = graveyardOf(Card("{1}{R}", "Fire // Ice", CardType.ENCHANTMENT), Card("{1}{R}", "Shock"))
        val roomId = state.getZone(ZoneKey(you, Zone.GRAVEYARD)).first()
        val lockedRoom = RoomComponent(
            faces = listOf(RoomFace(RoomFaceId("Fire"), "Fire", ManaCost.parse("{1}{R}")),
                RoomFace(RoomFaceId("Ice"), "Ice", ManaCost.parse("{1}{U}"))),
            unlocked = emptySet(),
        )
        // {2}{U}{R} and Shock's {1}{R}: a locked-room reading would see only Shock.
        state.updateEntity(roomId) { it.with(lockedRoom) }.distinctCosts() shouldBe 2
    }

    test("a face-down card has no mana cost") {
        graveyardOf(Card("{3}", faceDown = true)).distinctCosts() shouldBe 0
    }
})
