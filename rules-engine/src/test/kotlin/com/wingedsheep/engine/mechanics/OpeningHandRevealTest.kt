package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.dsl.revealFromOpeningHand
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Opening-hand reveal (CR 103.6b): "You may reveal this card from your opening hand. If you do, …".
 *
 * The reveal is offered in the same post-mulligan walk as a Leyline (CR 103.6 — starting player
 * first, then each other player in turn order). Its payoff here is a delayed trigger (CR 603.7a: a
 * static ability that lets a player take an action may create one) at "your first upkeep".
 */
class OpeningHandRevealTest : FunSpec({

    val revealer = card("Test Revealer") {
        manaCost = "{5}"
        typeLine = "Creature — Eldrazi"
        power = 5
        toughness = 5
        oracleText = "You may reveal this card from your opening hand. If you do, at the beginning of " +
            "your first upkeep, you gain 3 life."
        revealFromOpeningHand(
            Effects.CreateDelayedTrigger(
                step = Step.UPKEEP,
                fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                effect = Effects.GainLife(3)
            )
        )
    }

    val anyUpkeepRevealer = card("Test First-Upkeep Revealer") {
        manaCost = "{5}"
        typeLine = "Creature — Phyrexian"
        power = 5
        toughness = 5
        oracleText = "You may reveal this card from your opening hand. If you do, at the beginning of " +
            "the first upkeep, you gain 2 life."
        revealFromOpeningHand(Effects.CreateDelayedTrigger(step = Step.UPKEEP, effect = Effects.GainLife(2)))
    }

    val leyline = card("Test Leyline") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        mayBeginGameOnBattlefield()
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(revealer, anyUpkeepRevealer, leyline))
    }

    /** Swap every library copy of [name] into [playerId]'s hand, sending as many hand cards back. */
    fun GameTestDriver.moveToOpeningHand(playerId: EntityId, name: String) {
        var s = state
        val library = ZoneKey(playerId, Zone.LIBRARY)
        val hand = ZoneKey(playerId, Zone.HAND)
        val wanted = s.getLibrary(playerId).filter { s.getEntity(it)?.get<CardComponent>()?.name == name }
        val outgoing = s.getHand(playerId).filter { s.getEntity(it)?.get<CardComponent>()?.name != name }
            .take(wanted.size)
        for (id in wanted) s = s.removeFromZone(library, id).addToZone(hand, id)
        for (id in outgoing) s = s.removeFromZone(hand, id).addToZone(library, id)
        replaceState(s)
    }

    fun GameTestDriver.keepBoth() {
        submit(KeepHand(player1)).error shouldBe null
        submit(KeepHand(player2)).error shouldBe null
    }

    fun GameTestDriver.answerPending(choice: Boolean): YesNoDecision {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        submitYesNo(decision.playerId, choice).error shouldBe null
        return decision
    }

    fun GameTestDriver.handNames(playerId: EntityId) =
        state.getHand(playerId).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    test("revealing creates a delayed trigger that fires once, at your first upkeep") {
        val d = driver()
        d.initGame(Deck.of("Test Revealer" to 1, "Island" to 39), Deck.of("Island" to 40), skipMulligans = false)
        d.moveToOpeningHand(d.player1, "Test Revealer")
        d.keepBoth()

        val prompt = d.answerPending(true)
        prompt.playerId shouldBe d.player1
        prompt.prompt shouldBe "Reveal Test Revealer from your opening hand?"

        withClue("The walk ends and the card stays in hand (CR 103.6b)") {
            d.pendingDecision shouldBe null
            d.handNames(d.player1) shouldContain "Test Revealer"
        }
        val reveal = d.events.filterIsInstance<CardsRevealedEvent>().single()
        reveal.revealingPlayerId shouldBe d.player1
        reveal.cardNames shouldBe listOf("Test Revealer")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.getLifeTotal(d.player1) shouldBe 23

        // Only the first upkeep: by P1's next turn the delayed trigger is gone.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN) // P2's turn
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN) // P1's second turn
        d.activePlayer shouldBe d.player1
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("declining the reveal leaves no delayed trigger") {
        val d = driver()
        d.initGame(Deck.of("Test Revealer" to 1, "Island" to 39), Deck.of("Island" to 40), skipMulligans = false)
        d.moveToOpeningHand(d.player1, "Test Revealer")
        d.keepBoth()

        d.answerPending(false)
        d.pendingDecision shouldBe null
        d.events.filterIsInstance<CardsRevealedEvent>() shouldBe emptyList()
        d.state.delayedTriggers shouldBe emptyList()

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("each revealed copy creates its own delayed trigger") {
        val d = driver()
        d.initGame(Deck.of("Test Revealer" to 2, "Island" to 38), Deck.of("Island" to 40), skipMulligans = false)
        d.moveToOpeningHand(d.player1, "Test Revealer")
        d.keepBoth()

        d.answerPending(true)
        d.answerPending(true)
        d.pendingDecision shouldBe null

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.getLifeTotal(d.player1) shouldBe 26
    }

    test("'your first upkeep' waits for the revealer's own turn when they play second") {
        val d = driver()
        d.initGame(Deck.of("Island" to 40), Deck.of("Test Revealer" to 1, "Island" to 39), skipMulligans = false)
        d.moveToOpeningHand(d.player2, "Test Revealer")
        d.keepBoth()

        d.answerPending(true).playerId shouldBe d.player2

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("The starting player's upkeep is not P2's first upkeep") {
            d.activePlayer shouldBe d.player1
            d.getLifeTotal(d.player2) shouldBe 20
        }
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe d.player2
        d.getLifeTotal(d.player2) shouldBe 23
    }

    test("'the first upkeep' without a player gate fires on the starting player's upkeep") {
        val d = driver()
        d.initGame(Deck.of("Island" to 40), Deck.of("Test First-Upkeep Revealer" to 1, "Island" to 39), skipMulligans = false)
        d.moveToOpeningHand(d.player2, "Test First-Upkeep Revealer")
        d.keepBoth()

        d.answerPending(true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe d.player1
        d.getLifeTotal(d.player2) shouldBe 22
    }

    test("the starting player takes opening-hand actions first, reveals and leylines in one walk") {
        val d = driver()
        d.initGame(
            Deck.of("Test Leyline" to 1, "Island" to 39),
            Deck.of("Test Revealer" to 1, "Island" to 39),
            skipMulligans = false
        )
        d.moveToOpeningHand(d.player1, "Test Leyline")
        d.moveToOpeningHand(d.player2, "Test Revealer")
        d.keepBoth()

        val first = d.answerPending(true)
        first.playerId shouldBe d.player1
        first.prompt shouldBe "Begin the game with Test Leyline on the battlefield?"
        val second = d.answerPending(true)
        second.playerId shouldBe d.player2
        second.prompt shouldBe "Reveal Test Revealer from your opening hand?"

        d.pendingDecision shouldBe null
        d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))
            .mapNotNull { d.state.getEntity(it)?.get<CardComponent>()?.name } shouldBe listOf("Test Leyline")
        d.state.delayedTriggers.single().controllerId shouldBe d.player2
    }
})
