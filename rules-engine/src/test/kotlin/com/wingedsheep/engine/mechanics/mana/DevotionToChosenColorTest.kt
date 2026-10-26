package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `DynamicAmount.DevotionTo(ofChosenColor = true)` — "your devotion to that color" for the color
 * chosen earlier in the same resolution (CR 700.5). Two producers of that color: a wrapping
 * `ChooseColorThenEffect`, and `AddManaOfChoiceEffect` itself, which sizes its mana for the color
 * just picked — at activation (`manaColorChoice`) or through its own color prompt (Nykthos,
 * Shrine to Nyx; Nyx Lotus).
 */
class DevotionToChosenColorTest : FunSpec({

    // Green devotion 3 ({G/W}{G/W}{G}), white devotion 2 (the hybrid halves); {1} counts for nothing.
    val devotee = card("Hybrid Devotee") {
        manaCost = "{1}{G/W}{G/W}{G}"
        typeLine = "Creature — Test"
        power = 2
        toughness = 2
    }

    val prism = card("Devotion Prism") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddAnyColorMana(DynamicAmounts.devotionToChosenColor())
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val shrine = card("Devotion Shrine") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.ChooseColorThen(Effects.GainLife(DynamicAmounts.devotionToChosenColor()))
        }
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(devotee, prism, shrine))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Hybrid Devotee")
        d.putCreatureOnBattlefield(you, "Savannah Lions")
        // An opponent's devotion is theirs, never yours.
        d.putCreatureOnBattlefield(d.getOpponent(you), "Force of Nature")
        return d to you
    }

    fun GameTestDriver.pool(p: EntityId) = state.getEntity(p)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("a color picked at activation sizes the mana by devotion to that color") {
        val (d, you) = setup()
        val p = d.putPermanentOnBattlefield(you, "Devotion Prism")
        d.submit(ActivateAbility(you, p, prism.activatedAbilities.first().id, manaColorChoice = Color.GREEN)).error shouldBe null
        d.pool(you).green shouldBe 3
        d.pool(you).white shouldBe 0
    }

    test("a hybrid symbol counts toward each of its colors") {
        val (d, you) = setup()
        val p = d.putPermanentOnBattlefield(you, "Devotion Prism")
        d.submit(ActivateAbility(you, p, prism.activatedAbilities.first().id, manaColorChoice = Color.WHITE)).error shouldBe null
        d.pool(you).white shouldBe 3 // {G/W}{G/W} + Savannah Lions' {W}
    }

    test("a color picked through the ability's own prompt sizes the mana the same way") {
        val (d, you) = setup()
        val p = d.putPermanentOnBattlefield(you, "Devotion Prism")
        d.submit(ActivateAbility(you, p, prism.activatedAbilities.first().id)).error shouldBe null
        d.submitDecision(you, ColorChosenResponse(d.pendingDecision!!.id, Color.GREEN)).error shouldBe null
        d.pool(you).green shouldBe 3
    }

    test("a color with no devotion adds no mana") {
        val (d, you) = setup()
        val p = d.putPermanentOnBattlefield(you, "Devotion Prism")
        d.submit(ActivateAbility(you, p, prism.activatedAbilities.first().id, manaColorChoice = Color.BLACK)).error shouldBe null
        d.pool(you).isEmpty shouldBe true
    }

    test("ChooseColorThen's chosen color feeds the amount of an effect under it") {
        val (d, you) = setup()
        val s = d.putPermanentOnBattlefield(you, "Devotion Shrine")
        d.submit(ActivateAbility(you, s, shrine.activatedAbilities.first().id)).error shouldBe null
        d.bothPass()
        d.submitDecision(you, ColorChosenResponse(d.pendingDecision!!.id, Color.WHITE)).error shouldBe null
        d.getLifeTotal(you) shouldBe 23
    }

    test("auto-pay taps it for the color with devotion, sized by that devotion") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(you, "Devotion Prism")
        d.putLandOnBattlefield(you, "Forest")
        d.putLandOnBattlefield(you, "Forest")
        val force = d.putCardInHand(you, "Force of Nature") // {3}{G}{G}: 3 green from the Prism + 2 Forests
        d.castSpell(you, force).error shouldBe null
        d.state.stack.contains(force) shouldBe true
    }

    test("auto-pay never counts it for more than devotion to the color it makes") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(you, "Devotion Prism")
        d.putLandOnBattlefield(you, "Forest")
        val force = d.putCardInHand(you, "Force of Nature") // only 3 + 1 available
        d.castSpell(you, force).error shouldNotBe null
    }

    test("with no color chosen in context it reads only its fixed colors") {
        val (d, you) = setup()
        val evaluator = d.services.predicateEvaluator.amounts
        val bare = EffectContext(sourceId = null, controllerId = you)
        evaluator.evaluate(d.state, DynamicAmounts.devotionToChosenColor(), bare) shouldBe 0
        val greenPlusChosen = DynamicAmount.DevotionTo(listOf(Color.GREEN), ofChosenColor = true)
        evaluator.evaluate(d.state, greenPlusChosen, bare) shouldBe 3
        // A symbol matching both the fixed and the chosen color is counted once.
        evaluator.evaluate(d.state, greenPlusChosen, bare.copy(chosenColor = Color.GREEN)) shouldBe 3
        evaluator.evaluate(d.state, greenPlusChosen, bare.copy(chosenColor = Color.WHITE)) shouldBe 4
    }
})
