package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ErtaisScorn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ertai's Scorn {1}{U}{U} — Instant.
 *
 * "This spell costs {U} less to cast if an opponent cast two or more spells this turn.
 *  Counter target spell."
 *
 * Pins the self-cast *colored* reduction (the discount removes a {U}, so {1}{U} pays for it) and
 * its per-opponent gate: one spell isn't enough, and two opponents casting one spell each don't
 * add up.
 */
class ErtaisScornScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ErtaisScorn))
        return driver
    }

    fun bolt(driver: GameTestDriver, caster: EntityId, at: EntityId): EntityId {
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt, targets = listOf(at)).error shouldBe null
        return bolt
    }

    /** Exactly {1}{U}: enough only for the discounted cost. */
    fun giveDiscountedMana(driver: GameTestDriver, you: EntityId) {
        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 1)
    }

    test("after an opponent's second spell it costs {1}{U} and counters the target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        bolt(driver, opponent, you)
        val second = bolt(driver, opponent, you)
        driver.passPriority(opponent)

        val scorn = driver.putCardInHand(you, "Ertai's Scorn")
        giveDiscountedMana(driver, you)
        driver.castSpellWithTargets(you, scorn, listOf(ChosenTarget.Spell(second))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        // Only the uncountered bolt dealt damage.
        driver.getLifeTotal(you) shouldBe 17
        driver.getGraveyardCardNames(you) shouldBe listOf("Ertai's Scorn")
    }

    test("one opponent spell isn't enough for the discount") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        val only = bolt(driver, opponent, you)
        driver.passPriority(opponent)

        val scorn = driver.putCardInHand(you, "Ertai's Scorn")
        giveDiscountedMana(driver, you)
        driver.castSpellWithTargets(you, scorn, listOf(ChosenTarget.Spell(only))).error shouldNotBe null

        // The full {1}{U}{U} still casts it.
        driver.giveMana(you, Color.BLUE, 1)
        driver.castSpellWithTargets(you, scorn, listOf(ChosenTarget.Spell(only))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()
        driver.getLifeTotal(you) shouldBe 20
    }

    test("two opponents casting one spell each don't unlock the discount") {
        val driver = createDriver()
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Island" to 40) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (first, second, you) = players
        driver.activePlayer shouldBe first

        val spells = listOf(first, second).map { caster ->
            driver.priorityPlayer shouldBe caster
            val spell = bolt(driver, caster, you)
            driver.passPriority(caster)
            spell
        }
        driver.priorityPlayer shouldBe you

        val scorn = driver.putCardInHand(you, "Ertai's Scorn")
        giveDiscountedMana(driver, you)
        driver.castSpellWithTargets(you, scorn, listOf(ChosenTarget.Spell(spells.last()))).error shouldNotBe null

        // The full {1}{U}{U} still casts it, so the refusal above was the missing discount.
        driver.giveMana(you, Color.BLUE, 1)
        driver.castSpellWithTargets(you, scorn, listOf(ChosenTarget.Spell(spells.last()))).error shouldBe null
    }
})
