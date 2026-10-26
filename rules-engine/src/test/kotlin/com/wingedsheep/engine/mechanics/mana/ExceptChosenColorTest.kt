package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaColorSet.ExceptChosenColor] — "any other color": its base pool minus the color chosen
 * earlier in the same resolution. The Open the Omenpaths scenario test drives it through
 * `ChooseColorThen`; these pin the resolver's arithmetic.
 */
class ExceptChosenColorTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
    }

    fun GameTestDriver.resolve(colorSet: ManaColorSet, chosen: Color?) = ManaColorSetResolver.resolve(
        colorSet = colorSet,
        state = state,
        projected = state.projectedState,
        sourceId = null,
        controllerId = activePlayer!!,
        cardRegistry = cardRegistry,
        predicateEvaluator = services.predicateEvaluator,
        chosenColor = chosen,
    )

    test("removes the chosen color from any color") {
        driver().resolve(ManaColorSet.ExceptChosenColor(), Color.GREEN) shouldBe
            setOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED)
    }

    test("with no color chosen it is its base unchanged") {
        driver().resolve(ManaColorSet.ExceptChosenColor(), null) shouldBe Color.entries.toSet()
    }

    test("narrows a constrained base, and a chosen color outside the base removes nothing") {
        val d = driver()
        val redOrGreen = ManaColorSet.ExceptChosenColor(ManaColorSet.Specific(setOf(Color.RED, Color.GREEN)))
        d.resolve(redOrGreen, Color.RED) shouldBe setOf(Color.GREEN)
        d.resolve(redOrGreen, Color.BLUE) shouldBe setOf(Color.RED, Color.GREEN)
    }

    test("the chosen color reaches an ExceptChosenColor nested in a Union") {
        val union = ManaColorSet.Union(
            listOf(ManaColorSet.Specific(setOf(Color.WHITE)), ManaColorSet.ExceptChosenColor(ManaColorSet.Specific(setOf(Color.BLUE, Color.BLACK))))
        )
        driver().resolve(union, Color.BLUE) shouldBe setOf(Color.WHITE, Color.BLACK)
    }
})
