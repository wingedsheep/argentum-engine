package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "As this enters, choose two colors" (Tablet of the Guilds): the AI must answer with exactly two
 * colors — a one-color simulation can't — and, since the payoff is the spells it casts, the two its
 * own deck is made of.
 */
class ChooseTwoColorsDecisionTest : FunSpec({
    val tablet = card("AI Test Two-Color Tablet") {
        manaCost = "{2}"; typeLine = "Artifact"
        replacementEffect(EntersWithChoice(ChoiceType.COLOR, colorCount = 2))
        triggeredAbility {
            trigger = Triggers.you.casts()
            interveningIf = Conditions.TriggeringSpellMatches(GameObjectFilter.Any.sharingChosenColorWithSource())
            effect = Effects.ForEachChosenColor(
                effect = Effects.If(
                    Conditions.TriggeringSpellMatches(GameObjectFilter.Any.withChosenColor()),
                    Effects.GainLife(1)
                )
            )
        }
    }
    val registry = CardRegistry().apply { register(TestCards.all + tablet) }

    test("the AI names the two colors its own deck casts") {
        // R/G: red and green pips in hand and library, nothing white or blue anywhere.
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + tablet)
            initGame(Deck.of("Mountain" to 20, "Forest" to 20), Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val p1 = d.player1
        d.putCardInHand(p1, "Grizzly Bears")
        d.putCardInHand(p1, "Raging Goblin")
        d.putCardInHand(p1, "Lava Axe")
        val id = d.putCardInHand(p1, tablet.name)
        d.giveMana(p1, Color.RED, 2)
        d.castSpell(p1, id).error shouldBe null
        d.bothPass()
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()

        for (profile in listOf(AiProfile.PRODUCTION, AiProfile.PRODUCTION_CHOICES)) {
            val answer = AIPlayer.create(registry, p1, profile).respondToDecision(d.state, decision)
                .shouldBeInstanceOf<ColorChosenResponse>()
            answer.colors.toSet().size shouldBe 2
            (answer.color in answer.colors) shouldBe true
        }
        val informed = AIPlayer.create(registry, p1, AiProfile.PRODUCTION_CHOICES)
            .respondToDecision(d.state, decision) as ColorChosenResponse
        informed.colors shouldContainExactlyInAnyOrder listOf(Color.RED, Color.GREEN)
        d.submitDecision(p1, informed).error shouldBe null
    }
})
