package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.CelebrateTheMountainKing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Celebrate the Mountain-king (HOB #7) — {3}{W} Enchantment.
 *
 * "When this enchantment enters, for each opponent, exile up to one target nonland permanent that
 * player controls until this enchantment leaves the battlefield."
 * "When this enchantment enters, recruit."
 *
 * Two enters triggers fire together, and the recruit one pauses mid-resolution for its discard —
 * the case where a queued sibling trigger is easiest to lose. The test pins that both halves land,
 * and that destroying the enchantment afterwards gives the exiled permanent back (the Banishing
 * Light linked-exile pair).
 */
class CelebrateTheMountainKingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CelebrateTheMountainKing))
        return driver
    }

    fun GameTestDriver.drain(onDecision: (GameTestDriver) -> Unit) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 50) {
            if (state.pendingDecision != null) onDecision(this) else bothPass()
            guard++
        }
    }

    test("exiles an opponent's permanent and recruits; the exile comes back when it leaves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim: EntityId = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val lions = driver.putCardInHand(me, "Savannah Lions") // recruit's nonland discard

        driver.giveMana(me, Color.WHITE, 4)
        val enchantment = driver.putCardInHand(me, "Celebrate the Mountain-king")
        driver.castSpell(me, enchantment).error shouldBe null

        driver.drain { d ->
            when (val decision = d.state.pendingDecision) {
                is ChooseTargetsDecision -> d.submitTargetSelection(me, listOf(victim))
                is SelectCardsDecision ->
                    if (lions in decision.options) d.submitCardSelection(me, listOf(lions))
                    else d.autoResolveDecision()
                else -> d.autoResolveDecision()
            }
        }

        val permanent = driver.findPermanent(me, "Celebrate the Mountain-king")
        permanent shouldNotBe null

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.state.getZone(opponent, Zone.EXILE).contains(victim) shouldBe true

        driver.state.getZone(me, Zone.GRAVEYARD).contains(lions) shouldBe true
        driver.getPermanents(me).count { driver.getCardName(it) == "Human Soldier Token" } shouldBe 1

        // Destroy the enchantment — the leaves trigger returns the linked exile.
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 1)
        val disenchant = driver.putCardInHand(me, "Disenchant")
        driver.castSpellWithTargets(
            me,
            disenchant,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(permanent!!))
        ).error shouldBe null
        driver.drain { it.autoResolveDecision() }

        driver.findPermanent(me, "Celebrate the Mountain-king") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }
})
