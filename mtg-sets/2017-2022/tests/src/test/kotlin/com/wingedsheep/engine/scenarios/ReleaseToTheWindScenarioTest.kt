package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rix.cards.ReleaseToTheWind
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Release to the Wind {2}{U} — Instant.
 * "Exile target nonland permanent. For as long as that card remains exiled, its owner may cast it
 *  without paying its mana cost."
 *
 * Pins `GrantPlayWithoutPayingCost(ownerControls = true)`: the free cast belongs to the exiled
 * card's owner, not the caster, and lasts past the turn.
 */
class ReleaseToTheWindScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReleaseToTheWind))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        return driver
    }

    fun mayCast(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean =
        LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.FULL)
            .any { it.actionType == "CastSpell" && (it.action as? CastSpell)?.cardId == cardId }

    fun castRelease(driver: GameTestDriver, me: EntityId, target: EntityId) {
        val spell = driver.putCardInHand(me, "Release to the Wind")
        repeat(3) { driver.putLandOnBattlefield(me, "Island") }
        driver.submit(
            CastSpell(me, spell, listOf(ChosenTarget.Permanent(target)), paymentStrategy = PaymentStrategy.AutoPay)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("flickering your own creature: you recast it from exile with no mana") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castRelease(driver, me, bears)
        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain bears

        // Every Island is tapped for Release itself; the recast costs nothing.
        mayCast(driver, me, bears) shouldBe true
        driver.submit(CastSpell(me, bears, paymentStrategy = PaymentStrategy.AutoPay)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldContain bears
    }

    test("an opponent's permanent: only its owner may cast it free, and the permission outlasts the turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val oppBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        castRelease(driver, me, oppBears)
        driver.state.getZone(ZoneKey(opp, Zone.EXILE)) shouldContain oppBears
        mayCast(driver, me, oppBears) shouldBe false

        // On the owner's next turn, with no lands of their own, they recast it for free.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        mayCast(driver, opp, oppBears) shouldBe true
        driver.submit(CastSpell(opp, oppBears, paymentStrategy = PaymentStrategy.AutoPay)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)) shouldContain oppBears
    }
})
