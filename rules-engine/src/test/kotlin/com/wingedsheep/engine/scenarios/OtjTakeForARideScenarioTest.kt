package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TakeForARide
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Take for a Ride (OTJ {2}{R} sorcery, Threaten variant).
 *
 * Effect: gain control of target creature until end of turn, untap it, it gains haste.
 * Conditional flash: castable at instant speed only while you've committed a crime this turn.
 */
class OtjTakeForARideScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TakeForARide)
        return driver
    }

    test("gains control of target creature, untaps it, grants haste") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stolen = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2
        driver.tapPermanent(stolen)
        driver.isTapped(stolen) shouldBe true

        val spell = driver.putCardInHand(player, "Take for a Ride")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2) // {2}{R}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(stolen))
            )
        )
        driver.bothPass()

        // Now controlled by the caster (control change is a projected floating effect),
        // untapped, and has haste.
        val projected = projector.project(driver.state)
        projected.getController(stolen) shouldBe player
        driver.isTapped(stolen) shouldBe false
        projected.hasKeyword(stolen, Keyword.HASTE) shouldBe true
    }

    test("conditional flash: not castable at instant speed until you've committed a crime") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        val player = driver.player1
        val opponent = driver.player2

        // Move to the opponent's turn so it's an instant-speed window for player1.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        val onOpponentTurn = driver.state.activePlayerId == opponent

        // Take for a Ride needs a legal creature target to be offered at all.
        driver.putCreatureOnBattlefield(opponent, "Black Creature")

        val ride = driver.putCardInHand(player, "Take for a Ride")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)

        fun canCast(): Boolean {
            val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
            val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            return actions.any { (it.action as? CastSpell)?.cardId == ride }
        }

        // Without a crime committed: no instant-speed cast offered on the opponent's turn.
        if (onOpponentTurn) {
            canCast() shouldBe false
        }

        // Mark a crime committed this turn → conditional flash unlocks instant-speed casting.
        driver.replaceState(
            driver.state.copy(playersWhoCommittedCrimeThisTurn = setOf(player))
        )
        if (onOpponentTurn) {
            canCast() shouldBe true
        }
    }
})
