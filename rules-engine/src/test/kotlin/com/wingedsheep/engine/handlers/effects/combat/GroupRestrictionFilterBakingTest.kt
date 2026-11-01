package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.handlers.effects.ResolutionFilterBaker
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Group restrictions ("creatures [that player] controls can't block this turn") outlive the
 * resolution that created them: their filters are re-read on every projection, where the
 * ability's targets and trigger are gone. [ResolutionFilterBaker] pins every context-bound player
 * reference to a concrete player at resolution, so the restriction keeps naming that player while
 * the set of their creatures stays live (CR 611.2c).
 */
class GroupRestrictionFilterBakingTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
    }

    test("a target-player group restriction keeps binding that player after resolution") {
        val d = driver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val context = EffectContext(
            sourceId = null,
            controllerId = d.player1,
            targets = listOf(ChosenTarget.Player(d.player2))
        )

        val result = CantBlockGroupExecutor(d.services.predicateEvaluator.amounts).execute(
            d.state,
            CantBlockGroupEffect(GroupFilter(GameObjectFilter.Creature.targetPlayerControls())),
            context
        )
        d.replaceState(result.state)

        d.state.projectedState.cantBlock(theirs) shouldBe true
        d.state.projectedState.cantBlock(mine) shouldBe false

        val later = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.state.projectedState.cantBlock(later) shouldBe true
    }

    test("context-bound references are pinned, state-relative ones are left symbolic") {
        val d = driver()
        val context = EffectContext(
            sourceId = null,
            controllerId = d.player1,
            targets = listOf(ChosenTarget.Player(d.player2))
        )
        val pinned = ControllerPredicate.ControlledByReferencedPlayer(EffectTarget.SpecificEntity(d.player2))

        ResolutionFilterBaker.bake(GameObjectFilter.Creature.targetPlayerControls(), context, d.state)
            .controllerPredicate shouldBe pinned
        ResolutionFilterBaker.bake(
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.ContextTarget(0)), context, d.state
        ).controllerPredicate shouldBe pinned
        ResolutionFilterBaker.bake(GameObjectFilter.Creature.youControl(), context, d.state)
            .controllerPredicate shouldBe ControllerPredicate.ControlledByYou

        // Nothing to answer with (no combat): the reference stays symbolic rather than matching nobody.
        val defending = GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
        ResolutionFilterBaker.bake(defending, EffectContext(sourceId = null, controllerId = d.player1), d.state) shouldBe
            defending
    }

    test("dynamic amounts are frozen at resolution into their fixed siblings") {
        val d = driver()
        repeat(3) { d.putCreatureOnBattlefield(d.player1, "Grizzly Bears") }
        val context = EffectContext(sourceId = null, controllerId = d.player1)
        val myCreatures = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()

        ResolutionFilterBaker.bake(
            GameObjectFilter.Creature.powerAtMostDynamic(myCreatures), context, d.state, d.services.predicateEvaluator.amounts
        ).cardPredicates shouldBe GameObjectFilter.Creature.powerAtMost(3).cardPredicates
        // Nested under Not / anyOf branches too.
        val nested = GameObjectFilter(
            anyOf = listOf(
                GameObjectFilter(cardPredicates = listOf(CardPredicate.Not(CardPredicate.ManaValueEqualsDynamic(myCreatures)))),
                GameObjectFilter(cardPredicates = listOf(CardPredicate.ToughnessEqualsDynamic(myCreatures)))
            )
        )
        val baked = ResolutionFilterBaker.bake(nested, context, d.state, d.services.predicateEvaluator.amounts)
        baked.anyOf.flatMap { it.cardPredicates }.toSet() shouldBe setOf(
            CardPredicate.Not(CardPredicate.ManaValueEquals(3)),
            CardPredicate.ToughnessEquals(3)
        )
        // Without an evaluator only players are baked.
        ResolutionFilterBaker.bake(GameObjectFilter.Creature.powerAtMostDynamic(myCreatures), context, d.state)
            .cardPredicates shouldBe GameObjectFilter.Creature.powerAtMostDynamic(myCreatures).cardPredicates
    }

    test("an amount read off the object under test stays symbolic") {
        val d = driver()
        val context = EffectContext(sourceId = null, controllerId = d.player1)
        val perCandidate = GameObjectFilter.Creature.powerAtMostDynamic(
            DynamicAmounts.battlefield(Player.Candidate, GameObjectFilter.Land).count()
        )
        ResolutionFilterBaker.bake(perCandidate, context, d.state, d.services.predicateEvaluator.amounts) shouldBe
            perCandidate
    }

    test("a can't-block group keeps the resolution-time number while the matching set stays live") {
        val d = driver()
        val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val myCreatures = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()

        // One creature of mine at resolution: power <= 1.
        val result = CantBlockGroupExecutor(d.services.predicateEvaluator.amounts).execute(
            d.state,
            CantBlockGroupEffect(GroupFilter(GameObjectFilter.Creature.opponentControls().powerAtMostDynamic(myCreatures))),
            EffectContext(sourceId = null, controllerId = d.player1)
        )
        d.replaceState(result.state)
        d.state.projectedState.cantBlock(theirs) shouldBe false

        // A second creature of mine doesn't raise the frozen cap to 2.
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.state.projectedState.cantBlock(theirs) shouldBe false
        d.state.projectedState.cantBlock(first) shouldBe false
        // A newcomer with power 1 is caught: the set is live.
        val elf = d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        d.state.projectedState.cantBlock(elf) shouldBe true
    }

    test("a power-gated can't-block group reads final power, counters included (CR 613.11)") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val result = CantBlockGroupExecutor(d.services.predicateEvaluator.amounts).execute(
            d.state,
            CantBlockGroupEffect(GroupFilter(GameObjectFilter.Creature.powerAtMost(2))),
            EffectContext(sourceId = null, controllerId = d.player1)
        )
        d.replaceState(result.state)
        d.state.projectedState.cantBlock(bears) shouldBe true

        // A +1/+1 counter (layer 7) lifts it to 3 power: it can block again.
        d.replaceState(d.state.updateEntity(bears) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        })
        d.state.projectedState.cantBlock(bears) shouldBe false
    }

    test("only the creatures in a dynamic can't-block group show its badge") {
        val d = driver()
        val small = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val big = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val result = CantBlockGroupExecutor(d.services.predicateEvaluator.amounts).execute(
            d.state,
            CantBlockGroupEffect(GroupFilter(GameObjectFilter.Creature.opponentControls().powerAtMost(2))),
            EffectContext(sourceId = null, controllerId = d.player1)
        )
        d.replaceState(result.state)

        val view = ClientStateTransformer(d.cardRegistry, predicateEvaluator = d.services.predicateEvaluator)
            .transform(d.state, d.player2)
        view.cards.getValue(small).activeEffects.any { it.effectId.startsWith("cant_block") } shouldBe true
        view.cards.getValue(big).activeEffects.any { it.effectId.startsWith("cant_block") } shouldBe false
    }
})

