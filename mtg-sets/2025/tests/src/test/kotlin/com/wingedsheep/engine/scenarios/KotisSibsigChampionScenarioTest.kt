package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.mtg.sets.definitions.tdc.cards.KotisSibsigChampion
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Kotis, Sibsig Champion (TDC #5) {B}{G}{U} — Legendary Creature — Zombie Warrior 3/3.
 * "Once during each of your turns, you may cast a creature spell from your graveyard by exiling
 *  three other cards from your graveyard in addition to paying its other costs.
 *  Whenever one or more creatures you control enter, if one or more of them entered from a
 *  graveyard or was cast from a graveyard, put two +1/+1 counters on Kotis."
 *
 * Covers both halves of `Conditions.AnyEnteredOrWasCastFromGraveyard` (cast from a graveyard,
 * put onto the battlefield from one), the false case (a creature cast from hand), and the
 * once-per-turn graveyard cast with its exile-three-others cost.
 */
class KotisSibsigChampionScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KotisSibsigChampion, Zombify))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val kotis = driver.putCreatureOnBattlefield(me, "Kotis, Sibsig Champion")
        return Triple(driver, me, kotis)
    }

    fun plusCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun drainStack(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    fun castFromGraveyard(driver: GameTestDriver, me: EntityId, card: EntityId, exiled: List<EntityId>) =
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = card,
                additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )

    fun canCast(driver: GameTestDriver, me: EntityId, card: EntityId): Boolean =
        LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, me, EnumerationMode.FULL)
            .any { it.actionType == "CastSpell" && (it.action as? CastSpell)?.cardId == card }

    test("casting a creature from the graveyard exiles three other cards and puts two counters on Kotis") {
        val (driver, me, kotis) = setup()
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val fodder = (1..3).map { driver.putCardInGraveyard(me, "Forest") }

        canCast(driver, me, bears) shouldBe true
        castFromGraveyard(driver, me, bears, fodder).outcome shouldBe Outcome.Done
        drainStack(driver)

        driver.state.getBattlefield() shouldContain bears
        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContainAll fodder
        plusCounters(driver, kotis) shouldBe 2
    }

    test("a creature put onto the battlefield from a graveyard also counts") {
        val (driver, me, kotis) = setup()
        repeat(4) { driver.putLandOnBattlefield(me, "Swamp") }
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val zombify = driver.putCardInHand(me, "Zombify")

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = zombify,
                targets = listOf(ChosenTarget.Card(bears, me, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).outcome shouldBe Outcome.Done
        drainStack(driver)

        driver.state.getBattlefield() shouldContain bears
        plusCounters(driver, kotis) shouldBe 2
    }

    test("a creature cast from hand doesn't trigger Kotis") {
        val (driver, me, kotis) = setup()
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }
        val bears = driver.putCardInHand(me, "Grizzly Bears")

        driver.submit(
            CastSpell(playerId = me, cardId = bears, paymentStrategy = PaymentStrategy.AutoPay)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getBattlefield() shouldContain bears
        driver.state.stack.isEmpty() shouldBe true
        plusCounters(driver, kotis) shouldBe 0
    }

    test("the graveyard cast needs three other cards and works only once each turn") {
        val (driver, me, _) = setup()
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }
        val first = driver.putCardInGraveyard(me, "Grizzly Bears")
        val fodder = (1..2).map { driver.putCardInGraveyard(me, "Forest") }

        // Two other cards can't pay "exile three other cards".
        canCast(driver, me, first) shouldBe false
        val third = driver.putCardInGraveyard(me, "Forest")
        canCast(driver, me, first) shouldBe true
        castFromGraveyard(driver, me, first, fodder + third).outcome shouldBe Outcome.Done
        drainStack(driver)

        (1..3).forEach { _ -> driver.putCardInGraveyard(me, "Forest") }
        val second = driver.putCardInGraveyard(me, "Grizzly Bears")
        canCast(driver, me, second) shouldBe false
    }
})
