package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m10.cards.CaptainOfTheWatch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CaptainOfTheWatchScenarioTest : FunSpec({
    val soldier = card("Watch Test Soldier") {
        manaCost = "{W}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
    }
    val soldierArtifact = card("Watch Test Soldier Banner") {
        typeLine = "Kindred Artifact — Soldier"
    }

    test("only other Soldier creatures you control gain stats and vigilance") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(CaptainOfTheWatch, soldier, soldierArtifact))
            initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val captain = d.putCreatureOnBattlefield(d.player1, "Captain of the Watch")
        val yours = d.putCreatureOnBattlefield(d.player1, "Watch Test Soldier")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Watch Test Soldier")
        val otherType = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val noncreature = d.putPermanentOnBattlefield(d.player1, "Watch Test Soldier Banner")
        val projected = d.state.projectedState
        projected.getPower(captain) shouldBe 3
        projected.getToughness(captain) shouldBe 3
        projected.hasKeyword(captain, Keyword.VIGILANCE) shouldBe true
        projected.getPower(yours) shouldBe 2
        projected.getToughness(yours) shouldBe 2
        projected.hasKeyword(yours, Keyword.VIGILANCE) shouldBe true
        projected.getPower(theirs) shouldBe 1
        projected.getToughness(theirs) shouldBe 1
        projected.hasKeyword(theirs, Keyword.VIGILANCE) shouldBe false
        projected.getPower(otherType) shouldBe 2
        projected.getToughness(otherType) shouldBe 2
        projected.hasKeyword(otherType, Keyword.VIGILANCE) shouldBe false
        projected.hasKeyword(noncreature, Keyword.VIGILANCE) shouldBe false

        d.moveToGraveyard(captain)
        d.state.projectedState.getPower(yours) shouldBe 1
        d.state.projectedState.getToughness(yours) shouldBe 1
        d.state.projectedState.hasKeyword(yours, Keyword.VIGILANCE) shouldBe false
    }

    test("entering creates three white Soldiers that attack without tapping") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(CaptainOfTheWatch))
            initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val captain = d.putCardInHand(d.player1, "Captain of the Watch")
        d.giveMana(d.player1, Color.WHITE, 6)
        d.castSpell(d.player1, captain).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        val tokens = d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD)).filter {
            d.state.getEntity(it)?.has<TokenComponent>() == true
        }
        tokens.size shouldBe 3
        tokens.forEach {
            val card = d.state.getEntity(it)!!.get<CardComponent>()!!
            card.baseStats!!.basePower shouldBe 1
            card.baseStats!!.baseToughness shouldBe 1
            card.colors shouldBe setOf(Color.WHITE)
            d.state.projectedState.getPower(it) shouldBe 2
            d.state.projectedState.getToughness(it) shouldBe 2
            d.state.projectedState.hasSubtype(it, "Soldier") shouldBe true
            d.state.projectedState.hasKeyword(it, Keyword.VIGILANCE) shouldBe true
        }
        (tokens + captain).forEach { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, tokens + captain, d.player2).error shouldBe null
        (tokens + captain).forEach { d.isTapped(it) shouldBe false }
    }
})
