package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.all.cards.ForceOfWill
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Force of Will {3}{U}{U} — Instant.
 *
 * "You may pay 1 life and exile a blue card from your hand rather than pay this spell's mana cost.
 *  Counter target spell."
 *
 * The first self-alternative cost that bundles a life payment with a pitched card, and — unlike
 * Force of Negation — with no turn gate. Proves both halves of the cost are paid, that a nonblue
 * card can't be pitched, and that the cast works on your own turn too.
 */
class ForceOfWillScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ForceOfWill))
        return driver
    }

    fun pitch(you: EntityId, force: EntityId, target: EntityId, fodder: EntityId) = CastSpell(
        playerId = you,
        cardId = force,
        targets = listOf(ChosenTarget.Spell(target)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(fodder))
    )

    test("pitch cast pays 1 life, exiles the blue card, and counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        val elves = driver.putCardInHand(opponent, "Llanowar Elves")
        driver.giveMana(opponent, Color.GREEN, 1)
        driver.castSpell(opponent, elves).error shouldBe null
        driver.passPriority(opponent)

        val fodder = driver.putCardInHand(you, "Counterspell")
        val force = driver.putCardInHand(you, "Force of Will")
        driver.submit(pitch(you, force, elves, fodder)).error shouldBe null
        driver.getLifeTotal(you) shouldBe 19
        while (driver.stackSize > 0) driver.bothPass()

        driver.getGraveyardCardNames(opponent) shouldBe listOf("Llanowar Elves")
        driver.getExileCardNames(you) shouldBe listOf("Counterspell")
        driver.getGraveyardCardNames(you) shouldBe listOf("Force of Will")
    }

    test("a nonblue card can't be pitched") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, targets = listOf(you)).error shouldBe null
        driver.passPriority(opponent)

        val redCard = driver.putCardInHand(you, "Lightning Bolt")
        val force = driver.putCardInHand(you, "Force of Will")
        driver.submit(pitch(you, force, bolt, redCard)).error shouldNotBe null
        driver.getLifeTotal(you) shouldBe 20
    }

    test("the pitch cost has no turn gate: it works on your own turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.passPriority(you)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, targets = listOf(you)).error shouldBe null
        driver.passPriority(opponent)

        val fodder = driver.putCardInHand(you, "Counterspell")
        val force = driver.putCardInHand(you, "Force of Will")
        driver.submit(pitch(you, force, bolt, fodder)).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getLifeTotal(you) shouldBe 19
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Lightning Bolt")
    }
})
