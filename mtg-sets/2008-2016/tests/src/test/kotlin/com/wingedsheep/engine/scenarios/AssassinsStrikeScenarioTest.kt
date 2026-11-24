package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.AssassinsStrike
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Assassin's Strike (RTR #57) — "Destroy target creature. Its controller discards a card."
 *
 * The discard belongs to the destroyed creature's last-known controller, and that player picks the
 * card from their own hand — never the caster, and not the owner of a stolen creature.
 */
class AssassinsStrikeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AssassinsStrike))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(6) { d.putLandOnBattlefield(d.activePlayer!!, "Swamp") }
        return d
    }

    /** Casts the Strike at [creature] and passes until its discard choice is pending. */
    fun GameTestDriver.strike(creature: EntityId): SelectCardsDecision {
        val caster = activePlayer!!
        castSpellWithTargets(caster, putCardInHand(caster, "Assassin's Strike"), listOf(ChosenTarget.Permanent(creature)))
        bothPass()
        return pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("the destroyed creature's controller discards a card of their choice") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val keep = d.putCardInHand(opponent, "Swamp")
        val toss = d.putCardInHand(opponent, "Forest")
        val opponentHand = d.getHand(opponent).toList()

        val select = d.strike(bears)

        withClue("the opponent, not the caster, makes the choice") { select.playerId shouldBe opponent }
        select.options shouldContainExactlyInAnyOrder opponentHand
        d.submitCardSelection(opponent, listOf(toss))

        d.getGraveyard(opponent) shouldContain bears
        d.getGraveyard(opponent) shouldContain toss
        d.getHand(opponent) shouldContain keep
        d.state.stack.isEmpty() shouldBe true
    }

    test("striking your own creature makes you discard") {
        val d = driver()
        val caster = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val card = d.putCardInHand(caster, "Forest")

        val select = d.strike(bears)

        select.playerId shouldBe caster
        d.submitCardSelection(caster, listOf(card))
        d.getGraveyard(caster) shouldContain card
    }

    test("a stolen creature's controller discards, not its owner") {
        val d = driver()
        val caster = d.activePlayer!!
        val opponent = d.getOpponent(caster)
        // The caster owns the creature; the opponent has taken control of it.
        val stolen = d.putCreatureOnBattlefield(caster, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(stolen) { it.with(ControllerComponent(opponent)) })
        val toss = d.putCardInHand(opponent, "Forest")
        val casterHandBefore = d.getHandSize(caster)

        val select = d.strike(stolen)

        withClue("last-known controller decides, from their own hand") {
            select.playerId shouldBe opponent
            select.options shouldContain toss
        }
        d.submitCardSelection(opponent, listOf(toss))

        d.getGraveyard(caster) shouldContain stolen
        d.getGraveyard(opponent) shouldContain toss
        withClue("the owner discards nothing") { d.getHandSize(caster) shouldBe casterHandBefore }
    }
})
