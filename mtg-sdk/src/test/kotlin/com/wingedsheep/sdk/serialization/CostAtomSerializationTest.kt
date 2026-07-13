package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * §3.2 "one cost language": [CostAtom] is the single shared vocabulary of payable things, carried by
 * every cost *context* ([PayCost.Atom], [AdditionalCost.Atom], and — once migrated — `AbilityCost`).
 *
 * This is the SDK-side coverage net (the PR #606 pattern applied to the cost vocabulary): every
 * concrete [CostAtom] subtype must have a representative below that round-trips through the
 * polymorphic serializer inside both wrappers. The `sealedSubclasses` completeness assertion fails the
 * moment a new atom is added without a representative — forcing a conscious decision (and a reminder to
 * add the matching engine payment branch; the engine `when (atom)` blocks are exhaustive, so a missing
 * branch is already a compile error).
 */
class CostAtomSerializationTest : FunSpec({

    val json = CardSerialization.json

    // One representative instance per concrete CostAtom subtype.
    val representatives: List<CostAtom> = listOf(
        CostAtom.Mana(ManaCost.parse("{2}{U}")),
        CostAtom.PayLife(3),
        CostAtom.Sacrifice(GameObjectFilter.Creature, count = 2),
        CostAtom.Discard(count = 1, filter = GameObjectFilter.Any, random = true),
        CostAtom.ExileFrom(Zone.GRAVEYARD, GameObjectFilter.Creature, count = 3),
        CostAtom.TapPermanents(count = 1, filter = GameObjectFilter.Creature),
        CostAtom.ReturnToHand(GameObjectFilter.Any, count = 1),
        CostAtom.RevealFromHand(GameObjectFilter.Any, count = 1),
        CostAtom.RemoveCounters(Counters.PLUS_ONE_PLUS_ONE, filter = GameObjectFilter.Creature),
        CostAtom.RemoveCounters("charge", self = true),
        CostAtom.RemoveCounters(counterType = null, filter = GameObjectFilter.Creature)
    )

    test("every concrete CostAtom subtype has a representative in this test") {
        val expected = CostAtom::class.sealedSubclasses
            .map { it.simpleName }
            .toSet()
        val covered = representatives.map { it::class.simpleName }.toSet()
        covered shouldBe expected
    }

    test("each CostAtom round-trips through the polymorphic serializer") {
        for (atom in representatives) {
            val restored = json.decodeFromString(CostAtom.serializer(), json.encodeToString(CostAtom.serializer(), atom))
            restored shouldBe atom
        }
    }

    test("each CostAtom round-trips inside a PayCost.Atom wrapper") {
        for (atom in representatives) {
            val cost: PayCost = PayCost.Atom(atom)
            val restored = json.decodeFromString(PayCost.serializer(), json.encodeToString(PayCost.serializer(), cost))
            restored shouldBe cost
        }
    }

    test("each CostAtom round-trips inside an AdditionalCost.Atom wrapper") {
        for (atom in representatives) {
            val cost: AdditionalCost = AdditionalCost.Atom(atom)
            val restored = json.decodeFromString(AdditionalCost.serializer(), json.encodeToString(AdditionalCost.serializer(), cost))
            restored shouldBe cost
        }
    }

    test("selectionCount is the per-atom selection size and 0 for non-selection atoms") {
        CostAtom.Mana(ManaCost.parse("{1}")).selectionCount shouldBe 0
        CostAtom.PayLife(3).selectionCount shouldBe 0
        CostAtom.Discard(count = 2, random = true).selectionCount shouldBe 0  // random discard takes no selection
        CostAtom.Discard(count = 2, random = false).selectionCount shouldBe 2
        CostAtom.Sacrifice(count = 2).selectionCount shouldBe 2
        CostAtom.ExileFrom(Zone.GRAVEYARD, count = 3).selectionCount shouldBe 3
        CostAtom.TapPermanents(count = 1).selectionCount shouldBe 1
    }
})
