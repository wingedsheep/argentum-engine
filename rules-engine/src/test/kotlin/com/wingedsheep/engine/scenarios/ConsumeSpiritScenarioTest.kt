package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.ConsumeSpirit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Consume Spirit (MRD #60) — "Spend only black mana on X. Consume Spirit deals X damage to any
 * target and you gain X life."
 *
 * The Soul Burn plumbing at a single-colour restriction. Two things are worth pinning: the life
 * gain is the *chosen X* (Soul Burn's is the black mana spent on X, a different `DynamicAmount`,
 * and swapping them here would look identical in the common case), and X really is black-only —
 * generic mana can pay the `{1}` but never the `{X}`.
 */
class ConsumeSpiritScenarioTest : FunSpec({

    fun driver(deck: Deck = Deck.of("Swamp" to 40)): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ConsumeSpirit)
        d.initMirrorMatch(deck = deck, skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("deals X to the target and gains X life") {
        val d = driver()
        val me = d.player1
        val opp = d.player2

        val spell = d.putCardInHand(me, "Consume Spirit")
        // {X}{1}{B} at X=3 → 5 black mana total.
        d.giveMana(me, Color.BLACK, 5)
        d.castXSpell(me, spell, xValue = 3, targets = listOf(opp))
        d.bothPass()

        d.getLifeTotal(opp) shouldBe 17
        withClue("life gained is X, per the ruling — not the damage that actually landed") {
            d.getLifeTotal(me) shouldBe 23
        }
    }

    test("X=0 is legal and does nothing") {
        val d = driver()
        val me = d.player1
        val opp = d.player2

        val spell = d.putCardInHand(me, "Consume Spirit")
        d.giveMana(me, Color.BLACK, 2)
        d.castXSpell(me, spell, xValue = 0, targets = listOf(opp))
        d.bothPass()

        d.getLifeTotal(opp) shouldBe 20
        d.getLifeTotal(me) shouldBe 20
    }

    test("kills a creature and still gains the full X") {
        val d = driver()
        val me = d.player1
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") // 2/2

        val spell = d.putCardInHand(me, "Consume Spirit")
        d.giveMana(me, Color.BLACK, 6)
        d.castXSpell(me, spell, xValue = 4, targets = listOf(bear))
        d.bothPass()

        d.state.getBattlefield().contains(bear) shouldBe false
        withClue("4 damage to a 2/2 still gains 4, not 2") {
            d.getLifeTotal(me) shouldBe 24
        }
    }

    test("X can be paid only with black mana") {
        val d = driver(Deck.of("Swamp" to 20, "Forest" to 20))
        val me = d.player1
        d.putLandOnBattlefield(me, "Swamp")
        repeat(4) { d.putLandOnBattlefield(me, "Forest") }

        val registry = CardRegistry().apply { register(TestCards.all + ConsumeSpirit) }
        val solver = ManaSolver(registry)
        val cost = ManaCost.parse("{X}{1}{B}")
        val blackOnly = setOf(Color.BLACK)

        withClue("the lone Swamp is consumed by the mandatory {B}, leaving nothing black for X") {
            solver.canPay(d.state, me, cost, xValue = 1, xManaRestriction = blackOnly).shouldBeFalse()
        }
        withClue("without the restriction the green mana covers X=1 easily") {
            solver.canPay(d.state, me, cost, xValue = 1).shouldBeTrue()
        }

        // A second Swamp frees one black source for X.
        d.putLandOnBattlefield(me, "Swamp")
        solver.canPay(d.state, me, cost, xValue = 1, xManaRestriction = blackOnly).shouldBeTrue()
        solver.canPay(d.state, me, cost, xValue = 2, xManaRestriction = blackOnly).shouldBeFalse()
    }
})
