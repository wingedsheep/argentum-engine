package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.AngelOfSuffering
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Angel of Suffering (SNC #67) — {3}{B}{B} 5/3 flier.
 * "If damage would be dealt to you, prevent that damage and mill twice that many cards."
 *
 * Covers noncombat and combat damage, a library too small to mill the full count (the damage is
 * still prevented), and unpreventable damage (dealt in full, and you still mill twice that many).
 */
class AngelOfSufferingScenarioTest : FunSpec({

    val shock = card("Test Shock") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Test Shock deals 3 damage to target player."
        spell {
            val t = target(Targets.Player)
            effect = Effects.DealDamage(3, t)
        }
    }

    val leyline = card("Test Leyline") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Damage can't be prevented."
        replacementEffect(DamageCantBePrevented())
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AngelOfSuffering, shock, leyline))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Grizzly Bears" to 20), skipMulligans = true)
        return d
    }

    fun shockPlayer(d: GameTestDriver, caster: EntityId, target: EntityId) {
        val spell = d.putCardInHand(caster, "Test Shock")
        d.castSpell(caster, spell, listOf(target)).outcome shouldBe Outcome.Done
        d.bothPass()
    }

    test("noncombat damage to you is prevented and you mill twice that many") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, "Angel of Suffering")
        val libBefore = d.state.getLibrary(p1).size
        val oppLibBefore = d.state.getLibrary(p2).size

        shockPlayer(d, p1, p1)

        d.assertLifeTotal(p1, 20)
        (libBefore - d.state.getLibrary(p1).size) shouldBe 6
        d.getGraveyard(p1).count { d.getCardName(it) != "Test Shock" } shouldBe 6
        d.state.getLibrary(p2).size shouldBe oppLibBefore
    }

    test("damage to an opponent is untouched") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, "Angel of Suffering")
        val oppLibBefore = d.state.getLibrary(p2).size

        shockPlayer(d, p1, p2)

        d.assertLifeTotal(p2, 17)
        d.state.getLibrary(p2).size shouldBe oppLibBefore
    }

    test("combat damage to you is prevented and you mill twice that many") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p2, "Angel of Suffering")
        val bears = d.putPermanentOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        val libBefore = d.state.getLibrary(p2).size

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bears), p2)
        d.bothPass()
        d.declareNoBlockers(p2)
        d.bothPass()
        d.bothPass()

        d.assertLifeTotal(p2, 20)
        (libBefore - d.state.getLibrary(p2).size) shouldBe 4
    }

    test("damage is prevented even when the library can't cover the mill") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, "Angel of Suffering")
        d.state.getLibrary(p1).drop(2).forEach { d.moveToGraveyard(it) }
        d.state.getLibrary(p1).size shouldBe 2

        shockPlayer(d, p1, p1)

        d.assertLifeTotal(p1, 20)
        d.state.getLibrary(p1).size shouldBe 0
    }

    test("damage that can't be prevented is dealt, and you still mill twice that many") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, "Angel of Suffering")
        d.putPermanentOnBattlefield(p1, "Test Leyline")
        val libBefore = d.state.getLibrary(p1).size

        shockPlayer(d, p1, p1)

        d.assertLifeTotal(p1, 17)
        (libBefore - d.state.getLibrary(p1).size) shouldBe 6
    }
})
