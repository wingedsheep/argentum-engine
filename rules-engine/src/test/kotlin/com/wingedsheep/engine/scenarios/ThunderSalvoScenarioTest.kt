package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.CaughtInTheCrossfire
import com.wingedsheep.mtg.sets.definitions.otj.cards.ExplosiveDerailment
import com.wingedsheep.mtg.sets.definitions.otj.cards.ThunderSalvo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Thunder Salvo. */
class ThunderSalvoScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + ThunderSalvo + ExplosiveDerailment + CaughtInTheCrossfire
        )
        return driver
    }

    // ---------------------------------------------------------------------
    // Thunder Salvo
    // ---------------------------------------------------------------------

    test("Thunder Salvo with no other spells cast deals 2 damage (kills a 2/2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2

        val salvo = driver.putCardInHand(player, "Thunder Salvo")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = salvo,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        driver.bothPass()

        // 2 damage to a 2/2 -> dies.
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }

    test("Thunder Salvo deals 2 + other spells cast this turn (3 after one other spell)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A 3-toughness creature survives 2 damage but dies to 3.
        val giant = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        // Cast one other spell first this turn (a 0-cost-ish instant resolved fully).
        val otherSpell = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = otherSpell,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Now Thunder Salvo: X = 2 + 1 other spell = 3 -> kills the 3/3.
        val salvo = driver.putCardInHand(player, "Thunder Salvo")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = salvo,
                targets = listOf(ChosenTarget.Permanent(giant))
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }
})
