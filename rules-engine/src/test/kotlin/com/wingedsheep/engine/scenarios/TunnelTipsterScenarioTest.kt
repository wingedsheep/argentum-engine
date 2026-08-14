package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.GadgetTechnician
import com.wingedsheep.mtg.sets.definitions.mkm.cards.TunnelTipster
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tunnel Tipster (MKM #180) — {1}{G} 1/1 Mole Scout.
 *
 * "At the beginning of your end step, **if a face-down creature entered the battlefield under your
 *  control this turn**, put a +1/+1 counter on this creature.
 *  {T}: Add {G}."
 *
 * The intervening-if (CR 603.4) is the whole card, so the tests pin both arms of it: an end step
 * with no face-down entry must not put a counter, and one preceded by a face-down cast must. A
 * Gadget Technician cast face down for {3} is the fixture that flips the tracker.
 *
 * The third test is the load-bearing negative from the printed ruling: the condition is a
 * *historical* fact about the turn, so it must not be re-read as "a face-down creature is on the
 * battlefield now" — flipping the face-down creature face up before the end step still grows the
 * Tipster.
 */
class TunnelTipsterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(TunnelTipster, GadgetTechnician))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun castFaceDown(driver: GameTestDriver, player: EntityId) {
        val card = driver.putCardInHand(player, "Gadget Technician")
        driver.giveColorlessMana(player, 3)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()
    }

    /** Walk to the end step and let the (possible) trigger resolve. */
    fun runEndStep(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END)
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
    }

    test("no face-down creature entered — the trigger never goes on the stack") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tipster = driver.putCreatureOnBattlefield(player, "Tunnel Tipster")

        runEndStep(driver)

        withClue("the intervening-if failed, so no counter") {
            plusOneCounters(driver, tipster) shouldBe 0
        }
    }

    test("a face-down creature entered this turn — the end step grows the Tipster") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tipster = driver.putCreatureOnBattlefield(player, "Tunnel Tipster")

        castFaceDown(driver, player)
        runEndStep(driver)

        withClue("the face-down entry satisfied the intervening-if") {
            plusOneCounters(driver, tipster) shouldBe 1
        }
    }

    test("the condition is historical — the tracker survives into the same turn's end step") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tipster = driver.putCreatureOnBattlefield(player, "Tunnel Tipster")

        castFaceDown(driver, player)
        runEndStep(driver)
        plusOneCounters(driver, tipster) shouldBe 1

        // Round the table back to this player without casting anything face down.
        do {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            if (driver.activePlayer != player) driver.passPriorityUntil(Step.END)
        } while (driver.activePlayer != player)

        runEndStep(driver)

        withClue("the tracker is cleared each turn, so a clean turn adds nothing") {
            plusOneCounters(driver, tipster) shouldBe 1
        }
    }

    test("{T}: Add {G} is a mana ability that fills the pool") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tipster = driver.putCreatureOnBattlefield(player, "Tunnel Tipster")
        driver.removeSummoningSickness(tipster)

        val abilityId = TunnelTipster.script.activatedAbilities.single().id
        driver.submit(
            ActivateAbility(playerId = player, sourceId = tipster, abilityId = abilityId)
        ).error shouldBe null

        withClue("a mana ability doesn't use the stack — the green is in the pool immediately") {
            driver.stackSize shouldBe 0
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.green shouldBe 1
        }
        driver.isTapped(tipster) shouldBe true
    }
})
