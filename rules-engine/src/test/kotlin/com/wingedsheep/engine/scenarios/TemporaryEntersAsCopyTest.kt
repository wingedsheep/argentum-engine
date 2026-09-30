package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtEndOfTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `EntersAsCopy(duration = Duration.EndOfTurn)` — "as this enters, you may have it become a copy of
 * any creature on the battlefield until end of turn" (Cursed Mirror). The copy lasts until cleanup
 * and then the permanent is its printed self again, on every entry path: a land played directly,
 * and a card put onto the battlefield by an effect. (The spell-resolution path is covered by
 * Cursed Mirror's own scenario test.)
 */
class TemporaryEntersAsCopyTest : FunSpec({
    val mimicLand = card("Temporary Mimic Land") {
        typeLine = "Land"
        replacementEffect(EntersAsCopy(additionalKeywords = listOf(Keyword.HASTE), duration = Duration.EndOfTurn))
    }
    val mimicCreature = card("Temporary Mimic Creature") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 1; toughness = 1
        replacementEffect(EntersAsCopy(duration = Duration.EndOfTurn))
    }
    val returnOne = card("Temporary Mimic Return") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(t)
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(mimicLand, mimicCreature, returnOne))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    test("a land played as a temporary copy is the creature until cleanup, then the land again") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val land = d.putCardInHand(d.player1, mimicLand.name)
        d.playLand(d.player1, land).error shouldBe null
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null

        d.name(land) shouldBe "Grizzly Bears"
        d.state.projectedState.isCreature(land) shouldBe true
        d.state.projectedState.hasKeyword(land, Keyword.HASTE) shouldBe true

        d.passPriorityUntil(Step.UPKEEP)
        d.name(land) shouldBe mimicLand.name
        d.state.projectedState.isCreature(land) shouldBe false
        d.state.projectedState.hasKeyword(land, Keyword.HASTE) shouldBe false
        d.state.getEntity(land)!!.has<CopyOfComponent>() shouldBe false
        d.state.getEntity(land)!!.has<RevertCopyAtEndOfTurnComponent>() shouldBe false
    }

    test("a card reanimated as a temporary copy reverts at cleanup") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val mimic = d.putCardInGraveyard(d.player1, mimicCreature.name)
        val spell = d.putCardInHand(d.player1, returnOne.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(mimic, d.player1, Zone.GRAVEYARD)))
            .error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null

        d.name(mimic) shouldBe "Grizzly Bears"
        d.state.projectedState.getPower(mimic) shouldBe 2

        d.passPriorityUntil(Step.UPKEEP)
        d.name(mimic) shouldBe mimicCreature.name
        d.state.projectedState.getPower(mimic) shouldBe 1
    }

    test("a temporary copy that leaves the battlefield drops its revert marker with the copy") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val land = d.putCardInHand(d.player1, mimicLand.name)
        d.playLand(d.player1, land).error shouldBe null
        d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null
        d.state.getEntity(land)!!.has<RevertCopyAtEndOfTurnComponent>() shouldBe true

        d.replaceState(d.zones.moveToZone(d.state, land, Zone.HAND).state)
        d.name(land) shouldBe mimicLand.name
        d.state.getEntity(land)!!.has<RevertCopyAtEndOfTurnComponent>() shouldBe false
    }

    test("durations other than permanent and end of turn are rejected at definition time") {
        shouldThrow<IllegalArgumentException> { EntersAsCopy(duration = Duration.UntilYourNextTurn) }
    }
})
