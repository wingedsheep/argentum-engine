package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `DynamicAmounts.wouldBeDamage()` inside a static [PreventDamage]'s `onPrevented` rider reads the
 * damage that reached the prevention, before any was prevented — unlike `preventedDamage()`.
 *
 * - Partial prevention ("prevent 1"): the rider counts all 3 that would be dealt, `preventedDamage()` 1.
 * - Unpreventable damage (CR 615.12 — prevention effects still apply, preventing nothing): the rider
 *   runs and counts the full amount; `preventedDamage()` reads 0.
 * - Full prevention: both amounts agree.
 */
class WouldBeDamagePreventionRiderTest : FunSpec({
    fun ward(name: String, prevent: Int?) = card(name) {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(PreventDamage(
            amount = prevent?.let { DynamicAmounts.fixed(it) },
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
            // Gain life equal to the damage that would be dealt; draw cards equal to what was prevented.
            onPrevented = Effects.Composite(listOf(
                Effects.GainLife(DynamicAmounts.wouldBeDamage()),
                Effects.DrawCards(DynamicAmounts.preventedDamage())
            ))
        ))
    }
    val partialWard = ward("Would-Be Partial Ward", prevent = 1)
    val fullWard = ward("Would-Be Full Ward", prevent = null)
    fun burn(name: String, cantBePrevented: Boolean) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent), cantBePrevented = cantBePrevented) }
    }
    val burn = burn("Would-Be Burn", cantBePrevented = false)
    val unpreventableBurn = burn("Would-Be Unpreventable Burn", cantBePrevented = true)

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(partialWard, fullWard, burn, unpreventableBurn))
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.run(ward: String, spell: String): Pair<Int, Int> {
        putPermanentOnBattlefield(player2, ward)
        val hand = getHand(player2).size
        castSpell(player1, putCardInHand(player1, spell)).error shouldBe null
        bothPass().error shouldBe null
        return getLifeTotal(player2) to (getHand(player2).size - hand)
    }

    test("partial prevention: the rider's would-be amount is the whole damage, not the prevented part") {
        // 3 would be dealt, 1 prevented: lose 2, gain 3, draw 1.
        driver().run("Would-Be Partial Ward", "Would-Be Burn") shouldBe (20 - 2 + 3 to 1)
    }

    test("unpreventable damage: the rider still runs and counts the full damage, having prevented none") {
        // 3 dealt in full, 0 prevented: lose 3, gain 3, draw 0.
        driver().run("Would-Be Full Ward", "Would-Be Unpreventable Burn") shouldBe (20 - 3 + 3 to 0)
    }

    test("full prevention: the would-be and prevented amounts agree") {
        driver().run("Would-Be Full Ward", "Would-Be Burn") shouldBe (20 + 3 to 3)
    }
})
