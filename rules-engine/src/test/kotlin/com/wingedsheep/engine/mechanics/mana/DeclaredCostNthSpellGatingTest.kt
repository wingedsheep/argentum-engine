package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CostGating.NthOfTypePerTurn] with a `declaredCost`: "the first kicked spell you cast each turn
 * costs {1} less to cast" (Vine Gecko). The priced cast must declare the optional cost (announced
 * as the spell is cast, CR 601.2b, before its total cost is determined, CR 601.2f), and only
 * earlier casts this turn that declared the same cost count toward N.
 */
class DeclaredCostNthSpellGatingTest : FunSpec({

    val firstKickedDiscount = card("Test First Kicked Discount") {
        manaCost = "{1}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGeneric(1),
                gating = CostGating.NthOfTypePerTurn(1, declaredCost = ChoiceSlot.KICKED),
            )
        }
    }
    val kickerSorcery = card("Test Kicker Sorcery") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.kicker("{1}"))
    }
    val spell = card("Test Spell") { manaCost = "{2}{G}"; typeLine = "Sorcery" }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(firstKickedDiscount, kickerSorcery, spell))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.calculator() = CostCalculator(cardRegistry, PredicateEvaluator(cardRegistry = cardRegistry))

    fun record(slot: ChoiceSlot?) = CastSpellRecord(
        typeLine = TypeLine.parse("Sorcery"), manaValue = 1, colors = setOf(Color.GREEN),
        isFaceDown = false, declaredCostSlot = slot,
    )

    fun GameTestDriver.withHistory(vararg slots: ChoiceSlot?) =
        replaceState(state.copy(spellsCastThisTurnByPlayer = mapOf(player1 to slots.map { record(it) })))

    val discounted = ManaCost.parse("{1}{G}")

    test("only a cast that declares the cost is discounted") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, firstKickedDiscount.name)
        val calc = d.calculator()

        calc.calculateEffectiveCost(d.state, spell, d.player1, declaredCostSlot = ChoiceSlot.KICKED) shouldBe discounted
        withClue("an undeclared cast") {
            calc.calculateEffectiveCost(d.state, spell, d.player1) shouldBe spell.manaCost
        }
        withClue("a bargained spell is not a kicked one") {
            calc.calculateEffectiveCost(d.state, spell, d.player1, declaredCostSlot = ChoiceSlot.BARGAINED) shouldBe spell.manaCost
        }
        withClue("an opponent's kicked spell is out of scope") {
            calc.calculateEffectiveCost(d.state, spell, d.player2, declaredCostSlot = ChoiceSlot.KICKED) shouldBe spell.manaCost
        }
    }

    test("an earlier kicked spell this turn uses up the first; other casts don't") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, firstKickedDiscount.name)
        val calc = d.calculator()

        d.withHistory(null, ChoiceSlot.BARGAINED)
        withClue("unkicked and bargained casts don't count") {
            calc.calculateEffectiveCost(d.state, spell, d.player1, declaredCostSlot = ChoiceSlot.KICKED) shouldBe discounted
        }

        d.withHistory(null, ChoiceSlot.KICKED)
        withClue("a kicked spell already cast this turn was the first") {
            calc.calculateEffectiveCost(d.state, spell, d.player1, declaredCostSlot = ChoiceSlot.KICKED) shouldBe spell.manaCost
        }
    }

    test("a kicked cast is recorded as kicked, even before the modifier's source arrives") {
        val d = driver()
        val kicked = d.putCardInHand(d.player1, kickerSorcery.name)
        d.giveMana(d.player1, Color.GREEN, 2)
        d.submitSuccess(CastSpell(d.player1, kicked, declaredCostSlot = ChoiceSlot.KICKED))
        d.state.spellsCastThisTurnByPlayer[d.player1]!!.single().declaredCostSlot shouldBe ChoiceSlot.KICKED

        d.putPermanentOnBattlefield(d.player1, firstKickedDiscount.name)
        d.calculator().calculateEffectiveCost(d.state, spell, d.player1, declaredCostSlot = ChoiceSlot.KICKED) shouldBe
            spell.manaCost
    }

    test("the description names the declared cost") {
        firstKickedDiscount.staticAbilities.filterIsInstance<ModifySpellCost>().single().description shouldBe
            "The first kicked spell you cast costs {1} less to cast each turn"
    }
})
