package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.TheDeckOfManyThings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TheDeckOfManyThingsScenarioTest : FunSpec({
    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    fun setup(handSize: Int): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(TheDeckOfManyThings))
            it.initMirrorMatch(Deck.of("Island" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        d.getHand(player).drop(handSize).forEach { d.moveToGraveyard(it) }
        d.getHandSize(player) shouldBe handSize
        return d to player
    }

    /** Activate the Deck, force the d20's natural face, and resolve; returns the resolution result. */
    fun roll(d: GameTestDriver, player: EntityId, natural: Int): ExecutionResult {
        val deck = d.putPermanentOnBattlefield(player, "The Deck of Many Things")
        d.giveColorlessMana(player, 2)
        d.submit(ActivateAbility(player, deck, TheDeckOfManyThings.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        d.replaceState(d.state.copy(rng = GameRng.seeded(seedFor(natural))))
        return d.bothPass()
    }

    test("the hand size is subtracted, and a result of 0 or less discards the hand") {
        val (d, player) = setup(handSize = 7)
        val result = roll(d, player, natural = 5)
        val die = result.events.filterIsInstance<DieRolledEvent>().single()
        die.natural shouldBe 5
        die.result shouldBe -2
        d.getHandSize(player) shouldBe 0
    }

    test("1-9 returns a card at random from your graveyard") {
        val (d, player) = setup(handSize = 0)
        val graveyard = d.getGraveyard(player).size
        roll(d, player, natural = 6).events.filterIsInstance<DieRolledEvent>().single().result shouldBe 6
        d.getHandSize(player) shouldBe 1
        d.getGraveyard(player).size shouldBe graveyard - 1
    }

    test("10-19 reads the modified result and draws two cards") {
        val (d, player) = setup(handSize = 2)
        val die = roll(d, player, natural = 14).events.filterIsInstance<DieRolledEvent>().single()
        die.result shouldBe 12
        d.getHandSize(player) shouldBe 4
    }

    test("a natural 20 reduced by the hand misses the top row") {
        val (d, player) = setup(handSize = 1)
        roll(d, player, natural = 20)
        // Result 19: draw two, nothing reanimated.
        d.getHandSize(player) shouldBe 3
    }

    test("20 reanimates a creature from any graveyard, and its owner loses when it dies") {
        val (d, player) = setup(handSize = 0)
        val opponent = d.getOpponent(player)
        val courser = d.putCardInGraveyard(opponent, "Centaur Courser")
        roll(d, player, natural = 20)
        if (d.state.pendingDecision != null) d.submitCardSelection(player, listOf(courser))
        (courser in d.getPermanents(player)) shouldBe true

        val blade = d.putCardInHand(player, "Doom Blade")
        d.giveMana(player, Color.BLACK, 1)
        d.giveColorlessMana(player, 1)
        d.castSpell(player, blade, listOf(courser))
        d.bothPass()
        // The delayed trigger goes on the stack and resolves: the courser's owner loses.
        if (!d.state.gameOver) d.bothPass()
        d.assertGameOver(expectedWinner = player)
    }
})
