package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Disenchant
import com.wingedsheep.mtg.sets.definitions.pip.cards.Overencumbered
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Overencumbered {1}{W} — Enchantment — Aura
 *   Enchant opponent
 *   When this Aura enters, enchanted opponent creates a Clue token, a Food token, and a Junk token.
 *   At the beginning of combat on enchanted opponent's turn, that player may pay {1} for each
 *   artifact they control. If they don't, creatures can't attack this combat.
 *
 * Pins what a reader can't confirm from the script: the tokens land under the *enchanted* player's
 * control, the combat tax is asked of and charged to that player (scaled by their artifacts), an
 * unpaid or unaffordable tax stops their attack, and the Aura's controller's own combats are free.
 */
class OverencumberedScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val caster: EntityId, val victim: EntityId)

    /** The active player casts Overencumbered on their opponent and lets the ETB trigger resolve. */
    fun table(disenchantInResponse: Boolean = false): Table {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all + Overencumbered + Disenchant +
                PredefinedTokens.Clue + PredefinedTokens.Food + PredefinedTokens.Junk
        )
        d.initMirrorMatch(deck = Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = d.activePlayer!!
        val victim = d.getOpponent(caster)

        val aura = d.putCardInHand(caster, "Overencumbered")
        d.giveMana(caster, Color.WHITE, 2)
        d.castSpell(caster, aura, targets = listOf(victim)).error shouldBe null
        if (disenchantInResponse) {
            d.bothPass() // the Aura resolves; its enters trigger goes on the stack
            d.stackSize shouldBe 1
            val disenchant = d.putCardInHand(caster, "Disenchant")
            d.giveMana(caster, Color.WHITE, 2)
            d.castSpell(caster, disenchant, targets = listOf(aura)).error shouldBe null
            d.bothPass()
            d.findPermanent(caster, "Overencumbered") shouldBe null
        }
        var guard = 0
        while (guard++ < 10 && d.stackSize > 0) d.bothPass()
        return Table(d, caster, victim)
    }

    fun Table.bears(owner: EntityId): EntityId =
        d.putCreatureOnBattlefield(owner, "Grizzly Bears").also { d.removeSummoningSickness(it) }

    fun Table.colorlessFloating(playerId: EntityId): Int =
        (d.state.getEntity(playerId)!!.get<ManaPoolComponent>() ?: ManaPoolComponent()).colorless

    /** Advance to the victim's beginning of combat and resolve the tax trigger up to its prompt. */
    fun Table.toVictimsCombatTax(floatingColorless: Int) {
        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = victim)
        d.stackSize shouldBe 1
        if (floatingColorless > 0) d.giveColorlessMana(victim, floatingColorless)
        d.bothPass()
    }

    test("enchanted opponent creates a Clue, a Food, and a Junk token") {
        val t = table()
        for (token in listOf("Clue", "Food", "Junk")) {
            withClue("$token under the enchanted opponent's control") {
                t.d.findPermanent(t.victim, token).shouldNotBeNull()
            }
            withClue("$token not under the Aura's controller") {
                t.d.findPermanent(t.caster, token) shouldBe null
            }
        }
    }

    test("destroyed in response, the Aura still hands its tokens to the opponent it enchanted") {
        val t = table(disenchantInResponse = true)
        for (token in listOf("Clue", "Food", "Junk")) {
            withClue("$token under the last-known enchanted opponent's control") {
                t.d.findPermanent(t.victim, token).shouldNotBeNull()
            }
            t.d.findPermanent(t.caster, token) shouldBe null
        }
    }

    test("the Aura's controller's own combat is untaxed") {
        val t = table()
        val bears = t.bears(t.caster)
        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.stackSize shouldBe 0
        t.d.pendingDecision shouldBe null
        t.d.declareAttackers(t.caster, mapOf(bears to t.victim)).error shouldBe null
    }

    test("the enchanted player pays {1} per artifact - they may attack") {
        val t = table()
        val bears = t.bears(t.victim)
        t.toVictimsCombatTax(floatingColorless = 3)

        val prompt = t.d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        prompt.playerId shouldBe t.victim
        t.d.submitYesNo(t.victim, true).error shouldBe null
        withClue("three artifacts (Clue, Food, Junk) cost {3}") { t.colorlessFloating(t.victim) shouldBe 0 }

        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.victim, mapOf(bears to t.caster)).error shouldBe null
    }

    test("the enchanted player declines - creatures can't attack this combat") {
        val t = table()
        val bears = t.bears(t.victim)
        t.toVictimsCombatTax(floatingColorless = 3)

        t.d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe t.victim
        t.d.submitYesNo(t.victim, false).error shouldBe null
        t.colorlessFloating(t.victim) shouldBe 3

        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.victim, mapOf(bears to t.caster)).error shouldNotBe null
    }

    test("an unaffordable tax isn't offered - creatures can't attack this combat") {
        val t = table()
        val bears = t.bears(t.victim)
        t.toVictimsCombatTax(floatingColorless = 2)

        t.d.pendingDecision shouldBe null
        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.victim, mapOf(bears to t.caster)).error shouldNotBe null
    }
})
