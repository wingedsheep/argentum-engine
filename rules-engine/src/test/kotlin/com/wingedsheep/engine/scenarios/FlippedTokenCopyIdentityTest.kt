package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.handlers.effects.permanent.types.FlipEffectExecutor
import com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfEquippedCreatureExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfEquippedCreatureEffect
import com.wingedsheep.sdk.scripting.effects.FlipEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FlippedTokenCopyIdentityTest : FunSpec({
    val flipDefinition = CardDefinition.flipCard(
        card("Numeric Token Student") {
            manaCost = "{W}"; typeLine = "Creature — Human"; power = 1; toughness = 1
            keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))
            keywordAbility(KeywordAbility.bushido(1))
        },
        card("Numeric Token Teacher") {
            typeLine = "Legendary Creature — Human Samurai"; power = 3; toughness = 4
            keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 4))
            keywordAbility(KeywordAbility.bushido(2))
        },
    )
    test("flipped source token numeric values come from its upright identity plus copy exceptions") {
        val registry = CardRegistry().also { it.register(flipDefinition) }
        val owner = EntityId("numeric-token-owner")
        val upright = CopyExceptionApplier.apply(CardEntityFactory.create(flipDefinition, owner).get<CardComponent>()!!,
            CopyExceptions(addedNumericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 2))))
        val source = ComponentContainer.of(upright.flipSide!!, FlippedComponent(upright),
            ToxicComponent(6), NumericKeywordValuesComponent(mapOf(Keyword.BUSHIDO to 2)))
        val copy = CopyExceptionApplier.withNumericKeywords(ComponentContainer.of(upright), source,
            CopyExceptions(addedNumericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 3))), registry)
        copy.get<ToxicComponent>()!!.amount shouldBe 6
        copy.get<NumericKeywordValuesComponent>()!!.values[Keyword.BUSHIDO] shouldBe 1
    }
    test("ordinary source token numeric values remain component derived") {
        val owner = EntityId("ordinary-token-owner")
        val card = CardEntityFactory.create(flipDefinition, owner).get<CardComponent>()!!
        val source = ComponentContainer.of(card, ToxicComponent(7))
        CopyExceptionApplier.withNumericKeywords(ComponentContainer.of(card), source, CopyExceptions.None)
            .get<ToxicComponent>()!!.amount shouldBe 7
    }
    test("equipped token copy remains nonlegendary and keeps granted haste when it flips") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(flipDefinition))
        d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val source = d.putPermanentOnBattlefield(d.player1, flipDefinition.name)
        val equipment = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(source)) })
        val before = d.state.getBattlefield().toSet()
        val result = CreateTokenCopyOfEquippedCreatureExecutor(d.cardRegistry,
            predicateEvaluator = PredicateEvaluator(cardRegistry = d.cardRegistry)).execute(d.state,
            CreateTokenCopyOfEquippedCreatureEffect(removeLegendary = true, grantHaste = true),
            EffectContext(sourceId = equipment, controllerId = d.player1))
        result.error shouldBe null
        d.replaceState(result.state)
        val token = (d.state.getBattlefield().toSet() - before).single()
        d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
        d.state.getEntity(token)!!.get<CardComponent>()!!.baseKeywords.contains(Keyword.HASTE) shouldBe false
        val flipped = FlipEffectExecutor(d.cardRegistry).execute(d.state, FlipEffect(),
            EffectContext(sourceId = token, controllerId = d.player1))
        flipped.error shouldBe null
        d.replaceState(flipped.state)
        d.state.getEntity(token)!!.get<CardComponent>()!!.typeLine.isLegendary shouldBe false
        d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
        val copier = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val copied = com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor(
            PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry).execute(d.state,
            com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect(
                target = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(token),
                affected = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(copier)),
            EffectContext(sourceId = copier, controllerId = d.player1))
        copied.error shouldBe null
        d.replaceState(copied.state)
        d.state.projectedState.hasKeyword(copier, Keyword.HASTE) shouldBe false
    }
    test("target token copy static exception survives temporary recopy expiry without duplication") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val evaluator = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val amount = evaluator.conditions.amounts
        val created = com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfTargetExecutor(amount,
            com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler(d.cardRegistry), d.cardRegistry,
            com.wingedsheep.engine.handlers.TargetFinder(evaluator)).execute(d.state,
            com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect(
                target = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(bear),
                overridePower = 0, overrideToughness = 0,
                addedStaticAbilities = listOf(com.wingedsheep.sdk.scripting.ModifyStats(2, 2,
                    com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.source()))),
            EffectContext(sourceId = bear, controllerId = d.player1))
        created.error shouldBe null
        val token = (created.state.getBattlefield().toSet() - d.state.getBattlefield().toSet()).single()
        d.replaceState(created.state)
        d.state.projectedState.getPower(token) shouldBe 2
        val copied = com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor(
            evaluator, d.cardRegistry).execute(d.state,
            com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect(
                target = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(bear),
                affected = com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(token),
                duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn),
            EffectContext(sourceId = token, controllerId = d.player1))
        copied.error shouldBe null
        d.replaceState(copied.state)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.getBattlefield().contains(token) shouldBe true
        d.state.getEntity(token)!!.get<CardComponent>()!!.baseStats!!.power
            .shouldBe(com.wingedsheep.sdk.model.CharacteristicValue.Fixed(0))
        d.state.projectedState.getPower(token) shouldBe 2
    }

})
