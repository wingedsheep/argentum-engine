package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SuspendCardFromHand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Printed Suspend (CR 702.62a / 116.2f) as a special action is legal "any time you could begin to
 * cast this card" — instant speed for an instant or a card with flash, sorcery speed otherwise.
 * [AncestralVisionScenarioTest] proves the sorcery-speed side (Ancestral Vision is a Sorcery with
 * no flash); this file proves the instant-speed side with synthetic test cards, since no *real*
 * card in the engine's registry currently prints Suspend on an instant or a card with flash.
 *
 * Both cases reuse the same proof: put a spell on the stack first (the caster keeps priority right
 * after casting, but the stack is no longer empty), then confirm the suspend special action still
 * succeeds — the sorcery-speed-only gate (`canPlaySorcerySpeed`, which requires an empty stack)
 * would reject it, so success here demonstrates the instant-speed branch is actually exercised.
 */
class PrintedSuspendTimingTest : FunSpec({

    val testSuspendInstant = card("Test Suspend Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Suspend 2—{R} (...)\nDraw a card."
        spell {
            effect = Effects.DrawCards(1)
        }
        keywordAbility(KeywordAbility.suspend("{R}", 2))
    }

    val testSuspendFlashCreature = card("Test Suspend Flash Creature") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Flash\nSuspend 2—{G} (...)"
        keywords(Keyword.FLASH)
        keywordAbility(KeywordAbility.suspend("{G}", 2))
    }

    // No printed flash — instant-speed timing for this card comes entirely from
    // Test Flash Granter's battlefield grant, proving the permission side of "could begin to
    // cast this card" (CR 702.62a/c) honors a *granted* flash, not just a printed one.
    val testSuspendGrantedFlashCreature = card("Test Suspend Granted Flash Creature") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Suspend 2—{G} (...)"
        keywordAbility(KeywordAbility.suspend("{G}", 2))
    }

    val testFlashGranter = card("Test Flash Granter") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Wizard"
        power = 1
        toughness = 1
        oracleText = "Any player may cast Test Suspend Granted Flash Creature spells as though they had flash."
        staticAbility {
            ability = GrantFlashToSpellType(
                filter = GameObjectFilter.Any.named("Test Suspend Granted Flash Creature")
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                testSuspendInstant, testSuspendFlashCreature, testSuspendGrantedFlashCreature, testFlashGranter
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        return driver
    }

    test("an instant with printed suspend can be suspended with a non-empty stack (instant speed)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        driver.giveMana(me, Color.RED, 1)

        // Occupy the stack first: cast Lightning Bolt, which leaves the caster with priority but
        // a non-empty stack — the condition sorcery-speed suspend (Ancestral Vision) is rejected
        // under, per AncestralVisionScenarioTest's "can only be taken at sorcery speed" test.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.state.stack.isEmpty() shouldBe false
        driver.state.priorityPlayerId shouldBe me

        driver.submit(SuspendCardFromHand(me, card)).isSuccess shouldBe true
        driver.getExile(me).contains(card) shouldBe true
    }

    test("a non-instant card with printed flash and suspend can also be suspended with a non-empty stack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Flash Creature")
        driver.giveMana(me, Color.GREEN, 1)

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.state.stack.isEmpty() shouldBe false
        driver.state.priorityPlayerId shouldBe me

        driver.submit(SuspendCardFromHand(me, card)).isSuccess shouldBe true
        driver.getExile(me).contains(card) shouldBe true
    }

    test("a card with suspend and no printed flash, but granted flash, can be suspended with a non-empty stack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(me, "Test Flash Granter")
        val card = driver.putCardInHand(me, "Test Suspend Granted Flash Creature")
        driver.giveMana(me, Color.GREEN, 1)

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.state.stack.isEmpty() shouldBe false
        driver.state.priorityPlayerId shouldBe me

        driver.submit(SuspendCardFromHand(me, card)).isSuccess shouldBe true
        driver.getExile(me).contains(card) shouldBe true
    }

    test("the granted-flash suspend action is offered in legal actions with a non-empty stack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(me, "Test Flash Granter")
        val card = driver.putCardInHand(me, "Test Suspend Granted Flash Creature")
        driver.giveMana(me, Color.GREEN, 1)

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.state.stack.isEmpty() shouldBe false

        driver.legalActions(me).any {
            val action = it.action
            action is SuspendCardFromHand && action.cardId == card
        } shouldBe true
    }

    test("without the flash grant, the same creature can't be suspended with a non-empty stack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No Test Flash Granter on the battlefield this time — the creature has no printed flash,
        // so it should fall back to sorcery-speed-only, exactly as it did before the fix.
        val card = driver.putCardInHand(me, "Test Suspend Granted Flash Creature")
        driver.giveMana(me, Color.GREEN, 1)

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.state.stack.isEmpty() shouldBe false

        driver.legalActions(me).any {
            val action = it.action
            action is SuspendCardFromHand && action.cardId == card
        } shouldBe false

        driver.submit(SuspendCardFromHand(me, card)).isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
    }

    test("the suspend legal action is offered under normal circumstances") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        driver.giveMana(me, Color.RED, 1)

        driver.legalActions(me).any {
            val action = it.action
            action is SuspendCardFromHand && action.cardId == card
        } shouldBe true
    }

    test("a cast prohibition suppresses the suspend legal action") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        driver.giveMana(me, Color.RED, 1)
        val player = driver.state.getEntity(me)!!.with(CantCastSpellsComponent())
        driver.replaceState(driver.state.withEntity(me, player))

        driver.legalActions(me).any {
            val action = it.action
            action is SuspendCardFromHand && action.cardId == card
        } shouldBe false
    }

    test("a direct suspend action is rejected while the player cannot cast spells") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        driver.giveMana(me, Color.RED, 1)
        val player = driver.state.getEntity(me)!!.with(CantCastSpellsComponent())
        driver.replaceState(driver.state.withEntity(me, player))

        driver.submit(SuspendCardFromHand(me, card)).isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
    }

    test("explicit payment cannot pay a red suspend cost with a Forest") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        val forest = driver.putLandOnBattlefield(me, "Forest")

        driver.submit(
            SuspendCardFromHand(
                playerId = me,
                cardId = card,
                paymentStrategy = PaymentStrategy.Explicit(listOf(forest)),
            )
        ).isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
    }

    test("explicit payment rejects an opponent's mana source even when floating mana covers the cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        val opponentsMountain = driver.putLandOnBattlefield(opponent, "Mountain")
        driver.giveMana(me, Color.RED, 1)

        driver.submit(
            SuspendCardFromHand(
                playerId = me,
                cardId = card,
                paymentStrategy = PaymentStrategy.Explicit(listOf(opponentsMountain)),
            )
        ).isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
        driver.state.getEntity(opponentsMountain)?.has<TappedComponent>() shouldBe false
    }

    test("explicit payment rejects a non-mana permanent alongside a valid source") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        val creature = driver.putPermanentOnBattlefield(me, "Grizzly Bears")
        val mountain = driver.putLandOnBattlefield(me, "Mountain")

        driver.submit(
            SuspendCardFromHand(
                playerId = me,
                cardId = card,
                paymentStrategy = PaymentStrategy.Explicit(listOf(creature, mountain)),
            )
        ).isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe false
        driver.state.getEntity(mountain)?.has<TappedComponent>() shouldBe false
    }

    test("explicit payment succeeds when the named source matches the suspend cost's color") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Test Suspend Bolt")
        val mountain = driver.putLandOnBattlefield(me, "Mountain")

        driver.submit(
            SuspendCardFromHand(
                playerId = me,
                cardId = card,
                paymentStrategy = PaymentStrategy.Explicit(listOf(mountain)),
            )
        ).isSuccess shouldBe true
        driver.getExile(me).contains(card) shouldBe true
        driver.state.getEntity(mountain)?.has<TappedComponent>() shouldBe true
    }
})
