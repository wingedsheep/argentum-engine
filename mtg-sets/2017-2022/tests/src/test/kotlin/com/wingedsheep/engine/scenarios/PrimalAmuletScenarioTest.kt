package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.PrimalAmulet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Primal Amulet // Primal Wellspring (XLN #243).
 *
 * Pins the parts that depend on how the engine runs the script:
 *  - the {1} discount lets a {1}{R} instant be cast off a single {R};
 *  - the cast trigger's fourth counter offers the optional "remove those counters and transform",
 *    and declining leaves the Amulet on its front face with its counters;
 *  - Primal Wellspring's any-color mana carries the copy rider (the first any-color
 *    [com.wingedsheep.sdk.scripting.effects.ManaSpellRider.TriggersWhenSpent] copy), so an
 *    instant paid with it resolves twice.
 */
class PrimalAmuletScenarioTest : FunSpec({

    val wellspringAbilityId = PrimalAmulet.backFace!!.activatedAbilities.first().id

    /** A targetless {1}{R} instant: the discount makes it castable off one {R}. */
    val redInstant = CardDefinition(
        name = "Test Red Ember",
        manaCost = ManaCost.parse("{1}{R}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "You gain 2 life.",
        script = CardScript.spell(effect = Effects.GainLife(2))
    )

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PrimalAmulet, redInstant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cardName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun chargeCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun resolveAll(driver: GameTestDriver, player: EntityId, transform: Boolean) {
        var guard = 0
        while (guard++ < 40 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            when (driver.pendingDecision) {
                is YesNoDecision -> driver.submitYesNo(player, transform)
                null -> driver.bothPass()
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun castEmber(driver: GameTestDriver, player: EntityId) {
        val ember = driver.putCardInHand(player, "Test Red Ember")
        driver.giveMana(player, Color.RED, 1) // {1}{R} minus the Amulet's {1}
        driver.castSpell(player, ember).error shouldBe null
    }

    test("one cast adds a charge counter, and the {1} discount applies") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val amulet = driver.putPermanentOnBattlefield(p1, "Primal Amulet")

        castEmber(driver, p1)
        resolveAll(driver, p1, transform = true)

        chargeCounters(driver, amulet) shouldBe 1
        cardName(driver, amulet) shouldBe "Primal Amulet"
        driver.getLifeTotal(p1) shouldBe 22
    }

    test("declining at four counters keeps the Amulet and its counters") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val amulet = driver.putPermanentOnBattlefield(p1, "Primal Amulet")
        driver.addComponent(amulet, CountersComponent(mapOf(CounterType.CHARGE to 3)))

        castEmber(driver, p1)
        resolveAll(driver, p1, transform = false)

        cardName(driver, amulet) shouldBe "Primal Amulet"
        chargeCounters(driver, amulet) shouldBe 4
    }

    test("the fourth counter transforms it, and Wellspring mana copies the instant it pays for") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val amulet = driver.putPermanentOnBattlefield(p1, "Primal Amulet")
        driver.addComponent(amulet, CountersComponent(mapOf(CounterType.CHARGE to 3)))

        castEmber(driver, p1)
        resolveAll(driver, p1, transform = true)

        cardName(driver, amulet) shouldBe "Primal Wellspring"
        chargeCounters(driver, amulet) shouldBe 0
        driver.getLifeTotal(p1) shouldBe 22

        // Wellspring's red pays the {R}; the Amulet's discount is gone, so add the {1} too.
        driver.submitSuccess(
            ActivateAbility(p1, amulet, wellspringAbilityId, manaColorChoice = Color.RED)
        )
        val ember = driver.putCardInHand(p1, "Test Red Ember")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, ember).error shouldBe null
        resolveAll(driver, p1, transform = false)

        // 22 + 2 (copy) + 2 (original).
        driver.getLifeTotal(p1) shouldBe 26
    }
})
