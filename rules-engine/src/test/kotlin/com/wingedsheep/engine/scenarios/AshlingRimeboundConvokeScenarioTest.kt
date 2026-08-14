package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Real-card version of [ConvokeWithConditionalManaTest]: Ashling, Rimebound's conditional mana
 * ("add two mana of any one color. Spend this mana only to cast spells with mana value 4 or
 * greater") paying for a convoked spell, optionally alongside another mana creature.
 *
 * Cards: Ashling, Rekindled // Ashling, Rimebound (ECL), Collective Inferno ({3}{R}{R}, convoke,
 * MV 5), Great Forest Druid ("{T}: Add one mana of any color").
 */
class AshlingRimeboundConvokeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), skipMulligans = true)
        return driver
    }

    /**
     * Put Ashling, Rimebound (the back face) onto the battlefield and let its first-main-phase
     * trigger add two RED mana restricted to MV4+ spells. Returns Ashling's entity id.
     *
     * The two first-main triggers (add mana; may pay {R} to transform) both go on the stack, so
     * the pending decisions are drained until the pool holds the restricted mana.
     */
    fun GameTestDriver.giveAshlingMana(player: EntityId): EntityId {
        val ashling = putCreatureOnBattlefield(player, "Ashling, Rimebound")
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        drainDecisionsChoosingRed(player)
        return ashling
    }

    test("Ashling, Rimebound's first-main trigger floats two MV4+-restricted mana") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.giveAshlingMana(player)

        val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.all { it.color == Color.RED } shouldBe true
        pool.restrictedMana.all { it.restriction == ManaRestriction.SpellsWithManaValueAtLeast(4) } shouldBe true
    }

    test("Collective Inferno is castable with convoke + Ashling's restricted mana + a Druid") {
        // {3}{R}{R} (MV 5): convoke two creatures for {R}{R}, Ashling's two restricted red pay
        // {2} of the generic, and Great Forest Druid taps for the last {1}. No lands untapped.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.giveAshlingMana(player)

        val druid = driver.putCreatureOnBattlefield(player, "Great Forest Druid")
        driver.removeSummoningSickness(druid)
        val soldier1 = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val soldier2 = driver.putCreatureOnBattlefield(player, "Savannah Lions")

        val spellId = driver.putCardInHand(player, "Collective Inferno")

        // What the client sees: the cast is offered, flagged as a convoke cast, and the payload
        // carries Ashling's two restricted mana as eligible for it.
        val info = driver.legalActionInfoFor(player, spellId)
        (info != null) shouldBe true
        info!!.hasConvoke shouldBe true
        info.eligibleRestrictedMana?.size shouldBe 2

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = spellId,
                paymentStrategy = PaymentStrategy.AutoPay,
                alternativePayment = AlternativePaymentChoice(
                    convokedCreatures = mapOf(
                        soldier1 to ConvokePayment(color = null),
                        soldier2 to ConvokePayment(color = null)
                    )
                )
            )
        )
        result.isSuccess shouldBe true

        // Both restricted mana were spent; the Druid was tapped for the remainder.
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("Ashling's restricted mana is not advertised for a cheap spell") {
        // Lightning Bolt is MV 1, so Ashling's MV4+ mana can never pay for it. The Bolt is still
        // castable off the Druid's mana — the payload must offer the cast with *no* eligible
        // restricted mana, so the client's cost math doesn't credit mana the server would refuse.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.giveAshlingMana(player)
        val druid = driver.putCreatureOnBattlefield(player, "Great Forest Druid")
        driver.removeSummoningSickness(druid)

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val boltInfo = driver.legalActionInfoFor(player, bolt)

        (boltInfo != null) shouldBe true
        (boltInfo!!.eligibleRestrictedMana ?: emptyList()).size shouldBe 0
    }
})

/**
 * Drain the stack (Ashling's two first-main triggers), answering "choose a color" with RED and
 * declining the optional {R} transform payment, until the stack is empty.
 */
private fun GameTestDriver.drainDecisionsChoosingRed(player: EntityId) {
    var guard = 0
    while (guard++ < 40 && (stackSize > 0 || pendingDecision != null)) {
        val decision = pendingDecision
        when {
            decision is ChooseColorDecision ->
                submitDecision(player, ColorChosenResponse(decision.id, Color.RED))
            decision != null -> declinePendingDecision(player)
            else -> passPriority(priorityPlayer ?: player)
        }
    }
}

/** The client-facing legal-action payload for casting [cardId], or null when not offered. */
private fun GameTestDriver.legalActionInfoFor(playerId: EntityId, cardId: EntityId) =
    LegalActionEnricher(ManaSolver(cardRegistry), cardRegistry)
        .enrich(
            LegalActionEnumerator.create(cardRegistry).enumerate(state, playerId, EnumerationMode.FULL),
            state,
            playerId
        )
        .firstOrNull { (it.action as? CastSpell)?.cardId == cardId }

/** Decline a yes/no decision (Ashling's optional {R} transform payment). */
private fun GameTestDriver.declinePendingDecision(playerId: EntityId) {
    val decision = pendingDecision ?: return
    submitDecision(playerId, YesNoResponse(decision.id, false))
}
