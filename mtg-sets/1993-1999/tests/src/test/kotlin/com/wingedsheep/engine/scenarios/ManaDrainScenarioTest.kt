package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.ManaDrain
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
 * Mana Drain (LEG #65): "Counter target spell. At the beginning of your next main phase, add an
 * amount of {C} equal to that spell's mana value."
 *
 * Pins the two halves of "your next main phase" from the 2020-11-10 ruling: cast on an opponent's
 * turn it pays out in your precombat main phase; cast in your own precombat main phase it pays out
 * that same turn's postcombat main phase, not next turn's.
 */
class ManaDrainScenarioTest : FunSpec({

    /** Costs {5}: a mana value distinct from Mana Drain's own 2 and from zero. */
    val boulder = card("Drained Boulder") { manaCost = "{5}"; typeLine = "Artifact"; oracleText = "" }

    // Built during spec construction: `TestCards.all` scans the whole card corpus.
    val cards = TestCards.all + listOf(ManaDrain, boulder)

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.colorlessInPool(player: EntityId): Int =
        state.getEntity(player)?.get<ManaPoolComponent>()?.colorless ?: 0

    fun GameTestDriver.drain(caster: EntityId, spell: EntityId) {
        val drain = putCardInHand(caster, "Mana Drain")
        giveMana(caster, Color.BLUE, 2)
        castSpellWithTargets(caster, drain, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        bothPass().error shouldBe null
        withClue("the target is countered") {
            state.getZone(ZoneKey(getOpponent(caster), Zone.GRAVEYARD)).contains(spell) shouldBe true
        }
        withClue("the payoff waits for a main phase — nothing is added while Mana Drain resolves") {
            colorlessInPool(caster) shouldBe 0
        }
    }

    test("cast on an opponent's turn, it adds {C} equal to the mana value in your next precombat main phase") {
        val d = driver()
        val boulderCard = d.putCardInHand(d.player1, "Drained Boulder")
        d.giveColorlessMana(d.player1, 5)
        d.castSpell(d.player1, boulderCard).error shouldBe null
        d.passPriority(d.player1).error shouldBe null
        d.drain(d.player2, boulderCard)

        withClue("the opponent's postcombat main phase is not the Drain controller's main phase") {
            d.passPriorityUntil(Step.END)
            d.state.delayedTriggers.size shouldBe 1
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.activePlayerId shouldBe d.player2
        d.bothPass().error shouldBe null

        withClue("{C} equal to the countered spell's mana value — 5, not Mana Drain's own 2") {
            d.colorlessInPool(d.player2) shouldBe 5
        }
    }

    test("cast in your precombat main phase, it pays out in that turn's postcombat main phase") {
        val d = driver()
        // Player 1 passes in their main phase; player 2 casts an instant into it, and player 1 drains it.
        d.passPriority(d.player1).error shouldBe null
        val inspiration = d.putCardInHand(d.player2, "Inspiration")
        d.giveMana(d.player2, Color.BLUE, 4)
        d.castSpellWithTargets(d.player2, inspiration, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.passPriority(d.player2).error shouldBe null
        d.drain(d.player1, inspiration)
        d.currentStep shouldBe Step.PRECOMBAT_MAIN

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.state.activePlayerId shouldBe d.player1
        d.bothPass().error shouldBe null

        withClue("Inspiration's mana value of 4, in the same turn's postcombat main phase") {
            d.colorlessInPool(d.player1) shouldBe 4
        }
        d.state.delayedTriggers shouldBe emptyList()
    }
})
