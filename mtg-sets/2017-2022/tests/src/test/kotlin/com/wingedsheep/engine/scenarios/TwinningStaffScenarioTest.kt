package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c20.cards.TwinningStaff
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Twinning Staff (C20 #70) — its own "{7}, {T}: Copy target instant or sorcery spell you control"
 * is a copy instruction its replacement resizes: one activation makes two copies.
 */
class TwinningStaffScenarioTest : FunSpec({

    val lifeSorcery = card("Test Staff Life Sorcery") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + TwinningStaff + lifeSorcery)
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("{7}, {T} copies your spell twice with the Staff's own replacement") {
        val d = setup()
        val staff = d.putPermanentOnBattlefield(d.player1, "Twinning Staff")
        val before = d.getLifeTotal(d.player1)
        val spell = d.putCardInHand(d.player1, lifeSorcery.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell).error shouldBe null

        d.giveMana(d.player1, Color.BLUE, 7)
        d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = staff,
                abilityId = TwinningStaff.activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Spell(spell)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        while (d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()

        d.getLifeTotal(d.player1) - before shouldBe 3
    }
})
