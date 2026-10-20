package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.IchneumonDruid
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ichneumon Druid — {1}{G}{G} 1/1 (Legends #191)
 *
 * "Whenever an opponent casts an instant spell other than the first instant spell that player
 * casts each turn, this creature deals 4 damage to that player."
 *
 * The trigger restriction reads the *caster's* own instant history, so the cases that matter are
 * the ones a table-wide or Nth-exactly count would get wrong: the opponent's first instant is free,
 * their second and third each cost 4, and another player's instants never use up the opponent's
 * free one (the 2004 ruling: the count is tracked per player).
 */
class IchneumonDruidScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + IchneumonDruid)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Ichneumon Druid")
        return d
    }

    fun GameTestDriver.boltInHand(playerId: EntityId): EntityId {
        giveMana(playerId, Color.RED, 1)
        return putCardInHand(playerId, "Lightning Bolt")
    }

    /** The opponent casts a Bolt at player1 during player1's main phase and everything resolves. */
    fun GameTestDriver.opponentBolts(opponent: EntityId) {
        passPriority(player1)
        val bolt = boltInHand(opponent)
        castSpell(opponent, bolt, targets = listOf(player1)).error shouldBe null
        // Resolve the Druid's trigger (if any) and then the Bolt.
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("the opponent's first instant each turn is free; the second and third each deal 4 to them") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)

        d.opponentBolts(opponent)
        withClue("first instant: no trigger") {
            d.getLifeTotal(opponent) shouldBe 20
            d.getLifeTotal(d.player1) shouldBe 17
        }

        d.opponentBolts(opponent)
        withClue("second instant: the Druid deals 4 to its caster") {
            d.getLifeTotal(opponent) shouldBe 16
            d.getLifeTotal(d.player1) shouldBe 14
        }

        d.opponentBolts(opponent)
        withClue("third instant: triggers again") {
            d.getLifeTotal(opponent) shouldBe 12
            d.getLifeTotal(d.player1) shouldBe 11
        }
    }

    test("the count is per player: the controller's own instants neither trigger nor use up the opponent's free one") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)

        repeat(2) {
            val bolt = d.boltInHand(d.player1)
            d.castSpell(d.player1, bolt, targets = listOf(opponent)).error shouldBe null
            d.bothPass()
        }
        withClue("the controller's own instants never trigger it") {
            d.getLifeTotal(opponent) shouldBe 14
        }

        d.opponentBolts(opponent)
        withClue("the opponent's first instant of the turn is still free") {
            d.getLifeTotal(opponent) shouldBe 14
            d.getLifeTotal(d.player1) shouldBe 17
        }
    }
})
