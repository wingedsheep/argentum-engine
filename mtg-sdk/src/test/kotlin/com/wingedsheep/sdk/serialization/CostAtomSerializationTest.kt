package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CardMeasure
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.assertions.throwables.shouldThrow
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
        CostAtom.PayPlayerCounters(CounterType.ENERGY),
        CostAtom.PayPlayerCounters(CounterType.ENERGY, com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue),
        CostAtom.PayPlayerCounters(CounterType.ENERGY, CostAtom.CollectEvidence.TARGET_SUM),
        CostAtom.Sacrifice(GameObjectFilter.Creature, count = 2),
        CostAtom.Discard(count = 1, filter = GameObjectFilter.Any, random = true),
        CostAtom.ExileFrom(Zone.GRAVEYARD, GameObjectFilter.Creature, count = 3),
        // The processor cost: every player's exile, into the owner's graveyard.
        CostAtom.ExileFrom(
            Zone.EXILE, GameObjectFilter.Any.ownedByOpponent(), count = 1,
            anyPlayersZone = true, destination = Zone.GRAVEYARD,
        ),
        CostAtom.Mill(count = 1),
        CostAtom.ExileTopOfLibrary(count = 10),
        CostAtom.VariablePermanents(GameObjectFilter.Artifact, minCount = 1, excludeSelf = true),
        CostAtom.TapPermanents(count = 1, filter = GameObjectFilter.Creature),
        CostAtom.ReturnToHand(GameObjectFilter.Any, count = 1),
        CostAtom.RevealFromHand(GameObjectFilter.Any, count = 1),
        CostAtom.PutFromHandOnTopOfLibrary(count = 1, filter = GameObjectFilter.Any),
        CostAtom.RemoveCounters(CounterType.PLUS_ONE_PLUS_ONE, filter = GameObjectFilter.Creature),
        CostAtom.RemoveCounters(CounterType.CHARGE, self = true),
        CostAtom.RemoveCounters(counterType = null, filter = GameObjectFilter.Creature),
        CostAtom.PutCountersOnSelf(CounterType.PAGE, count = 1),
        CostAtom.PutCountersOnPermanent(CounterType.MINUS_ONE_MINUS_ONE, filter = GameObjectFilter.Creature),
        CostAtom.CollectEvidence(amount = 6),
        // Urgent Necropsy's derived threshold — the shape that made `amount` a DynamicAmount, and
        // so the one whose round-trip is worth pinning separately from the literal.
        CostAtom.CollectEvidence(CostAtom.CollectEvidence.TARGET_SUM),
        CostAtom.RevealNotedCreatureType,
        CostAtom.DiscardHand,
        CostAtom.SacrificeAll(GameObjectFilter.Creature),
        CostAtom.Unattach(),
        CostAtom.Unattach(ofGrantingPermanent = true),
        CostAtom.ExileFromGraveyardForTotal(
            filter = GameObjectFilter.Any.withColor(Color.BLACK),
            measure = CardMeasure.ColoredManaSymbols(listOf(Color.BLACK)),
            minTotal = 15,
        ),
        CostAtom.ExileFromGraveyardForTotal(
            measure = CardMeasure.ManaValue,
            minTotal = 6,
        ),
        CostAtom.ExileFromGraveyardForTotal(
            measure = CardMeasure.DistinctCardTypes,
            minTotal = 4,
            excludeSelf = true,
        )
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
        // Top-of-library costs take no selection: the cards are the top, not a player's pick.
        CostAtom.Mill(count = 1).selectionCount shouldBe 0
        CostAtom.ExileTopOfLibrary(count = 10).selectionCount shouldBe 0
    }

    test("the processor cost reads as printed and defaults leave the exile cost unchanged") {
        val processor = CostAtom.ExileFrom(
            Zone.EXILE, GameObjectFilter.Any.ownedByOpponent(), anyPlayersZone = true, destination = Zone.GRAVEYARD,
        )
        processor.exiles shouldBe false
        processor.description shouldBe "put a card an opponent owns from exile into that player's graveyard"
        processor.copy(count = 2).description shouldBe
            "put two cards an opponent owns from exile into their owners' graveyards"
        processor.selectionCount shouldBe 1

        val exile = CostAtom.ExileFrom(Zone.GRAVEYARD, GameObjectFilter.Creature)
        exile.exiles shouldBe true
        // destination is a defaulted field: an exile cost's JSON carries no new key.
        json.encodeToString(CostAtom.serializer(), exile).contains("destination") shouldBe false
    }

    test("a destination other than exile or graveyard, or equal to the source zone, is rejected") {
        shouldThrow<IllegalArgumentException> { CostAtom.ExileFrom(Zone.GRAVEYARD, destination = Zone.HAND) }
        shouldThrow<IllegalArgumentException> { CostAtom.ExileFrom(Zone.GRAVEYARD, destination = Zone.GRAVEYARD) }
    }
})
