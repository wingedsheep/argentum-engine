package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the placer of a Saga's lore counters (G146). A Saga's controller puts its
 * entry lore counter on it as it enters (CR 714.3a; CR 122.6a) and the precombat-main lore counter
 * (CR 714.3c), so "whenever you put a lore counter on a Saga you control"
 * (`getsCounters(LORE, by = Player.You)`) sees all three entry/accrual paths: a resolving Saga
 * spell (`PermanentEntry`), a Saga put onto the battlefield by an effect (`ZoneMovementUtils`), and
 * the turn-based action (`BeginningPhaseManager`).
 */
class SagaLoreCounterPlacerTest : FunSpec({

    val sagaYouControl = GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA).youControl()

    // "Whenever you put a lore counter on a Saga you control, you gain 1 life."
    val loreObserver = card("Lore Placer Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a(sagaYouControl).getsCounters(CounterType.LORE, by = Player.You)
            effect = Effects.GainLife(1)
        }
    }

    val quietSaga = card("Quiet Test Saga") {
        manaCost = "{1}"
        typeLine = "Enchantment — Saga"
        sagaChapter(1) { effect = Effects.DrawCards(1) }
        sagaChapter(2) { effect = Effects.DrawCards(1) }
        sagaChapter(3) { effect = Effects.DrawCards(1) }
    }

    val returnSaga = card("Return Test Saga") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val saga = target(TargetFilter(GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA).ownedByYou(), zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefieldFromGraveyard(saga)
        }
    }

    // An opponent's instant that puts a lore counter on any Saga — placer and controller differ.
    val loreBump = card("Lore Bump") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val saga = target(TargetFilter(GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA)))
            effect = Effects.AddCounters(CounterType.LORE, 1, saga)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(loreObserver, quietSaga, returnSaga, loreBump))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while (guard++ < 20 && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.castQuietSaga(controller: EntityId) {
        val spell = putCardInHand(controller, "Quiet Test Saga")
        giveColorlessMana(controller, 1)
        castSpell(controller, spell)
        drain()
    }

    test("a resolving Saga spell's entry lore counter is put by its controller") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Lore Placer Observer")
        val life = driver.getLifeTotal(me)

        driver.castQuietSaga(me)

        withClue("the observer saw the entry lore counter") { driver.getLifeTotal(me) shouldBe life + 1 }
    }

    test("the precombat-main lore counter is put by the Saga's controller") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Lore Placer Observer")
        driver.castQuietSaga(me)
        val life = driver.getLifeTotal(me)

        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        if (driver.activePlayer != me) {
            driver.passPriorityUntil(Step.END, maxPasses = 300)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        }
        driver.drain()

        withClue("the observer saw the turn-based lore counter") { driver.getLifeTotal(me) shouldBe life + 1 }
    }

    test("a Saga put onto the battlefield by an effect gets its entry lore counter from its controller") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Lore Placer Observer")
        val saga = driver.putCardInGraveyard(me, "Quiet Test Saga")
        val spell = driver.putCardInHand(me, "Return Test Saga")
        driver.giveColorlessMana(me, 1)
        val life = driver.getLifeTotal(me)

        driver.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(saga, me, Zone.GRAVEYARD)))
        driver.drain()

        withClue("the Saga is back") { (driver.findPermanent(me, "Quiet Test Saga") != null) shouldBe true }
        withClue("the observer saw the entry lore counter") { driver.getLifeTotal(me) shouldBe life + 1 }
    }

    test("a lore counter an opponent puts on your Saga doesn't trigger your observer") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != me }
        driver.putPermanentOnBattlefield(me, "Lore Placer Observer")
        driver.castQuietSaga(me)
        val saga = driver.findPermanent(me, "Quiet Test Saga")!!
        val life = driver.getLifeTotal(me)

        driver.passPriority(me)
        val bump = driver.putCardInHand(opponent, "Lore Bump")
        driver.giveColorlessMana(opponent, 1)
        driver.castSpellWithTargets(opponent, bump, listOf(ChosenTarget.Permanent(saga)))
        driver.drain()

        withClue("the Saga got the opponent's lore counter") {
            driver.state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 2
        }
        withClue("you didn't put it, so your observer stays quiet") { driver.getLifeTotal(me) shouldBe life }
    }
})
