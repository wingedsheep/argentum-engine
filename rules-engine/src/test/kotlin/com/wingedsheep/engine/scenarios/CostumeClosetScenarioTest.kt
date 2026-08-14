package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.CostumeCloset
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Costume Closet (SPM) — "Whenever a modified creature you control leaves the battlefield, put a
 * +1/+1 counter on this artifact."
 *
 * Pins the last-known-information fix for the modified-LTB trigger: when a modified creature leaves,
 * its counters and attachments are already stripped, so the `StatePredicate.IsModified` filter must
 * be evaluated against the departing permanent's [EntitySnapshot] (counters +
 * `wasEquipped`/`wasEnchanted`). Before the fix the predicate fell through fail-open, so the Closet
 * would have gained a counter for *any* creature-you-control departure — the "unmodified creature"
 * case is the regression guard.
 */
class CostumeClosetScenarioTest : FunSpec({

    // A trivial Equipment so we can exercise the "modified because equipped" leg.
    val testEquipment = CardDefinition.equipment(
        name = "Test Blade",
        manaCost = ManaCost.parse("{1}"),
        equipCost = ManaCost.parse("{1}"),
    )

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CostumeCloset, testEquipment))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun closetCounters(driver: GameTestDriver, closet: EntityId): Int =
        driver.state.getEntity(closet)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun destroy(driver: GameTestDriver, you: EntityId, victim: EntityId) {
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        val doomBlade = driver.putCardInHand(you, "Doom Blade")
        driver.castSpellWithTargets(you, doomBlade, listOf(ChosenTarget.Permanent(victim)))
        resolveStack(driver)
    }

    test("a modified (counter) creature leaving adds a +1/+1 counter to the Closet") {
        val (driver, you, _) = newGame()
        val closet = driver.putPermanentOnBattlefield(you, "Costume Closet")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.addComponent(lions, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))

        closetCounters(driver, closet) shouldBe 0
        destroy(driver, you, lions)
        closetCounters(driver, closet) shouldBe 1
    }

    test("a modified (equipped) creature leaving adds a +1/+1 counter to the Closet") {
        val (driver, you, _) = newGame()
        val closet = driver.putPermanentOnBattlefield(you, "Costume Closet")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val blade = driver.putPermanentOnBattlefield(you, "Test Blade")
        driver.addComponent(blade, AttachedToComponent(lions))
        driver.addComponent(lions, AttachmentsComponent(listOf(blade)))

        closetCounters(driver, closet) shouldBe 0
        destroy(driver, you, lions)
        closetCounters(driver, closet) shouldBe 1
    }

    test("an unmodified creature leaving does NOT add a counter (regression guard)") {
        val (driver, you, _) = newGame()
        val closet = driver.putPermanentOnBattlefield(you, "Costume Closet")
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")

        closetCounters(driver, closet) shouldBe 0
        destroy(driver, you, lions)
        closetCounters(driver, closet) shouldBe 0
    }
})
