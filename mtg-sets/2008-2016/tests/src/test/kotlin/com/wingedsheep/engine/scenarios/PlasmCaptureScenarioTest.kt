package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.PlasmCapture
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Plasm Capture (DGM #91): "Counter target spell. At the beginning of your next first main phase,
 * add X mana in any combination of colors, where X is that spell's mana value."
 *
 * Pins that X is frozen when the delayed trigger is created — the countered spell is in the
 * graveyard long before the payoff — and paid out pip by pip in the colours the controller picks,
 * and that "your next first main phase" skips the same turn's second main phase (2013-04-15 ruling).
 */
class PlasmCaptureScenarioTest : FunSpec({

    /** Costs {5}: a mana value distinct from Plasm Capture's own 4 and from zero. */
    val boulder = card("Captured Boulder") { manaCost = "{5}"; typeLine = "Artifact"; oracleText = "" }

    val cards = TestCards.all + listOf(PlasmCapture, boulder)

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.capture(caster: EntityId, spell: EntityId) {
        val capture = putCardInHand(caster, "Plasm Capture")
        giveMana(caster, Color.GREEN, 2)
        giveMana(caster, Color.BLUE, 2)
        castSpellWithTargets(caster, capture, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        bothPass().error shouldBe null
        withClue("the target is countered") {
            state.getZone(ZoneKey(getOpponent(caster), Zone.GRAVEYARD)).contains(spell) shouldBe true
        }
    }

    /** Resolve the payoff trigger, answering each pip prompt with the next of [colors]. */
    fun GameTestDriver.payOut(player: EntityId, colors: List<Color>) {
        bothPass().error shouldBe null
        for (color in colors) {
            val decision = pendingDecision as? ChooseColorDecision
            withClue("a colour prompt for each of the X pips") { (decision != null) shouldBe true }
            submitDecision(player, ColorChosenResponse(decision!!.id, color)).error shouldBe null
        }
        withClue("no prompt beyond X pips") { (pendingDecision is ChooseColorDecision) shouldBe false }
    }

    test("cast on an opponent's turn, it adds X mana of the chosen colours in your next first main phase") {
        val d = driver()
        val boulderCard = d.putCardInHand(d.player1, "Captured Boulder")
        d.giveColorlessMana(d.player1, 5)
        d.castSpell(d.player1, boulderCard).error shouldBe null
        d.passPriority(d.player1).error shouldBe null
        d.capture(d.player2, boulderCard)

        withClue("the opponent's main phases are not the Capture controller's") {
            d.passPriorityUntil(Step.END)
            d.state.delayedTriggers.size shouldBe 1
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.activePlayerId shouldBe d.player2
        d.payOut(d.player2, listOf(Color.GREEN, Color.GREEN, Color.BLUE, Color.RED, Color.BLACK))

        withClue("five mana — the countered spell's mana value, not Plasm Capture's own 4") {
            val pool = d.pool(d.player2)
            pool.green shouldBe 2
            pool.blue shouldBe 1
            pool.red shouldBe 1
            pool.black shouldBe 1
            pool.white shouldBe 0
            pool.colorless shouldBe 0
        }
        d.state.delayedTriggers shouldBe emptyList()
    }

    test("cast in your first main phase, it skips that turn's second main phase and pays out next turn") {
        val d = driver()
        // Player 1 passes in their first main phase; player 2 casts into it, and player 1 captures it.
        d.passPriority(d.player1).error shouldBe null
        val inspiration = d.putCardInHand(d.player2, "Inspiration")
        d.giveMana(d.player2, Color.BLUE, 4)
        d.castSpellWithTargets(d.player2, inspiration, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.passPriority(d.player2).error shouldBe null
        d.capture(d.player1, inspiration)
        d.currentStep shouldBe Step.PRECOMBAT_MAIN

        withClue("the same turn's second main phase is not a first main phase") {
            d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            d.bothPass().error shouldBe null
            (d.pendingDecision is ChooseColorDecision) shouldBe false
            d.state.delayedTriggers.size shouldBe 1
        }
        withClue("nor is the opponent's first main phase") {
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.state.activePlayerId shouldBe d.player2
            d.state.delayedTriggers.size shouldBe 1
        }
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.activePlayerId shouldBe d.player1
        d.payOut(d.player1, listOf(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE))

        withClue("Inspiration's mana value of 4, all white as chosen") {
            d.pool(d.player1).white shouldBe 4
        }
        d.state.delayedTriggers shouldBe emptyList()
    }
})
