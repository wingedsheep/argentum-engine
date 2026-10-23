package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.TheDeckOfManyThings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TheDeckOfManyThingsScenarioTest : FunSpec({
    val bounce = card("Test Deck Bounce") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val creature = target(TargetFilter.Creature)
            effect = Effects.Move(creature, Zone.HAND)
        }
    }

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    fun setup(handSize: Int): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(TheDeckOfManyThings, bounce))
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
        val die = roll(d, player, natural = 20).events.filterIsInstance<DieRolledEvent>().single()
        die.natural shouldBe 20
        die.result shouldBe 19
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

    test("a reanimated creature that leaves and comes back is a new object: its death costs nothing") {
        val (d, player) = setup(handSize = 0)
        val opponent = d.getOpponent(player)
        val courser = d.putCardInGraveyard(opponent, "Centaur Courser")
        roll(d, player, natural = 20)
        if (d.state.pendingDecision != null) d.submitCardSelection(player, listOf(courser))
        (courser in d.getPermanents(player)) shouldBe true

        // Bounced to its owner's hand, then cast by that owner: a new object (CR 400.7).
        val bouncer = d.putPermanentOnBattlefield(player, bounce.name)
        d.submit(ActivateAbility(player, bouncer, bounce.script.activatedAbilities.single().id, listOf(ChosenTarget.Permanent(courser)))).outcome shouldBe Outcome.Done
        d.bothPass()
        (courser in d.getHand(opponent)) shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        d.giveMana(opponent, Color.GREEN, 1)
        d.giveColorlessMana(opponent, 2)
        d.castSpell(opponent, courser, emptyList())
        d.bothPass()
        (courser in d.getPermanents(opponent)) shouldBe true

        val blade = d.putCardInHand(opponent, "Doom Blade")
        d.giveMana(opponent, Color.BLACK, 1)
        d.giveColorlessMana(opponent, 1)
        d.castSpell(opponent, blade, listOf(courser))
        d.bothPass()
        d.state.stack.size shouldBe 0
        d.state.gameOver shouldBe false
    }
})
