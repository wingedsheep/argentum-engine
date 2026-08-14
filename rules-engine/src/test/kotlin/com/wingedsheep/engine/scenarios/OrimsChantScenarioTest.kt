package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pls.cards.OrimsChant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Orim's Chant (PLS #11) — target player can't cast spells this turn; when kicked, no creatures
 * can attack that turn. The attack restriction is global, not limited to the targeted player.
 */
class OrimsChantScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(OrimsChant))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun castChant(
        d: GameTestDriver,
        caster: EntityId,
        target: EntityId,
        kicked: Boolean
    ) {
        repeat(if (kicked) 2 else 1) { d.putLandOnBattlefield(caster, "Plains") }
        val chant = d.putCardInHand(caster, "Orim's Chant")
        val result = d.submit(
            CastSpell(
                playerId = caster,
                cardId = chant,
                targets = listOf(ChosenTarget.Player(target)),
                declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        withClue("Orim's Chant should be castable: ${result.error}") {
            result.isSuccess shouldBe true
        }
        d.bothPass()
    }

    test("unkicked restricts only the targeted player's spell casting") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)

        castChant(d, caster, opponent, kicked = false)

        d.state.getEntity(opponent)?.has<CantCastSpellsComponent>() shouldBe true
        d.state.getEntity(caster)?.has<CantCastSpellsComponent>() shouldBe false
    }

    test("target player may be the caster") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)

        castChant(d, caster, caster, kicked = false)

        d.state.getEntity(caster)?.has<CantCastSpellsComponent>() shouldBe true
        d.state.getEntity(opponent)?.has<CantCastSpellsComponent>() shouldBe false
    }

    test("kicked prevents every creature from attacking, including the caster's") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        val casterCreature = d.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val opponentCreature = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castChant(d, caster, opponent, kicked = true)

        d.state.getEntity(opponent)?.has<CantCastSpellsComponent>() shouldBe true
        d.state.projectedState.cantAttack(casterCreature) shouldBe true
        d.state.projectedState.cantAttack(opponentCreature) shouldBe true
    }
})
