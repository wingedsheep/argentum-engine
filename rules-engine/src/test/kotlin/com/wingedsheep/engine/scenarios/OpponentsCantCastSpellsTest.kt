package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.VoiceOfVictory
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for the [com.wingedsheep.sdk.scripting.OpponentsCantCastSpells] static ability — an
 * opponent-scoped continuous cast prohibition (Tarkir: Dragonstorm Tier-3 gap #19).
 *
 * Voice of Victory ({1}{W} 1/3): "Your opponents can't cast spells during your turn." Modeled as
 * `OpponentsCantCastSpells(onlyDuringYourTurn = true)`, read at cast-legality time and OR'd into
 * the central `cantCastSpells` gate so it covers every casting zone.
 *
 * The probe spell is Lightning Bolt (an instant targeting any target). Using an instant avoids the
 * sorcery-speed timing confound: a creature/sorcery already wouldn't enumerate on an opponent's
 * turn for unrelated reasons. `LegalActionEnumerator.enumerate` returns actions regardless of
 * priority or affordability, so presence/absence of the CastSpell action isolates the restriction.
 */
class OpponentsCantCastSpellsTest : FunSpec({

    fun createDriver(startingPlayer: Int = 0): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VoiceOfVictory))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, startingPlayer = startingPlayer)
        return driver
    }

    fun GameTestDriver.boltActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> {
        val enumerator = LegalActionEnumerator.create(cardRegistry)
        return enumerator.enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }
    }

    test("opponents can't cast spells during the controller's turn") {
        val driver = createDriver(startingPlayer = 0)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.putCreatureOnBattlefield(controller, "Voice of Victory")
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        driver.boltActionsFor(opponent, bolt) shouldHaveSize 0
    }

    test("the controller can still cast their own spells during their turn") {
        val driver = createDriver(startingPlayer = 0)
        val controller = driver.activePlayer!!

        driver.putCreatureOnBattlefield(controller, "Voice of Victory")
        val bolt = driver.putCardInHand(controller, "Lightning Bolt")
        driver.giveMana(controller, Color.RED, 1)

        driver.boltActionsFor(controller, bolt).isNotEmpty() shouldBe true
    }

    test("opponents can cast spells on their own turn (the restriction is your-turn-only)") {
        // player2 is the active player; Voice of Victory's controller (player1) is not active,
        // so the onlyDuringYourTurn restriction does not apply.
        val driver = createDriver(startingPlayer = 1)
        val activeOpponent = driver.activePlayer!!
        val voiceController = driver.getOpponent(activeOpponent)

        driver.putCreatureOnBattlefield(voiceController, "Voice of Victory")
        val bolt = driver.putCardInHand(activeOpponent, "Lightning Bolt")
        driver.giveMana(activeOpponent, Color.RED, 1)

        driver.boltActionsFor(activeOpponent, bolt).isNotEmpty() shouldBe true
    }

    test("the handler rejects an opponent's cast attempt during the controller's turn") {
        val driver = createDriver(startingPlayer = 0)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.putCreatureOnBattlefield(controller, "Voice of Victory")
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        // Hand priority to the opponent so the only barrier to casting is the static restriction.
        driver.passPriority(controller)

        driver.submitExpectFailure(
            CastSpell(opponent, bolt, targets = listOf(ChosenTarget.Player(controller)))
        )
    }
})
