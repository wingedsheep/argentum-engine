package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.OathOfTheGatewatchWastes183
import com.wingedsheep.mtg.sets.definitions.ogw.cards.VileRedeemer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vile Redeemer — "When you cast this spell, you may pay {C}. If you do, create a 1/1 colorless
 * Eldrazi Scion creature token for each nontoken creature that died under your control this turn."
 *
 * Only nontoken creatures that died under *your* control count: a token of yours dying and an
 * opponent's creature dying are both ignored.
 */
class VileRedeemerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VileRedeemer, OathOfTheGatewatchWastes183))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Bolt [victim] and let the bolt resolve. */
    fun GameTestDriver.bolt(me: EntityId, victim: EntityId) {
        val bolt = putCardInHand(me, "Lightning Bolt")
        giveMana(me, Color.RED, 1)
        castSpell(me, bolt, listOf(victim))
        bothPass()
    }

    /**
     * Kills two nontoken creatures of mine, one token of mine, and one opponent's creature, then
     * casts Vile Redeemer from a floating {G}{G}{G} and resolves its cast trigger (Wastes pays {C}).
     */
    fun GameTestDriver.setUpDeathsAndCastRedeemer(me: EntityId, opponent: EntityId) {
        putLandOnBattlefield(me, "Wastes")
        val mine = List(2) { putCreatureOnBattlefield(me, "Grizzly Bears") }
        val myToken = putCreatureOnBattlefield(me, "Grizzly Bears").also { addComponent(it, TokenComponent) }
        val theirs = putCreatureOnBattlefield(opponent, "Grizzly Bears")
        (mine + myToken + theirs).forEach { bolt(me, it) }
        getCreatures(me).size shouldBe 0
        getCreatures(opponent).size shouldBe 0

        val redeemer = putCardInHand(me, "Vile Redeemer")
        giveMana(me, Color.GREEN, 3)
        submitSuccess(CastSpell(me, redeemer, paymentStrategy = PaymentStrategy.FromPool))
        bothPass() // resolve the cast trigger; Vile Redeemer is still on the stack beneath it
    }

    fun GameTestDriver.scionsOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)!!.has<TokenComponent>() }

    test("paying {C} creates a Scion for each nontoken creature that died under your control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.setUpDeathsAndCastRedeemer(me, opponent)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        (driver.pendingDecision as? SelectManaSourcesDecision)?.let { driver.submitManaAutoPayOrDecline(me, autoPay = true) }

        driver.scionsOf(me).size shouldBe 2
        driver.scionsOf(opponent).size shouldBe 0
    }

    test("declining the payment creates no Scions") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.setUpDeathsAndCastRedeemer(me, opponent)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.scionsOf(me).size shouldBe 0
    }
})
