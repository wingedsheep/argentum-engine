package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.informedChoiceDecisions], one test per misplay from the 2026-10-09 AI-vs-AI log
 * review. Each case runs [AiProfile.PRODUCTION] beside [AiProfile.PRODUCTION_CHOICES] on the same
 * position, so the test records the old answer as well as asserting the new one — if the old one
 * ever stops being wrong, the position no longer reproduces the misplay and needs rebuilding.
 */
class InformedChoiceDecisionsTest : FunSpec({

    val registry = CardRegistry().apply { register(TestCards.all) }

    fun driver(deck1: Deck, deck2: Deck): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initGame(deck1, deck2, skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun ai(profile: AiProfile, playerId: EntityId) = AIPlayer.create(registry, playerId, profile)

    /** Pass priority until [playerId] is asked a [T]; fails rather than looping if it never is. */
    fun <T : PendingDecision> GameTestDriver.passUntilDecision(playerId: EntityId, type: Class<T>): T {
        repeat(40) {
            val pending = state.pendingDecision
            if (pending != null && pending.playerId == playerId && type.isInstance(pending)) return type.cast(pending)
            if (pending != null) autoResolveDecision() else bothPass()
        }
        error("${type.simpleName} for $playerId never arrived")
    }

    test("game 4: a land that taps for the chosen colour picks one the deck needs, not white") {
        // R/G: green and red pips in hand and library, nothing white anywhere.
        val d = driver(Deck.of("Mountain" to 20, "Forest" to 20), Deck.of("Plains" to 40))
        val p1 = d.player1
        d.putCardInHand(p1, "Grizzly Bears")
        d.putCardInHand(p1, "Raging Goblin")
        d.putCardInHand(p1, "Lava Axe")
        val room = d.putCardInHand(p1, "Room of Refuge")
        d.submit(PlayLand(p1, room))
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()

        val before = ai(AiProfile.PRODUCTION, p1).respondToDecision(d.state, decision) as ColorChosenResponse
        withClue("the position must still reproduce the misplay") { before.color shouldBe Color.WHITE }

        val after = ai(AiProfile.PRODUCTION_CHOICES, p1).respondToDecision(d.state, decision) as ColorChosenResponse
        // Lava Axe's {R} plus Raging Goblin's outweigh Grizzly Bears' {G} with no basics out yet.
        after.color shouldBe Color.RED
    }

    test("game 3: a landwalk grant names a land type the defending player controls") {
        val d = driver(Deck.of("Island" to 40), Deck.of("Forest" to 20, "Swamp" to 20))
        val p1 = d.player1
        val p2 = d.player2
        repeat(3) { d.putLandOnBattlefield(p1, "Island") }
        d.putLandOnBattlefield(p2, "Forest")
        d.putLandOnBattlefield(p2, "Swamp")
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val cloak = d.putCardInHand(p1, "Traveler's Cloak")
        d.castSpell(p1, cloak, listOf(bears))
        val decision = d.passUntilDecision(p1, ChooseOptionDecision::class.java)
        decision.prompt shouldBe "Choose a basic land type"

        val before = ai(AiProfile.PRODUCTION, p1).respondToDecision(d.state, decision) as OptionChosenResponse
        withClue("the position must still reproduce the misplay") {
            decision.options[before.optionIndex] shouldNotBeIn listOf("Swamp", "Forest")
        }

        val after = ai(AiProfile.PRODUCTION_CHOICES, p1).respondToDecision(d.state, decision) as OptionChosenResponse
        decision.options[after.optionIndex] shouldBeIn listOf("Swamp", "Forest")
    }

    test("game 9: Lammastide Weave names a card the targeted opponent is made of, not the first name") {
        val d = driver(Deck.of("Forest" to 40), Deck.of("Swamp" to 24, "Hill Giant" to 16))
        val p1 = d.player1
        val p2 = d.player2
        repeat(2) { d.putLandOnBattlefield(p1, "Forest") }
        // A land on top, so the mill gains nothing whatever is named. The old answer simulated every
        // name against the real library; with a Hill Giant on top that peek would find it, and the
        // position would stop reproducing the tie that sent the log's Weave to the first name.
        d.putCardOnTopOfLibrary(p2, "Swamp")
        val weave = d.putCardInHand(p1, "Lammastide Weave")
        d.castSpell(p1, weave, listOf(p2))
        val decision = d.passUntilDecision(p1, ChooseOptionDecision::class.java)

        val before = ai(AiProfile.PRODUCTION, p1).respondToDecision(d.state, decision) as OptionChosenResponse
        withClue("the position must still reproduce the misplay") { before.optionIndex shouldBe 0 }

        val after = ai(AiProfile.PRODUCTION_CHOICES, p1).respondToDecision(d.state, decision) as OptionChosenResponse
        // Hill Giant: sixteen copies at mana value 4 — the name a mill most likely hits for the most life.
        decision.options[after.optionIndex] shouldBe "Hill Giant"
    }

    // Fasting: "If you would begin your draw step, you may skip that step instead. If you do, you
    // gain 2 life." Asked in the upkeep, so a one-step simulation never reaches the draw it gives up.
    fun fastingUpkeep(library: Deck): Pair<GameTestDriver, YesNoDecision> {
        val d = driver(library, Deck.of("Island" to 40))
        val p1 = d.player1
        repeat(2) { d.putLandOnBattlefield(p1, "Plains") }
        d.putPermanentOnBattlefield(p1, "Fasting")
        // Stuck on two lands with only four-drops in hand: exactly the hand that needs the draw.
        // The opening seven go back to the library first — a land already in hand would take
        // this turn's drop and leave the draw worth only its place on the hand curve.
        var cleared = d.state
        for (card in cleared.getZone(p1, Zone.HAND)) {
            cleared = cleared.moveToZone(card, ZoneKey(p1, Zone.HAND), ZoneKey(p1, Zone.LIBRARY))
        }
        d.replaceState(cleared)
        repeat(3) { d.putCardInHand(p1, "Hill Giant") }
        d.passPriorityUntil(Step.UPKEEP, activePlayer = p1)
        return d to d.passUntilDecision(p1, YesNoDecision::class.java)
    }

    test("game 7: Fasting does not skip the draw a land-light hand is waiting on") {
        val (d, decision) = fastingUpkeep(Deck.of("Plains" to 40))
        val p1 = d.player1

        val before = ai(AiProfile.PRODUCTION, p1).respondToDecision(d.state, decision) as YesNoResponse
        withClue("the position must still reproduce the misplay") { before.choice shouldBe true }

        val after = ai(AiProfile.PRODUCTION_CHOICES, p1).respondToDecision(d.state, decision) as YesNoResponse
        after.choice shouldBe false
    }

    test("Fasting control: with no library left the skipped draw costs nothing, so take the life") {
        val (d, decision) = fastingUpkeep(Deck.of("Plains" to 40))
        val p1 = d.player1
        var state = d.state
        for (card in state.getZone(p1, Zone.LIBRARY)) {
            state = state.moveToZone(card, ZoneKey(p1, Zone.LIBRARY), ZoneKey(p1, Zone.EXILE))
        }

        val after = ai(AiProfile.PRODUCTION_CHOICES, p1).respondToDecision(state, decision) as YesNoResponse
        after.choice shouldBe true
    }
})
