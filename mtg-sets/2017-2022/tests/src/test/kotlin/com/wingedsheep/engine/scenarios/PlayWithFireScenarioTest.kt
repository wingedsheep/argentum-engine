package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.PlayWithFire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Play with Fire — "Play with Fire deals 2 damage to any target. If a player is dealt damage this
 * way, scry 1."
 *
 * The scry is gated on the spell's own record of which players it actually damaged, so it happens
 * for a player target and not for a creature target.
 */
class PlayWithFireScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(PlayWithFire))
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("targeting a player deals 2 damage and its caster scries 1") {
        val d = driver()
        val lifeBefore = d.getLifeTotal(d.player2)
        val spell = d.putCardInHand(d.player1, "Play with Fire")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, spell, listOf(d.player2)).error shouldBe null
        d.bothPass().error shouldBe null

        d.getLifeTotal(d.player2) shouldBe lifeBefore - 2
        val scry = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        scry.playerId shouldBe d.player1
        scry.options.size shouldBe 1
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        d.pendingDecision shouldBe null
    }

    test("targeting a creature deals 2 damage and there is no scry") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, "Play with Fire")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, spell, listOf(bear)).error shouldBe null
        d.bothPass().error shouldBe null

        d.getGraveyardCardNames(d.player2).contains("Grizzly Bears") shouldBe true
        d.pendingDecision shouldBe null
    }
})
