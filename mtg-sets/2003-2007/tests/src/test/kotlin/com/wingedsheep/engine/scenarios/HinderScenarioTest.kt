package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.Hinder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hinder (CHK #65) — "Counter target spell. If that spell is countered this way, put that card on
 * your choice of the top or bottom of its owner's library instead of into that player's graveyard."
 *
 * The victim is the active player casting a creature; Hinder's controller responds and chooses.
 */
class HinderScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Hinder)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.hinderCourser(): Pair<EntityId, ChooseOptionDecision> {
        val victim = player1
        val caster = player2
        giveMana(victim, Color.GREEN, 3)
        val courser = putCardInHand(victim, "Centaur Courser")
        castSpell(victim, courser).outcome shouldBe Outcome.Done
        passPriority(victim)
        giveMana(caster, Color.BLUE, 3)
        val hinder = putCardInHand(caster, "Hinder")
        val cast = castSpellWithTargets(caster, hinder, listOf(ChosenTarget.Spell(courser)))
        withClue(cast.error ?: "casting Hinder failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("Hinder's controller chooses, not the spell's owner") { decision.playerId shouldBe caster }
        return courser to decision
    }

    test("Hinder counters a spell onto the bottom of its owner's library") {
        val d = driver()
        val (courser, decision) = d.hinderCourser()
        d.submitDecision(d.player2, OptionChosenResponse(decision.id, decision.options.indexOf(LibraryChoicePosition.Bottom.label)))

        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).last() shouldBe courser
        d.getGraveyardCardNames(d.player1).contains("Centaur Courser") shouldBe false
        d.findPermanent(d.player1, "Centaur Courser") shouldBe null
        d.getGraveyardCardNames(d.player2).contains("Hinder") shouldBe true
    }

    test("Hinder can put the countered card on top instead") {
        val d = driver()
        val (courser, decision) = d.hinderCourser()
        d.submitDecision(d.player2, OptionChosenResponse(decision.id, decision.options.indexOf(LibraryChoicePosition.Top.label)))

        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe courser
    }
})
