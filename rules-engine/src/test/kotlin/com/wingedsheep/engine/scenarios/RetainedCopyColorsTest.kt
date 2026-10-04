package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RetainedCopyColorsTest : FunSpec({
    val recopy = card("Test Colored Recopy") {
        manaCost = "{R}"
        typeLine = "Creature — Shapeshifter"; power = 2; toughness = 2
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            val t = target(TargetFilter.Creature)
            effect = Effects.EachPermanentBecomesCopyOfTarget(target = t, affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainColors = true, retainResolvingTriggeredAbility = true))
        }
    }
    val green = card("Test Green Copier") {
        manaCost = "{G}"; typeLine = "Creature — Shapeshifter"; power = 2; toughness = 2
        replacementEffect(EntersAsCopy(exceptions = CopyExceptions(retainColors = true)))
    }
    val returnCard = card("Test Colored Reanimate") {
        manaCost = "{B}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureInGraveyard)
            effect = Effects.PutOntoBattlefield(t)
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(recopy, green, returnCard))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    test("a later recopy retains the current red identity instead of the original green identity") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, recopy.name)
        val copier = d.putPermanentOnBattlefield(d.player1, green.name)
        val evaluator = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val executor = EachPermanentBecomesCopyOfTargetExecutor(evaluator)
        val result = executor.execute(d.state, EachPermanentBecomesCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0), affected = EffectTarget.Self),
            EffectContext(sourceId = copier, controllerId = d.player1, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(source))))
        d.replaceState(result.state)
        card(d, copier).colors shouldBe setOf(Color.RED)
        d.moveToGraveyard(source)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.bothPass().error shouldBe null
        card(d, copier).name shouldBe "Grizzly Bears"
        card(d, copier).colors shouldBe setOf(Color.RED)
        card(d, copier).copyTriggeredAbilities.size shouldBe 1
    }
    test("a group copy retains each affected permanent's own color including colorless") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val red = d.putPermanentOnBattlefield(d.player1, recopy.name)
        val blue = d.putPermanentOnBattlefield(d.player1, green.name)
        d.replaceState(d.state.updateEntity(blue) { it.with(card(d, blue).copy(colors = setOf(Color.BLUE))) })
        val colorless = d.putPermanentOnBattlefield(d.player1, green.name)
        d.replaceState(d.state.updateEntity(colorless) { it.with(card(d, colorless).copy(colors = emptySet())) })
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry))
            .execute(d.state, EachPermanentBecomesCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0), filter = GroupFilter.AllCreaturesYouControl,
                exceptions = CopyExceptions(retainColors = true)),
                EffectContext(sourceId = red, controllerId = d.player1, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(source))))
        result.events.filterIsInstance<com.wingedsheep.engine.core.CopiableCharacteristicsChangedEvent>()
            .map { it.entityId }.toSet() shouldBe setOf(red, blue, colorless)
        d.replaceState(result.state)
        card(d, red).colors shouldBe setOf(Color.RED)
        card(d, blue).colors shouldBe setOf(Color.BLUE)
        card(d, colorless).colors shouldBe emptySet()
    }
    for (fromSelf in listOf(false, true)) {
        test("new token has no original color to retain and omits copied devoid fromSelf=$fromSelf") {
            val maker = card("Test Retained Token Colors $fromSelf") {
                manaCost = "{U}"; typeLine = "Creature — Eldrazi"
                power = 1; toughness = 1; keywords(Keyword.DEVOID)
                triggeredAbility {
                    trigger = Triggers.you.beginningOf(Step.UPKEEP)
                    effect = if (fromSelf) Effects.CreateTokenCopyOfSelf(exceptions = CopyExceptions(retainColors = true))
                        else Effects.CreateTokenCopyOfTarget(EffectTarget.Self, exceptions = CopyExceptions(retainColors = true))
                }
            }
            val d = driver()
            d.registerCards(listOf(maker))
            val source = d.putPermanentOnBattlefield(d.player1, maker.name)
            d.passPriorityUntil(Step.UPKEEP)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.passPriorityUntil(Step.UPKEEP)
            d.bothPass().error shouldBe null
            val token = d.state.getBattlefield().single { it != source && card(d, it).name == maker.name }
            card(d, token).colors shouldBe emptySet()
            card(d, token).baseKeywords shouldBe emptySet()
        }
    }
    test("effect-driven entry retains the entrant's color before copying") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val entrant = d.putCardInGraveyard(d.player1, green.name)
        val spell = d.putCardInHand(d.player1, returnCard.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(
            com.wingedsheep.engine.state.components.stack.ChosenTarget.Card(entrant, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(source)).error shouldBe null
        card(d, entrant).name shouldBe "Grizzly Bears"
        card(d, entrant).colors shouldBe setOf(Color.GREEN)
    }
})
