package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Divine Deflection (AVR #18) — "Prevent the next X damage that would be dealt to you and/or
 * permanents you control this turn. If damage is prevented this way, Divine Deflection deals that
 * much damage to any target."
 *
 * X sizes one pool shared by the caster and their permanents; what it prevents is dealt to the
 * target chosen on cast. The pool's group mechanics are pinned in `PreventNextDamageGroupPoolTest`.
 */
class DivineDeflectionScenarioTest : FunSpec({

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolve() {
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("X = 2 prevents 2 of a Bolt to your creature and deals 2 to the target") {
        val d = newGame()
        val p1 = d.player1
        val p2 = d.player2
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val enemy = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        d.giveMana(p1, Color.WHITE, 3)
        d.castXSpell(p1, d.putCardInHand(p1, "Divine Deflection"), 2, listOf(enemy)).error shouldBe null
        d.resolve()

        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, d.putCardInHand(p1, "Lightning Bolt"), listOf(bears)).error shouldBe null
        d.resolve()

        d.damageOn(bears) shouldBe 1
        // The reflected 2 kills the targeted 2/2.
        d.findPermanent(p2, "Grizzly Bears") shouldBe null
        d.getLifeTotal(p2) shouldBe 20
    }

    test("damage to you is prevented up to X, and a target that left takes nothing") {
        val d = newGame()
        val p1 = d.player1
        val p2 = d.player2
        val enemy = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        d.giveMana(p1, Color.WHITE, 3)
        d.castXSpell(p1, d.putCardInHand(p1, "Divine Deflection"), 2, listOf(enemy)).error shouldBe null
        d.resolve()

        // The target dies before any damage is prevented.
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, d.putCardInHand(p1, "Lightning Bolt"), listOf(enemy)).error shouldBe null
        d.resolve()

        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, d.putCardInHand(p1, "Lightning Bolt"), listOf(p1)).error shouldBe null
        d.resolve()

        // 2 of the Bolt prevented, 1 gets through.
        d.getLifeTotal(p1) shouldBe 19
        d.getLifeTotal(p2) shouldBe 20
    }
})
