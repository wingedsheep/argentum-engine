package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.Unbury
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unbury — {1}{B} Instant
 * Choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Return two target creature cards that share a creature type from your graveyard to your hand.
 *
 * Both modes target as the spell is cast (CR 601.2c). The second mode's pair must share a creature
 * type — read off the cards' printed type lines, with a changeling card sharing every type
 * (CR 702.73a).
 */
class UnburyScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Unbury))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLACK, 1)
        return driver to me
    }

    fun GameTestDriver.castMode(me: EntityId, mode: Int, cards: List<EntityId>): ExecutionResult {
        val spell = putCardInHand(me, "Unbury")
        val targets = cards.map { ChosenTarget.Card(it, me, Zone.GRAVEYARD) }
        return submit(
            CastSpell(
                me, spell, targets,
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(targets),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("mode one returns the targeted creature card") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castMode(me, 0, listOf(bears)).error shouldBe null
        driver.bothPass()

        driver.getHand(me).contains(bears) shouldBe true
        driver.getGraveyard(me).contains(lions) shouldBe true
    }

    test("mode two returns two targeted creature cards that share a creature type") {
        val (driver, me) = setup()
        val bears1 = driver.putCardInGraveyard(me, "Grizzly Bears")
        val bears2 = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castMode(me, 1, listOf(bears1, bears2)).error shouldBe null
        driver.bothPass()

        driver.getHand(me).containsAll(listOf(bears1, bears2)) shouldBe true
        driver.getGraveyard(me).contains(lions) shouldBe true
    }

    test("mode two rejects a pair with no creature type in common") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")   // Bear
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")  // Cat

        withClue("a Bear and a Cat share no creature type") {
            driver.castMode(me, 1, listOf(bears, lions)).error shouldNotBe null
        }
    }

    test("a changeling card shares a creature type with anything") {
        val (driver, me) = setup()
        val shapesharer = driver.putCardInGraveyard(me, "Shapesharer")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castMode(me, 1, listOf(shapesharer, lions)).error shouldBe null
        driver.bothPass()

        driver.getHand(me).containsAll(listOf(shapesharer, lions)) shouldBe true
    }

    test("a creature card in an opponent's graveyard is not a legal target") {
        val (driver, me) = setup()
        val theirs = driver.putCardInGraveyard(driver.getOpponent(me), "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Unbury")
        val target = listOf(ChosenTarget.Card(theirs, driver.getOpponent(me), Zone.GRAVEYARD))
        driver.submit(
            CastSpell(me, spell, target, chosenModes = listOf(0), modeTargetsOrdered = listOf(target),
                paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldNotBe null
    }

    test("a lone target that leaves the graveyard in response makes mode one do nothing") {
        val (driver, me) = setup()
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castMode(me, 0, listOf(bears)).error shouldBe null
        driver.replaceState(driver.state.moveToZone(bears, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE)))
        driver.bothPass()

        driver.getExile(me).contains(bears) shouldBe true
        withClue("no other card is returned in its place") { driver.getGraveyard(me).contains(lions) shouldBe true }
    }

    test("mode two still returns one of the pair when the other leaves the graveyard in response") {
        val (driver, me) = setup()
        val bears1 = driver.putCardInGraveyard(me, "Grizzly Bears")
        val bears2 = driver.putCardInGraveyard(me, "Grizzly Bears")

        driver.castMode(me, 1, listOf(bears1, bears2)).error shouldBe null
        driver.replaceState(driver.state.moveToZone(bears1, ZoneKey(me, Zone.GRAVEYARD), ZoneKey(me, Zone.EXILE)))
        driver.bothPass()

        driver.getExile(me).contains(bears1) shouldBe true
        withClue("CR 608.2b: the still-legal target is returned") { driver.getHand(me).contains(bears2) shouldBe true }
    }
})
