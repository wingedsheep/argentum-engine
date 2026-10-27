package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pins the last-known-information classification of every [EffectTarget.SingleEntity]
 * (CR 113.7a / 603.10 / 608.2h) and the [EntitySnapshot] derived counter accessors that replaced the former per-count
 * scalars. `lkiPolicyFor` is an exhaustive `when`, so a new reference variant cannot be added
 * without choosing its policy here — this test documents and guards those choices.
 */
class LkiPolicyTest : FunSpec({

    test("references that read a permanent after it has left fall back to LKI") {
        listOf(
            EffectTarget.Self,
            EffectTarget.GrantingSource,
            EffectTarget.TriggeringEntity,
            EffectTarget.EnchantedCreature,
            EffectTarget.EquippedCreature,
            EffectTarget.EnchantedPermanent,
            EffectTarget.SacrificedAsCost(),
            EffectTarget.TappedAsCost(),
            EffectTarget.PipelineTarget("chosen"),
        ).forEach { ref ->
            lkiPolicyFor(ref) shouldBe LkiPolicy.LIVE_THEN_LKI
        }
    }

    test("references that only ever name a live permanent never fall back to LKI") {
        listOf(
            EffectTarget.SpecificEntity(EntityId("e1")),
            EffectTarget.RingBearer(),
            EffectTarget.AffectedEntity,
            EffectTarget.AmassedArmy,
            EffectTarget.ChosenCreature,
            EffectTarget.AttachedToTriggeringPermanent,
        ).forEach { ref ->
            lkiPolicyFor(ref) shouldBe LkiPolicy.LIVE_ONLY
        }
    }

    test("a loop's object and a chosen target read LKI only from this resolution's own move off the battlefield") {
        listOf(
            EffectTarget.IterationEntity,
            EffectTarget.ContextTarget(0),
            EffectTarget.BoundVariable("creature"),
        ).forEach { ref ->
            lkiPolicyFor(ref) shouldBe LkiPolicy.LIVE_THEN_OWN_MOVE_LKI
        }
    }

    test("cards that are never on the battlefield read their printed characteristics") {
        listOf(
            EffectTarget.LibraryTop(),
            EffectTarget.LinkedExiledCard(),
            EffectTarget.DiscardedAsCost(),
        ).forEach { ref ->
            lkiPolicyFor(ref) shouldBe LkiPolicy.LIVE_ONLY
        }
    }

    test("snapshot counter accessors derive the former scalar counts from the counters map") {
        val snapshot = EntitySnapshot(
            entityId = EntityId("e1"),
            counters = mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3, CounterType.MINUS_ONE_MINUS_ONE to 1, CounterType.LOYALTY to 4),
        )
        snapshot.plusOnePlusOneCounters shouldBe 3
        snapshot.minusOneMinusOneCounters shouldBe 1
        snapshot.totalCounters shouldBe 8
    }
})
