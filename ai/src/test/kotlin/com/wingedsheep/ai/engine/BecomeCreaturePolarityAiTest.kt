package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.EmbodimentOfFury
import com.wingedsheep.mtg.sets.definitions.ogw.cards.EmbodimentOfInsight
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A targeted "becomes an N/N creature" (Embodiment of Fury's landfall animate) hands the target's
 * controller a body for the turn, so the AI reads it as a gift and aims a sizeable one at its own
 * side. A tiny animate, or one on a slot that can already hold a creature, stays open to both sides.
 */
class BecomeCreaturePolarityAiTest : FunSpec({

    fun animator(power: Int, filter: TargetFilter = TargetFilter.Land) = card("Animator fixture $power ${filter.description}") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Elemental"
        this.power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
            val land = target(filter)
            effect = Effects.BecomeCreature(
                target = land,
                power = power,
                toughness = power,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
            )
        }
    }

    val big = animator(3)
    val small = animator(1)
    val onCreature = animator(4, TargetFilter.Creature)
    val registry = CardRegistry().apply { register(TestCards.all + listOf(big, small, EmbodimentOfFury, EmbodimentOfInsight)) }

    test("a sizeable land animate is a gift; a tiny one, or one on a creature, stays flexible") {
        fun read(card: com.wingedsheep.sdk.model.CardDefinition): List<TargetPolarity> {
            val trigger = card.script.triggeredAbilities.single()
            return TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements, source = card)
        }
        read(big) shouldBe listOf(TargetPolarity.BENEFICIAL)
        read(small) shouldBe listOf(TargetPolarity.FLEXIBLE)
        // On a creature it is a base-P/T set: it shrinks a big one as readily as it grows a small one.
        read(onCreature) shouldBe listOf(TargetPolarity.FLEXIBLE)
        AiReadingAudit.read(EmbodimentOfFury).readFully shouldBe true
        AiReadingAudit.read(EmbodimentOfInsight).readFully shouldBe true
    }

    test("the landfall animate goes on our own land, not the opponent's") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(big))
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        d.putCreatureOnBattlefield(d.player1, big.name)
        d.putLandOnBattlefield(d.player1, "Forest")
        d.putLandOnBattlefield(d.player2, "Forest")
        d.putLandOnBattlefield(d.player2, "Forest")
        val drop = d.putCardInHand(d.player1, "Forest")
        d.playLand(d.player1, drop)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val response = AIPlayer.create(registry, d.player1, AiProfile.LIVE).respondToDecision(d.state, decision)
        val target = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten().single()
        d.state.projectedState.getController(target) shouldBe d.player1
    }
})
