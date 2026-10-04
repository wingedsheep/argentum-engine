package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OpponentsMustAttackYou
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [OpponentsMustAttackYou] — "each opponent must attack you or a planeswalker you control with at
 * least one creature each combat if able" (Trove of Temptation). A player-level requirement
 * (CR 508.1d): one creature at the controller or their planeswalker is enough, any creature will
 * do, and it lapses when no creature can attack them for free.
 */
class OpponentsMustAttackYouTest : FunSpec({

    val trove = card("Test Trove") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment"
        oracleText = "Each opponent must attack you or a planeswalker you control with at least one creature each combat if able."
        staticAbility { ability = OpponentsMustAttackYou }
    }

    val prison = card("Test Prison") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
        oracleText = "Creatures can't attack you unless their controller pays {2} for each creature they control that's attacking you."
        staticAbility { ability = AttackTax(DynamicAmount.Fixed(2)) }
    }

    val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val loner = card("Test Loner") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Cat"
        power = 2
        toughness = 2
        oracleText = "This creature can't attack unless another creature also attacks."
        staticAbility { ability = CantAttackUnlessCoAttacker(GameObjectFilter.Creature) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(trove, prison, walker, loner))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("declaring no attackers is rejected while a creature can attack the Trove's controller") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(bears)
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(bears to driver.player2)).error shouldBe null
    }

    test("one attacker is enough — the rest may stay home") {
        val driver = newDriver()
        val a = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val b = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        driver.removeSummoningSickness(a)
        driver.removeSummoningSickness(b)
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Neither creature is individually mandatory: the requirement names no creature.
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, driver.player1)
            .single { it.actionType == "DeclareAttackers" }.mandatoryAttackers shouldBe null
        driver.declareAttackers(driver.player1, mapOf(b to driver.player2)).error shouldBe null
    }

    test("attacking a planeswalker the Trove's controller controls satisfies it") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(bears)
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(bears to pw)).error shouldBe null
    }

    test("no creature able to attack — summoning sick — means no requirement") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")

        // With no able attacker the declaration is skipped outright; the turn reaches its end step
        // instead of stalling on a requirement nobody can meet.
        driver.passPriorityUntil(Step.END)
        driver.state.turnNumber shouldBe 1
        driver.state.step shouldBe Step.END
    }

    test("an attack tax lifts the requirement — the attacker never has to pay it (CR 508.1d)") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(bears)
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")
        driver.putPermanentOnBattlefield(driver.player2, "Test Prison")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("the Trove's controller is under no requirement on their own turn") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(bears)
        driver.putPermanentOnBattlefield(driver.player1, "Test Trove")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("a lone creature that can't attack alone isn't able — no attack is legal") {
        val driver = newDriver()
        val cat = driver.putCreatureOnBattlefield(driver.player1, "Test Loner")
        driver.removeSummoningSickness(cat)
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("with a co-attacker available the restricted creature counts as able") {
        val driver = newDriver()
        val cat = driver.putCreatureOnBattlefield(driver.player1, "Test Loner")
        val lions = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        driver.removeSummoningSickness(cat)
        driver.removeSummoningSickness(lions)
        driver.putPermanentOnBattlefield(driver.player2, "Test Trove")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
    }
})
