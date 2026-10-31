package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.DrannithMagistrate
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ThinkTwice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Drannith Magistrate (IKO #11) — {1}{W} Creature — Human Wizard, 1/3.
 *
 * "Your opponents can't cast spells from anywhere other than their hands."
 *
 * The resolution-time half (cascade, discover) is pinned by the engine's
 * `ResolutionCastProhibitionScenarioTest`.
 */
class DrannithMagistrateScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(DrannithMagistrate, ThinkTwice))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castableCards(player: EntityId): Set<EntityId> =
        legalActions(player).filter { it.affordable }.mapNotNull { (it.action as? CastSpell)?.cardId }.toSet()

    test("an opponent can't flashback a spell from their graveyard, but still casts from hand") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.giveMana(me, Color.BLUE, 4)
        val flashback = driver.putCardInGraveyard(me, "Think Twice")
        val fromHand = driver.putCardInHand(me, "Think Twice")

        val flashbackCast = driver.legalActions(me).map { it.action }
            .filterIsInstance<CastSpell>().first { it.cardId == flashback }

        val magistrate = driver.putCreatureOnBattlefield(opponent, "Drannith Magistrate")
        val locked = driver.castableCards(me)
        locked shouldNotContain flashback
        locked shouldContain fromHand
        // The handler refuses it too, not only the legal-action list.
        driver.submitExpectFailure(flashbackCast)

        driver.moveToGraveyard(magistrate)
        driver.castableCards(me) shouldContain flashback
    }

    test("the Magistrate's controller can still cast from their graveyard") {
        val driver = newDriver()
        val me = driver.player1
        driver.giveMana(me, Color.BLUE, 4)
        val flashback = driver.putCardInGraveyard(me, "Think Twice")
        driver.putCreatureOnBattlefield(me, "Drannith Magistrate")

        driver.castableCards(me) shouldContain flashback
    }
})
