package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.TheSeedcore
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Seedcore (ONE #259) — Land — Sphere.
 *
 *   {T}: Add {C}.
 *   {T}: Add one mana of any color. Spend this mana only to cast Phyrexian creature spells.
 *   Corrupted — {T}: Target 1/1 creature gets +2/+1 until end of turn. Activate only if an
 *   opponent has three or more poison counters.
 */
class TheSeedcoreScenarioTest : FunSpec({

    val phyrexianCreature = CardDefinition.creature(
        name = "Test Phyrexian",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Phyrexian"), Subtype("Beast")),
        power = 2,
        toughness = 2
    )
    val plainCreature = CardDefinition.creature(
        name = "Test Elf",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1
    )
    val oneOneCreature = CardDefinition.creature(
        name = "Test Mite",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Mite")),
        power = 1,
        toughness = 1
    )
    val oneTwoCreature = CardDefinition.creature(
        name = "Test Wall",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Wall")),
        power = 1,
        toughness = 2
    )

    val colorlessAbility = TheSeedcore.activatedAbilities[0].id
    val restrictedAbility = TheSeedcore.activatedAbilities[1].id
    val pumpAbility = TheSeedcore.activatedAbilities[2].id

    fun setup(opponentPoison: Int = 0): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TheSeedcore + phyrexianCreature + plainCreature + oneOneCreature + oneTwoCreature)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.activePlayer!!
        val seedcore = driver.putPermanentOnBattlefield(p1, "The Seedcore")
        if (opponentPoison > 0) {
            val opp = driver.getOpponent(p1)
            driver.replaceState(
                driver.state.updateEntity(opp) {
                    it.with(CountersComponent(mapOf(CounterType.POISON to opponentPoison)))
                }
            )
        }
        return driver to seedcore
    }

    test("restricted mana pays for a Phyrexian creature spell") {
        val (driver, seedcore) = setup()
        val p1 = driver.activePlayer!!
        driver.submit(ActivateAbility(p1, seedcore, restrictedAbility, manaColorChoice = Color.GREEN))
        val card = driver.putCardInHand(p1, "Test Phyrexian")
        driver.submit(CastSpell(playerId = p1, cardId = card, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldBe Outcome.Done
    }

    test("restricted mana cannot pay for a non-Phyrexian creature spell") {
        val (driver, seedcore) = setup()
        val p1 = driver.activePlayer!!
        driver.submit(ActivateAbility(p1, seedcore, restrictedAbility, manaColorChoice = Color.GREEN))
        val card = driver.putCardInHand(p1, "Test Elf")
        driver.submit(CastSpell(playerId = p1, cardId = card, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldNotBe Outcome.Done
    }

    test("colorless ability is unrestricted") {
        val (driver, seedcore) = setup()
        val p1 = driver.activePlayer!!
        driver.submit(ActivateAbility(p1, seedcore, colorlessAbility))
        val card = driver.putCardInHand(p1, "Test Mite")
        driver.submit(CastSpell(playerId = p1, cardId = card, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldBe Outcome.Done
    }

    test("corrupted: target 1/1 creature gets +2/+1") {
        val (driver, seedcore) = setup(opponentPoison = 3)
        val p1 = driver.activePlayer!!
        val mite = driver.putCreatureOnBattlefield(p1, "Test Mite")
        driver.submit(
            ActivateAbility(p1, seedcore, pumpAbility, targets = listOf(ChosenTarget.Permanent(mite)))
        ).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.getPower(mite) shouldBe 3
        driver.state.projectedState.getToughness(mite) shouldBe 2
    }

    test("corrupted ability can't target a creature that isn't 1/1") {
        val (driver, seedcore) = setup(opponentPoison = 3)
        val p1 = driver.activePlayer!!
        val wall = driver.putCreatureOnBattlefield(p1, "Test Wall")
        driver.submit(
            ActivateAbility(p1, seedcore, pumpAbility, targets = listOf(ChosenTarget.Permanent(wall)))
        ).error shouldNotBe null
    }

    test("corrupted ability can't be activated below three opponent poison") {
        val (driver, seedcore) = setup(opponentPoison = 2)
        val p1 = driver.activePlayer!!
        val mite = driver.putCreatureOnBattlefield(p1, "Test Mite")
        driver.submit(
            ActivateAbility(p1, seedcore, pumpAbility, targets = listOf(ChosenTarget.Permanent(mite)))
        ).error shouldNotBe null
    }
})
