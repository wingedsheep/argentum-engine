package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SetColors
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A card's own printed "[this] is all colors" (a self-filter [SetColors] at the top of its script —
 * Sphinx of the Guildpact) is a characteristic-defining ability: CR 604.3 makes it function in every
 * zone, and CR 613.3 applies CDAs before every other effect in their layer. So the SDK folds the
 * colors into the card's own colors and the engine doesn't also project them as a timestamped
 * layer-5 effect.
 */
class SelfDefinedColorCdaTest : FunSpec({

    val allColors = Color.entries.toSet()

    val rainbow = card("CDA Probe Rainbow") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Sphinx"
        power = 2
        toughness = 2
        staticAbility { ability = SetColors(allColors, GroupFilter.source()) }
    }

    // "As long as this is tapped, it's all colors" — conditional, so not a CDA (CR 604.3a).
    val conditionalRainbow = card("CDA Probe Conditional Rainbow") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
        staticAbility {
            ability = SetColors(allColors, GroupFilter.source())
            condition = Conditions.SourceIsTapped
        }
    }

    // "All creatures are black" — a layer-5 color-setting effect from another permanent.
    val darken = card("CDA Probe Darken") {
        manaCost = "{B}"
        typeLine = "Enchantment"
        staticAbility { ability = SetColors(setOf(Color.BLACK), GroupFilter.AllCreatures) }
    }

    // "Blue spells you cast cost {1} less to cast."
    val blueDiscount = card("CDA Probe Blue Discount") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.BLUE)),
                modification = CostModification.ReduceGeneric(1),
            )
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(rainbow, conditionalRainbow, darken, blueDiscount))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
    }

    fun GameTestDriver.cardColors(id: EntityId): Set<Color> = state.getEntity(id)!!.get<CardComponent>()!!.colors

    test("the definition derives the self colors and their identity; a conditional one is not derived") {
        rainbow.selfDefinedColors shouldBe allColors
        rainbow.colors shouldBe allColors
        rainbow.colorIdentity shouldBe allColors
        conditionalRainbow.selfDefinedColors shouldBe null
        conditionalRainbow.colors shouldBe emptySet()
        darken.selfDefinedColors shouldBe null
    }

    test("the CDA reads as '[this] is all colors'") {
        SetColors(allColors, GroupFilter.source()).description shouldBe "this creature is all colors"
    }

    test("CR 604.3: the card is all colors in hand and in the graveyard") {
        val d = driver()
        d.cardColors(d.putCardInHand(d.player1, rainbow.name)) shouldBe allColors
        d.cardColors(d.putCardInGraveyard(d.player1, rainbow.name)) shouldBe allColors
        d.cardColors(d.putCardInHand(d.player1, conditionalRainbow.name)) shouldBe emptySet()
    }

    test("CR 604.3: a spell being cast is judged by its CDA colors (blue spells cost {1} less)") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, blueDiscount.name)
        val calculator = CostCalculator(d.cardRegistry, PredicateEvaluator(cardRegistry = d.cardRegistry))
        calculator.calculateEffectiveCost(d.state, rainbow, d.player1) shouldBe ManaCost.parse("{2}")
        calculator.calculateEffectiveCost(d.state, conditionalRainbow, d.player1) shouldBe ManaCost.parse("{3}")
    }

    test("on the battlefield the permanent projects as all colors") {
        val d = driver()
        val probe = d.putCreatureOnBattlefield(d.player1, rainbow.name)
        d.state.projectedState.getColors(probe) shouldBe allColors.mapTo(mutableSetOf()) { it.name }
    }

    test("CR 613.3: an older 'all creatures are black' still overwrites the CDA colors") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, darken.name)
        val probe = d.putCreatureOnBattlefield(d.player1, rainbow.name)
        d.state.projectedState.getColors(probe) shouldBe setOf("BLACK")
    }
})
