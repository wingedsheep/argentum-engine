package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.effects.permanent.types.flipDfcInPlace
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EffectCopyEntryDfcTest : FunSpec({
    test("effect-driven copy of a transforming creature stays single-faced and restores its own identity") {
        val copier = card("Single Faced Entry Copier") {
            manaCost = "{U}"
            typeLine = "Creature — Shapeshifter"
            power = 1
            toughness = 1
            replacementEffect(EntersAsCopy())
        }
        val front = card("Copy Subject Front") {
            manaCost = "{G}"
            typeLine = "Creature — Human"
            power = 2
            toughness = 2
        }
        val back = card("Copy Subject Back") {
            typeLine = "Creature — Werewolf"
            power = 5
            toughness = 5
        }
        val subject = CardDefinition.doubleFacedCreature(front, back)
        val returnSpell = card("Return Single Faced Copier") {
            manaCost = "{U}"
            typeLine = "Sorcery"
            spell {
                val t = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.PutOntoBattlefieldFromGraveyard(t)
            }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(copier, subject, back, returnSpell))
        d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val target = d.putPermanentOnBattlefield(d.player2, subject.name)
        val id = d.putCardInGraveyard(d.player1, copier.name)
        val spell = d.putCardInHand(d.player1, returnSpell.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, spell,
            listOf(ChosenTarget.Card(id, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(target)).error shouldBe null
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe subject.name
        d.state.getEntity(id)!!.has<DoubleFacedComponent>() shouldBe false
        flipDfcInPlace(d.state, d.cardRegistry, id) shouldBe null
        d.replaceState(d.zones.moveToZone(d.state, id, Zone.HAND).state)
        d.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe copier.name
    }
})
