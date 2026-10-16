package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * CR 202.3e — "When calculating the mana value of an object with an {X} in its mana cost, X is
 * treated as 0 while the object is not on the stack, and X is treated as the number chosen for it
 * while the object is on the stack."
 *
 *  - [ManaValues.of] counts the announced X once per `{X}` on the stack, and 0 once the card leaves.
 *  - Every reader routes through it: a numeric "its mana value" read, a mana-value target filter,
 *    the [SpellCastEvent] that "a spell with mana value N" triggers read, and the cast history.
 */
class SpellManaValueIncludesXTest : FunSpec({

    val xSpell = card("Test X Spell") {
        manaCost = "{X}{R}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    val doubleXSpell = card("Test Double X Spell") {
        manaCost = "{X}{X}{R}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    /** "Target spell's controller... you gain life equal to its mana value." — the numeric read. */
    val probe = card("Test Mana Value Probe") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Choose target spell. You gain life equal to its mana value."
        spell {
            val spell = target(TargetFilter.SpellOnStack)
            effect = Effects.GainLife(DynamicAmounts.manaValueOf(spell))
        }
    }

    /** "Choose target spell with mana value 4 or greater." — the predicate read. */
    val bigSpellProbe = card("Test Big Spell Probe") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Choose target spell with mana value 4 or greater. You gain 1 life."
        spell {
            target(TargetFilter.SpellOnStack.manaValueAtLeast(4))
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(xSpell, doubleXSpell, probe, bigSpellProbe))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Player 1 casts [name] with [x]; player 2 then holds priority with the spell on the stack. */
    fun GameTestDriver.castX(name: String, x: Int, redPips: Int = 1): com.wingedsheep.sdk.model.EntityId {
        val spell = putCardInHand(player1, name)
        giveMana(player1, Color.RED, redPips)
        giveColorlessMana(player1, x * if (name == doubleXSpell.name) 2 else 1)
        val result = castXSpell(player1, spell, x)
        result.error shouldBe null
        result.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe
            (if (name == doubleXSpell.name) 2 * x else x) + 1
        passPriority(player1)
        return spell
    }

    test("an X spell on the stack has mana value X plus its other symbols; off the stack X is 0") {
        val d = driver()
        val spell = d.castX(xSpell.name, 3)
        ManaValues.of(d.state, spell) shouldBe 4

        d.bothPass() // resolve: the card is in the graveyard now
        ManaValues.of(d.state, spell) shouldBe 1
    }

    test("each {X} counts the chosen X — {X}{X}{R} cast for X=2 is mana value 5") {
        val d = driver()
        val spell = d.castX(doubleXSpell.name, 2)
        ManaValues.of(d.state, spell) shouldBe 5
    }

    test("X=0 leaves the printed mana value") {
        val d = driver()
        val spell = d.castX(xSpell.name, 0)
        ManaValues.of(d.state, spell) shouldBe 1
    }

    test("'its mana value' read by a resolving spell counts the target's X") {
        val d = driver()
        val spell = d.castX(xSpell.name, 3)
        val probeCard = d.putCardInHand(d.player2, probe.name)
        d.giveMana(d.player2, Color.BLUE, 1)
        val before = d.getLifeTotal(d.player2)
        d.castSpellWithTargets(d.player2, probeCard, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe before + 4
    }

    test("a mana-value target filter sees the X spell as mana value 4") {
        val d = driver()
        val big = d.castX(xSpell.name, 3)
        val probeCard = d.putCardInHand(d.player2, bigSpellProbe.name)
        d.giveMana(d.player2, Color.BLUE, 1)
        d.castSpellWithTargets(d.player2, probeCard, listOf(ChosenTarget.Spell(big))).error shouldBe null
    }

    test("the same filter rejects the X spell cast for X=2 (mana value 3)") {
        val d = driver()
        val small = d.castX(xSpell.name, 2)
        val probeCard = d.putCardInHand(d.player2, bigSpellProbe.name)
        d.giveMana(d.player2, Color.BLUE, 1)
        d.castSpellWithTargets(d.player2, probeCard, listOf(ChosenTarget.Spell(small))).error shouldNotBe null
    }

    test("the cast history records the spell's mana value with X") {
        val d = driver()
        d.castX(xSpell.name, 4)
        d.state.spellsCastThisTurnByPlayer[d.player1].orEmpty().single().manaValue shouldBe 5
    }
})
