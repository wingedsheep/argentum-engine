package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Awaken (CR 702.113) reaches the AI as a `CastWithAlternativeCost` offer whose last target slot is
 * the awaken land. With the mana for it, the AI should awaken — the removal still aimed at the
 * opponent's creature, the +1/+1 counters at its own land.
 */
class AwakenAiTest : FunSpec({

    val path = card("Awaken AI Test Path") {
        manaCost = "{1}{B}{B}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.awaken(4, "{5}{B}{B}"))
        spell {
            val t = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.Destroy(t)
        }
    }

    test("with seven lands, the AI casts the removal for its awaken cost, aimed the right way round") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + path)
        d.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val lands = (1..7).map { d.putLandOnBattlefield(player, "Swamp") }
        val ourBears = d.putCreatureOnBattlefield(player, "Grizzly Bears")
        val theirBears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val cardId = d.putCardInHand(player, path.name)

        val chosen = AIPlayer.create(d.services.cardRegistry, player).chooseAction(d.state)
            .shouldBeInstanceOf<CastSpell>()
        chosen.cardId shouldBe cardId
        chosen.alternativeCostType shouldBe AlternativeCostType.AWAKEN
        chosen.targets.first() shouldBe ChosenTarget.Permanent(theirBears)
        ((chosen.targets.last() as ChosenTarget.Permanent).entityId in lands) shouldBe true
        (ourBears in chosen.targets.map { (it as ChosenTarget.Permanent).entityId }) shouldBe false
    }

    test("the awaken land slot reads as helping its target, after the printed slot's reading") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + path)
        d.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        repeat(7) { d.putLandOnBattlefield(player, "Swamp") }
        d.putCreatureOnBattlefield(d.getOpponent(player), "Grizzly Bears")
        val cardId = d.putCardInHand(player, path.name)
        val intents = IntentCatalog.of(CardRegistry().apply { register(TestCards.all + path) })

        val casts = d.legalActions(player).filter { (it.action as? CastSpell)?.cardId == cardId }
        val awaken = casts.single { (it.action as CastSpell).alternativeCostType == AlternativeCostType.AWAKEN }
        val normal = casts.single { !(it.action as CastSpell).useAlternativeCost }
        TargetSelection.polarities(d.state, awaken, intents) shouldBe
            listOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL)
        TargetSelection.polarities(d.state, normal, intents) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("an awaken spell feeds the +1/+1 counter axis") {
        (SynergyAxis.PLUS_COUNTERS in SynergyAxes.of(path).feeds) shouldBe true
    }
})
