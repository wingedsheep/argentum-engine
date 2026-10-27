package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanOnlyAttackAlone
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.MustAttack
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * [CanOnlyAttackAlone] — "this creature can only attack alone" (Master of Cruelties). A
 * declaration restriction (CR 508.1c) over the whole attacker set (CR 506.5), weighed against
 * attack requirements by CR 508.1d: the declaration obeying the most requirements wins.
 */
class CanOnlyAttackAloneTest : FunSpec({

    val loner = card("Test Loner") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Demon"
        power = 1
        toughness = 4
        oracleText = "This creature can only attack alone."
        staticAbility { ability = CanOnlyAttackAlone() }
    }

    val eager = card("Test Eager") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        oracleText = "This creature attacks each combat if able."
        staticAbility { ability = MustAttack() }
    }

    val melee = card("Test Melee") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment"
        oracleText = "All creatures attack each combat if able."
        staticAbility { ability = MustAttack(GroupFilter.AllCreatures) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(loner, eager, melee))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.creature(name: String): EntityId =
        putCreatureOnBattlefield(player1, name).also { removeSummoningSickness(it) }

    /** A second requirement on [id], resolved onto it ("attacks this turn if able"). */
    fun GameTestDriver.markMustAttack(id: EntityId) =
        replaceState(state.updateEntity(id) { it.with(MustAttackThisTurnComponent) })

    fun GameTestDriver.attackAction(): LegalAction =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, player1)
            .single { it.actionType == "DeclareAttackers" }

    test("it attacks alone, or the others attack without it — never together") {
        val driver = newDriver()
        val master = driver.creature("Test Loner")
        val bears = driver.creature("Centaur Courser")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().loneAttackers shouldBe listOf(master)
        val both = driver.declareAttackers(driver.player1, mapOf(master to driver.player2, bears to driver.player2))
        both.error shouldNotBe null
        both.error!! shouldContain "Test Loner can only attack alone"
        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2)).error shouldBe null
    }

    test("attacking alone is legal") {
        val driver = newDriver()
        val master = driver.creature("Test Loner")
        driver.creature("Centaur Courser")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
    }

    test("another creature that must attack does, and it stays home (the card's ruling)") {
        val driver = newDriver()
        val master = driver.creature("Test Loner")
        val goblin = driver.creature("Test Eager")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().mandatoryAttackers shouldBe listOf(goblin)
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(goblin to driver.player2)).error shouldBe null
    }

    test("both required, one other creature: a tie — either attack is legal, neither is locked in") {
        val driver = newDriver()
        driver.creature("Test Loner")
        driver.creature("Centaur Courser")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().mandatoryAttackers shouldBe null
        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
    }

    test("in a tie it may attack alone") {
        val driver = newDriver()
        val master = driver.creature("Test Loner")
        driver.creature("Centaur Courser")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
    }

    test("in a tie the other creature may attack without it") {
        val driver = newDriver()
        driver.creature("Test Loner")
        val bears = driver.creature("Centaur Courser")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2)).error shouldBe null
    }

    test("two other required creatures obey more requirements — they attack, it stays home") {
        val driver = newDriver()
        val master = driver.creature("Test Loner")
        val bears = driver.creature("Centaur Courser")
        val lions = driver.creature("Savannah Lions")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().mandatoryAttackers!! shouldContainExactlyInAnyOrder listOf(bears, lions)
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2, lions to driver.player2))
            .error shouldBe null
    }

    test("required and on its own, it must attack") {
        val driver = newDriver()
        val master = driver.creature("Test Loner")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().mandatoryAttackers shouldBe listOf(master)
        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
    }

    test("requirements are counted, not creatures: two of its own outweigh one other creature's (CR 508.1d)") {
        val driver = newDriver()
        val master = driver.creature("Test Loner").also { driver.markMustAttack(it) }
        val bears = driver.creature("Centaur Courser")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().mandatoryAttackers shouldBe listOf(master)
        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
    }

    test("two requirements of its own tie two other required creatures — either attack is legal") {
        val driver = newDriver()
        val master = driver.creature("Test Loner").also { driver.markMustAttack(it) }
        val bears = driver.creature("Centaur Courser")
        driver.creature("Savannah Lions")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackAction().mandatoryAttackers shouldBe null
        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(master to driver.player2)).error shouldBe null
    }

    test("in that tie the two others may attack without it") {
        val driver = newDriver()
        driver.creature("Test Loner").also { driver.markMustAttack(it) }
        val bears = driver.creature("Centaur Courser")
        val lions = driver.creature("Savannah Lions")
        driver.putPermanentOnBattlefield(driver.player1, "Test Melee")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2, lions to driver.player2))
            .error shouldBe null
    }
})
