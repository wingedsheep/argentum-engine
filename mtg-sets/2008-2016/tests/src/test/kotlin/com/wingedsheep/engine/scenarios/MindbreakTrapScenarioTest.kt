package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.zen.cards.MindbreakTrap
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mindbreak Trap {2}{U}{U} — Instant — Trap.
 *
 * "If an opponent cast three or more spells this turn, you may pay {0} rather than pay this
 *  spell's mana cost. Exile any number of target spells."
 *
 * Pins the per-opponent trap condition (rulings: casts count, not resolutions; a single opponent
 * must reach three, so three opponents with one spell each don't unlock it) and that every chosen
 * spell is exiled while an unchosen one still resolves.
 */
class MindbreakTrapScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MindbreakTrap))
        return driver
    }

    fun freeTrap(you: EntityId, trap: EntityId, spells: List<EntityId>) = CastSpell(
        playerId = you,
        cardId = trap,
        targets = spells.map { ChosenTarget.Spell(it) },
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
    )

    fun bolt(driver: GameTestDriver, caster: EntityId, at: EntityId): EntityId {
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt, targets = listOf(at)).error shouldBe null
        return bolt
    }

    test("after an opponent's third spell it costs {0} and exiles every chosen spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        // The first spell resolves; the condition counts casts, not what is still on the stack.
        val elves = driver.putCardInHand(opponent, "Llanowar Elves")
        driver.giveMana(opponent, Color.GREEN, 1)
        driver.castSpell(opponent, elves).error shouldBe null
        driver.bothPass()

        val first = bolt(driver, opponent, you)
        val second = bolt(driver, opponent, you)
        driver.passPriority(opponent)

        val trap = driver.putCardInHand(you, "Mindbreak Trap")
        driver.submit(freeTrap(you, trap, listOf(first, second))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getLifeTotal(you) shouldBe 20
        driver.getExileCardNames(opponent) shouldBe listOf("Lightning Bolt", "Lightning Bolt")
        driver.getGraveyardCardNames(you) shouldBe listOf("Mindbreak Trap")
    }

    test("an unchosen spell still resolves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        val first = bolt(driver, opponent, you)
        bolt(driver, opponent, you)
        bolt(driver, opponent, you)
        driver.passPriority(opponent)

        val trap = driver.putCardInHand(you, "Mindbreak Trap")
        driver.submit(freeTrap(you, trap, listOf(first))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getLifeTotal(you) shouldBe 14
        driver.getExileCardNames(opponent) shouldBe listOf("Lightning Bolt")
    }

    test("two spells aren't enough for the free cast, but the mana cost still works") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        bolt(driver, opponent, you)
        val second = bolt(driver, opponent, you)
        driver.passPriority(opponent)

        val trap = driver.putCardInHand(you, "Mindbreak Trap")
        driver.submit(freeTrap(you, trap, listOf(second))).error shouldNotBe null

        driver.giveMana(you, Color.BLUE, 4)
        driver.castSpellWithTargets(you, trap, listOf(ChosenTarget.Spell(second))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getLifeTotal(you) shouldBe 17
        driver.getExileCardNames(opponent) shouldBe listOf("Lightning Bolt")
    }

    test("three opponents casting one spell each don't unlock the free cast") {
        val driver = createDriver()
        val players = driver.initMultiplayer(decks = List(4) { Deck.of("Island" to 40) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (first, second, third, you) = players
        driver.activePlayer shouldBe first

        val spells = listOf(first, second, third).map { caster ->
            driver.priorityPlayer shouldBe caster
            val spell = bolt(driver, caster, you)
            driver.passPriority(caster)
            spell
        }
        driver.priorityPlayer shouldBe you

        val trap = driver.putCardInHand(you, "Mindbreak Trap")
        driver.submit(freeTrap(you, trap, spells)).error shouldNotBe null

        // The same targets are legal for the full mana cost — only the trap condition failed.
        driver.giveMana(you, Color.BLUE, 4)
        driver.castSpellWithTargets(you, trap, spells.map { ChosenTarget.Spell(it) }).error shouldBe null
    }
})
