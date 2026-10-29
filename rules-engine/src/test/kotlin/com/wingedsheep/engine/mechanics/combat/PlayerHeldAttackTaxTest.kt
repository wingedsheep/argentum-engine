package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An [AttackTax] granted to a *player* — Summon: Yojimbo's "until your next turn, creatures can't
 * attack you unless their controller pays {2} for each of those creatures". The grant is the
 * printed Ghostly Prison tax held on the player's behalf: it taxes each creature attacking that
 * player (not their planeswalkers unless it says so), it outlives the effect's source, it ends at
 * the start of the granting player's next turn, and — like any attack tax — it never forces a
 * creature that must attack to pay (CR 508.1d).
 */
class PlayerHeldAttackTaxTest : FunSpec({

    val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val brute = card("Test Brute") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Ogre"
        power = 3
        toughness = 3
        oracleText = "This creature attacks each combat if able."
        staticAbility { ability = MustAttack() }
    }

    val prison = card("Test Prison") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
        oracleText = "Creatures can't attack you unless their controller pays {2} for each creature they control that's attacking you."
        staticAbility { ability = AttackTax(DynamicAmount.Fixed(2)) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(walker, brute, prison))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Hand [holder] a "can't attack you unless …pays {2} each" tax until their next turn. */
    fun GameTestDriver.grantTax(holder: EntityId, coversPlaneswalkers: Boolean = false) {
        replaceState(
            state.copy(
                grantedStaticAbilities = state.grantedStaticAbilities + GrantedStaticAbility(
                    entityId = holder,
                    ability = AttackTax(DynamicAmount.Fixed(2), coversPlaneswalkers = coversPlaneswalkers),
                    duration = Duration.UntilYourNextTurn,
                    controllerId = holder,
                )
            )
        )
    }

    fun GameTestDriver.readyCreature(name: String): EntityId =
        putCreatureOnBattlefield(player1, name).also { removeSummoningSickness(it) }

    test("taxes each creature attacking the player who holds it") {
        val driver = newDriver()
        val a = driver.readyCreature("Centaur Courser")
        val b = driver.readyCreature("Savannah Lions")
        driver.grantTax(driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(driver.player1, listOf(a, b), driver.player2)
        val decision = result.newState.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.requiredCost shouldBe "{1}{1}{1}{1}"
    }

    test("a tax held by the attacking player charges nothing") {
        val driver = newDriver()
        val a = driver.readyCreature("Centaur Courser")
        driver.grantTax(driver.player1)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(driver.player1, listOf(a), driver.player2)
        result.error shouldBe null
        (result.newState.pendingDecision is SelectManaSourcesDecision) shouldBe false
    }

    test("\"can't attack you\" leaves the holder's planeswalkers free to attack") {
        val driver = newDriver()
        val a = driver.readyCreature("Centaur Courser")
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        driver.grantTax(driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(driver.player1, mapOf(a to pw))
        result.error shouldBe null
        (result.newState.pendingDecision is SelectManaSourcesDecision) shouldBe false
    }

    test("\"you or planeswalkers you control\" taxes the planeswalker attack too") {
        val driver = newDriver()
        val a = driver.readyCreature("Centaur Courser")
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        driver.grantTax(driver.player2, coversPlaneswalkers = true)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(driver.player1, mapOf(a to pw))
        result.newState.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
            .requiredCost shouldBe "{1}{1}"
    }

    test("a creature that must attack isn't forced to pay the tax (CR 508.1d)") {
        val driver = newDriver()
        driver.readyCreature("Test Brute")
        driver.grantTax(driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("a must-attack creature with an untaxed planeswalker to attack still has to attack") {
        val driver = newDriver()
        val brute = driver.readyCreature("Test Brute")
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        driver.grantTax(driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe "Test Brute must attack this combat if able"
        driver.declareAttackers(driver.player1, mapOf(brute to pw)).error shouldBe null
    }

    test("a printed tax excuses a must-attack creature the same way") {
        val driver = newDriver()
        driver.readyCreature("Test Brute")
        driver.putPermanentOnBattlefield(driver.player2, "Test Prison")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("goad's \"a player other than the goader\" doesn't count a taxed player (CR 508.1d)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(walker, brute, prison))
        val p = driver.initMultiplayer(decks = List(3) { Deck.of("Forest" to 40) }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val courser = driver.putCreatureOnBattlefield(p[0], "Centaur Courser")
        driver.removeSummoningSickness(courser)
        // Goaded by P2; P3 — the only player other than the goader — holds the tax.
        driver.replaceState(driver.state.updateEntity(courser) { it.with(GoadedComponent(setOf(p[1]))) })
        driver.grantTax(p[2])
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, p[0])
            .single { it.actionType == "DeclareAttackers" }.mandatoryAttackers shouldBe listOf(courser)
        driver.declareAttackers(p[0], emptyMap()).error shouldNotBe null
        // Attacking the goader is the free way to obey goad — not a broken "other than" requirement.
        driver.declareAttackers(p[0], mapOf(courser to p[1])).error shouldBe null
    }

    test("Taunt doesn't bind a creature that could attack the taunter only by paying") {
        val driver = newDriver()
        driver.readyCreature("Centaur Courser")
        driver.replaceState(
            driver.state.updateEntity(driver.player1) {
                it.with(MustAttackPlayerComponent(defenderId = driver.player2, activeThisTurn = true))
            }
        )
        driver.grantTax(driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, driver.player1)
            .single { it.actionType == "DeclareAttackers" }.mandatoryAttackers shouldBe null
        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("the tax ends as the holder's next turn begins") {
        val driver = newDriver()
        driver.grantTax(driver.player1)
        driver.passPriorityUntil(Step.END)
        driver.state.grantedStaticAbilities.size shouldBe 1

        // Through the opponent's turn the grant stands; it lapses in the holder's untap step.
        driver.passPriorityUntil(Step.UPKEEP, driver.player2)
        driver.state.grantedStaticAbilities.size shouldBe 1
        driver.passPriorityUntil(Step.UPKEEP, driver.player1)
        driver.state.grantedStaticAbilities.size shouldBe 0
    }
})
