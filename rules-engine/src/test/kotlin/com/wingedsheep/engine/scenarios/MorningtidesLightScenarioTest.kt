package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.MorningtidesLight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Morningtide's Light — {3}{W} Sorcery (ECL)
 *
 * "Exile any number of target creatures. At the beginning of the next end step, return those
 * cards to the battlefield tapped under their owners' control.
 * Until your next turn, prevent all damage that would be dealt to you.
 * Exile Morningtide's Light."
 *
 * ## Covered scenarios
 * - Three targets across both players: all three exiled, all three back tapped at end step,
 *   each under its *owner's* control
 * - The legal action offers every creature on the board (the "any number" cap), not just one
 * - Casting with zero targets is legal and still sets up the damage shield
 */
class MorningtidesLightScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MorningtidesLight)
        return driver
    }

    test("exiles every targeted creature and returns them all tapped at the next end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val mine2 = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Morningtide's Light")
        driver.giveMana(me, Color.WHITE, 4)

        driver.castSpellWithTargets(
            me, spell,
            listOf(
                ChosenTarget.Permanent(mine1),
                ChosenTarget.Permanent(mine2),
                ChosenTarget.Permanent(theirs)
            )
        )
        driver.bothPass()

        // All three are gone from the battlefield, and the spell exiled itself.
        driver.getCreatures(me).shouldHaveSize(0)
        driver.getCreatures(opponent).shouldHaveSize(0)
        driver.getExileCardNames(me) shouldContainExactlyInAnyOrder
            listOf("Grizzly Bears", "Centaur Courser", "Morningtide's Light")
        driver.getExileCardNames(opponent) shouldContainExactlyInAnyOrder listOf("Grizzly Bears")

        // At the beginning of the next end step one delayed trigger per exiled creature fires.
        driver.passPriorityUntil(Step.END)
        driver.stackSize shouldBe 3
        repeat(3) { driver.bothPass() }
        driver.stackSize shouldBe 0

        val returnedMine = driver.getCreatures(me)
        val returnedTheirs = driver.getCreatures(opponent)
        returnedMine shouldHaveSize 2
        returnedTheirs shouldHaveSize 1
        returnedMine.map { driver.getCardName(it) } shouldContainExactlyInAnyOrder
            listOf("Grizzly Bears", "Centaur Courser")
        (returnedMine + returnedTheirs).forEach { driver.isTapped(it) shouldBe true }
    }

    test("legal action offers every creature on the board as a target, not just one") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Morningtide's Light")
        driver.giveMana(me, Color.WHITE, 4)

        val cast = driver.legalActions(me).single {
            it.actionType == "CastSpell" && it.description == "Cast Morningtide's Light"
        }

        cast.targetCount shouldBe 3        // "any number" — every legal target, not the static 1
        cast.minTargets shouldBe 0
        cast.validTargets!! shouldHaveSize 3
    }

    test("casting with no targets is legal and still prevents damage to you") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(me, "Morningtide's Light")
        driver.giveMana(me, Color.WHITE, 4)

        driver.castSpellWithTargets(me, spell, emptyList())
        driver.bothPass()

        driver.getExileCardNames(me) shouldContainExactlyInAnyOrder listOf("Morningtide's Light")

        // Opponent's burn is prevented until my next turn.
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(me)))
            .isSuccess shouldBe true
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 20
    }
})
