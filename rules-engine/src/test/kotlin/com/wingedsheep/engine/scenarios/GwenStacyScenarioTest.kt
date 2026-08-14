package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.GwenStacy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gwen Stacy // Ghost-Spider (SPM #78) — a transforming double-faced Legendary Creature.
 *
 * Front — Gwen Stacy · {1}{R} · 2/1:
 *   ETB: exile the top card of your library. You may play that card for as long as you control
 *        this creature.
 *   {2}{U}{R}{W}: Transform Gwen Stacy. Activate only as a sorcery.
 *
 * Back — Ghost-Spider · 4/4, Flying, vigilance, haste:
 *   Whenever you play a land from exile or cast a spell from exile, put a +1/+1 counter on it.
 *   Remove two counters from Ghost-Spider: Exile the top card of your library. You may play that
 *        card this turn.
 */
class GwenStacyScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GwenStacy)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 8 && stackSize > 0) bothPass()
    }

    // The front's only non-mana activated ability is the Transform ability.
    val transformId = GwenStacy.activatedAbilities.first { !it.isManaAbility }.id
    // The back's only non-mana activated ability is the remove-two-counters impulse.
    val removeTwoId = GwenStacy.backFace!!.activatedAbilities.first { !it.isManaAbility }.id

    // ─────────────────────────────────────────────────────────────────────────
    // Front — ETB impulse, playable for as long as you control Gwen
    // ─────────────────────────────────────────────────────────────────────────
    test("front ETB exiles the top card and it is playable while you control Gwen") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val top = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val gwen = driver.putCardInHand(me, "Gwen Stacy")
        driver.giveMana(me, Color.RED, 2)
        driver.castSpell(me, gwen).error shouldBe null
        driver.settle()

        withClue("the top card of the library was exiled") {
            (top in driver.getExile(me)) shouldBe true
        }
        withClue("a may-play permission was granted for the exiled card") {
            driver.state.mayPlayPermissions.any { top in it.cardIds } shouldBe true
        }
        withClue("while Gwen is on the battlefield the exiled card can be cast from exile") {
            driver.giveMana(me, Color.GREEN, 2)
            driver.castSpell(me, top).error shouldBe null
        }
    }

    test("the exiled card is no longer playable once you no longer control Gwen") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val top = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val gwen = driver.putCardInHand(me, "Gwen Stacy")
        driver.giveMana(me, Color.RED, 2)
        driver.castSpell(me, gwen).error shouldBe null
        driver.settle()
        (top in driver.getExile(me)) shouldBe true

        // "for as long as you control this creature" — Gwen leaves the battlefield. The real
        // zone transition strips a permanent's ControllerComponent; the blunt moveToGraveyard test
        // helper only moves zones, so strip the controller too to mirror an actual leave (which is
        // what YouControlSource reads).
        driver.moveToGraveyard(gwen)
        driver.replaceState(driver.state.updateEntity(gwen) { it.without<ControllerComponent>() })

        driver.giveMana(me, Color.GREEN, 2)
        withClue("the YouControlSource gate fails once Gwen leaves, so the card can't be cast") {
            driver.castSpell(me, top).error.shouldNotBeNull()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Front — transform
    // ─────────────────────────────────────────────────────────────────────────
    test("{2}{U}{R}{W} sorcery-speed ability transforms Gwen Stacy into Ghost-Spider") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val gwen = driver.putCardInHand(me, "Gwen Stacy")
        driver.giveMana(me, Color.RED, 2)
        driver.castSpell(me, gwen).error shouldBe null
        driver.settle()

        val gwenPerm = driver.findPermanent(me, "Gwen Stacy")!!
        driver.giveMana(me, Color.RED, 3)
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.submit(
            ActivateAbility(me, gwenPerm, transformId, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.settle()

        withClue("Gwen flipped to her back face") {
            driver.state.getEntity(gwenPerm)!!.get<CardComponent>()!!.name shouldBe "Ghost-Spider"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back — triggers + remove-two-counters impulse
    // ─────────────────────────────────────────────────────────────────────────
    test("remove-two-counters exiles the top card, and casting it from exile adds a +1/+1 counter") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val ghost = driver.putCreatureOnBattlefield(me, "Ghost-Spider")
        driver.removeSummoningSickness(ghost)
        driver.addComponent(ghost, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        val top = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        driver.submit(ActivateAbility(me, ghost, removeTwoId)).error shouldBe null
        driver.settle()

        withClue("the two-counter cost was paid") { driver.plusOne(ghost) shouldBe 0 }
        withClue("the top card was exiled by the impulse") { (top in driver.getExile(me)) shouldBe true }

        // Casting the exiled spell from exile triggers "cast a spell from exile → +1/+1 counter".
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, top).error shouldBe null
        driver.settle()

        withClue("casting a spell from exile put one +1/+1 counter on Ghost-Spider") {
            driver.plusOne(ghost) shouldBe 1
        }
    }

    test("playing a land from exile adds a +1/+1 counter to Ghost-Spider") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val ghost = driver.putCreatureOnBattlefield(me, "Ghost-Spider")
        driver.removeSummoningSickness(ghost)
        driver.addComponent(ghost, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        val top = driver.putCardOnTopOfLibrary(me, "Mountain")

        driver.submit(ActivateAbility(me, ghost, removeTwoId)).error shouldBe null
        driver.settle()
        withClue("the two-counter cost was paid") { driver.plusOne(ghost) shouldBe 0 }
        (top in driver.getExile(me)) shouldBe true

        // Playing the exiled land triggers "play a land from exile → +1/+1 counter".
        driver.playLand(me, top).error shouldBe null
        driver.settle()

        withClue("playing a land from exile put one +1/+1 counter on Ghost-Spider") {
            driver.plusOne(ghost) shouldBe 1
        }
        withClue("the exiled land was played onto the battlefield") {
            (top in driver.getExile(me)) shouldBe false
        }
    }
})
