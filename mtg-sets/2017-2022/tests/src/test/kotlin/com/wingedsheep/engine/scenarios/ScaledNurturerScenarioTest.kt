package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.ScaledNurturer
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scaled Nurturer (canonical CLB #252).
 *
 * {T}: Add {G}. When you spend this mana to cast a Dragon creature spell, you gain 2 life.
 *
 * The first non-copy use of [com.wingedsheep.sdk.scripting.effects.ManaSpellRider.TriggersWhenSpent]:
 * an arbitrary effect queued above the spell. The tests pin the trigger to its spell filter, its
 * timing (it resolves before the Dragon), its multiplicity (one trigger per {G} spent), and the
 * ruling that it fires even when the Nurturer is gone by the time the mana is spent.
 */
class ScaledNurturerScenarioTest : FunSpec({

    val nurturerAbilityId = ScaledNurturer.activatedAbilities[0].id

    val dragon = CardDefinition.creature(
        name = "Test Moss Wyrm",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 2,
        toughness = 2,
    )

    val bigDragon = CardDefinition.creature(
        name = "Test Elder Moss Wyrm",
        manaCost = ManaCost.parse("{G}{G}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 4,
        toughness = 4,
    )

    val nonDragon = CardDefinition.creature(
        name = "Test Moss Elf",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ScaledNurturer, dragon, bigDragon, nonDragon))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    fun tapNurturer(driver: GameTestDriver, you: EntityId, nurturer: EntityId) {
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = nurturer, abilityId = nurturerAbilityId))
    }

    test("a Dragon creature spell paid with its mana gains 2 life, before the Dragon resolves") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nurturer = driver.putCreatureOnBattlefield(you, "Scaled Nurturer")
        driver.removeSummoningSickness(nurturer)
        val wyrm = driver.putCardInHand(you, "Test Moss Wyrm")
        tapNurturer(driver, you, nurturer)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.state.stack.size shouldBe 2

        driver.bothPass()
        // The life-gain trigger sat above the spell: it has resolved, the Dragon hasn't yet.
        driver.getLifeTotal(you) shouldBe 22
        driver.state.stack shouldContain wyrm

        driver.bothPass()
        driver.findPermanent(you, "Test Moss Wyrm") shouldBe wyrm
        driver.getLifeTotal(you) shouldBe 22
    }

    test("a non-Dragon creature spell paid with its mana gains nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nurturer = driver.putCreatureOnBattlefield(you, "Scaled Nurturer")
        driver.removeSummoningSickness(nurturer)
        val elf = driver.putCardInHand(you, "Test Moss Elf")
        tapNurturer(driver, you, nurturer)

        // "The mana created by Scaled Nurturer can be spent on anything" — it pays for the Elf, and
        // the rider is consumed but doesn't fire.
        driver.castSpell(you, elf).error shouldBe null
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 20
    }

    test("two Nurturers' mana spent on one Dragon triggers twice") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val first = driver.putCreatureOnBattlefield(you, "Scaled Nurturer")
        val second = driver.putCreatureOnBattlefield(you, "Scaled Nurturer")
        driver.removeSummoningSickness(first)
        driver.removeSummoningSickness(second)
        val wyrm = driver.putCardInHand(you, "Test Elder Moss Wyrm")
        tapNurturer(driver, you, first)
        tapNurturer(driver, you, second)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.state.stack.size shouldBe 3
        driver.bothPass()
        driver.bothPass()
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 24
    }

    test("the trigger fires even if the Nurturer has left the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nurturer = driver.putCreatureOnBattlefield(you, "Scaled Nurturer")
        driver.removeSummoningSickness(nurturer)
        val wyrm = driver.putCardInHand(you, "Test Moss Wyrm")
        tapNurturer(driver, you, nurturer)
        driver.moveToGraveyard(nurturer)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 22
    }

    test("auto-pay tapping the Nurturer for a Dragon carries the rider too") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nurturer = driver.putCreatureOnBattlefield(you, "Scaled Nurturer")
        driver.removeSummoningSickness(nurturer)
        val wyrm = driver.putCardInHand(you, "Test Moss Wyrm")

        // No manual activation: the solver taps the Nurturer as the only green source.
        driver.castSpell(you, wyrm).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 22
    }
})
