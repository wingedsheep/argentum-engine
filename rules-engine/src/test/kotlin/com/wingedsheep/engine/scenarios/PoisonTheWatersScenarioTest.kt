package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.PoisonTheWaters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Poison the Waters — {1}{B} Sorcery, "Choose one —"
 *   • All creatures get -1/-1 until end of turn.
 *   • Target player reveals their hand. You choose an artifact or creature card from it.
 *     That player discards that card.
 *
 * Mode 0 is the Infest-style group debuff; mode 1 is the Despise-style reveal-and-discard
 * filtered to artifact-or-creature cards.
 */
class PoisonTheWatersScenarioTest : FunSpec({

    val projector = StateProjector()

    // A vanilla 1/1 that dies to -1/-1.
    val Squire = CardDefinition.creature("Test Squire", ManaCost.parse("{W}"), setOf(Subtype("Human")), 1, 1)

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(PoisonTheWaters)
        d.registerCard(Squire)
        return d
    }

    fun setup(d: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        d.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(p1, 1)
        d.giveMana(p1, Color.BLACK, 1)
        return p1 to d.getOpponent(p1)
    }

    test("mode 0 — all creatures get -1/-1: a 1/1 dies, a 2/2 shrinks to 1/1") {
        val d = driver()
        val (p1, p2) = setup(d)

        d.putCreatureOnBattlefield(p2, "Test Squire") // 1/1 → dies
        d.putCreatureOnBattlefield(p1, "Grizzly Bears") // 2/2 → 1/1
        val bears = d.getCreatures(p1).first()

        val spell = d.putCardInHand(p1, "Poison the Waters")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = emptyList(),
            chosenModes = listOf(0),
            modeTargetsOrdered = emptyList()
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")
        d.bothPass()

        d.findPermanent(p2, "Test Squire").shouldBeNull()
        d.getGraveyardCardNames(p2) shouldContain "Test Squire"

        d.findPermanent(p1, "Grizzly Bears").shouldNotBeNull()
        projector.getProjectedPower(d.state, bears) shouldBe 1
        projector.getProjectedToughness(d.state, bears) shouldBe 1
    }

    test("mode 1 — target player reveals hand; you discard a chosen artifact or creature card") {
        val d = driver()
        val (p1, p2) = setup(d)

        // Opponent's hand: a creature (selectable), a land (not selectable).
        d.putCardInHand(p2, "Grizzly Bears")
        d.putCardInHand(p2, "Forest")

        val spell = d.putCardInHand(p1, "Poison the Waters")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = listOf(ChosenTarget.Player(p2)),
            chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(p2)))
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")
        d.bothPass() // resolve → reveal + select decision

        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val bears = d.getHand(p2).first {
            d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Grizzly Bears"
        }
        val forest = d.getHand(p2).first {
            d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest"
        }
        // Only the creature is selectable; the land is shown but not selectable.
        select.options shouldContainExactlyInAnyOrder listOf(bears)
        select.nonSelectableOptions shouldContain forest

        d.submitCardSelection(p1, listOf(bears))

        // The chosen creature is discarded to the opponent's graveyard.
        d.getGraveyardCardNames(p2) shouldContain "Grizzly Bears"
        // The land stays in hand.
        d.getHand(p2).any {
            d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest"
        } shouldBe true
    }
})
