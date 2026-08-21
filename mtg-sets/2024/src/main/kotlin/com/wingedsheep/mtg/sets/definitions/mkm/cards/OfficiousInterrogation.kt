package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Officious Interrogation — Murders at Karlov Manor #222
 * {W}{U} · Instant
 *
 * This spell costs {W}{U} more to cast for each target beyond the first.
 * Choose any number of target players. Investigate X times, where X is the total number of
 * creatures those players control.
 *
 * **Three things had to exist first, and each is the general form, not a special case.**
 *
 * *The tax.* A per-*mode* mana increase already existed (escalate,
 * `ModalEffect.additionalManaCostPerExtraMode`); a per-*target* one did not. Rather than build a
 * second parallel rail through enumeration, validation and payment, this rides the
 * [ModifySpellCost] / [SpellCostTarget.SelfCast] static that already prices a spell against its own
 * chosen targets — the same machinery behind Dragon's Prey ("costs {2} more if it targets a
 * Dragon"). Two additions: [CostReductionSource.ChosenTargetsBeyondTheFirst], a count source, and
 * [CostModification.IncreaseColoredPerUnit], the exact tax mirror of the long-standing
 * `ReduceColoredPerUnit`. Phyrexian Purge's "3 life more for each target" is the life-side sibling
 * that already worked (`AdditionalCost.PayLifePerTarget`); this is the mana side.
 *
 * *The plural target reference.* "Those players" is not [Player.TargetPlayer] — that resolves to a
 * *single* targeted player, so on a spell targeting four it would have counted one player's
 * creatures and quietly under-investigated. [Player.EachTargetedPlayer] resolves to all of them,
 * and `DynamicAmount.Count` sums over the resolved list, which is what makes "the total number of
 * creatures those players control" one amount rather than a per-target loop. It also gets the
 * 2024-02-02 ruling right for free: a target that has become illegal is already gone from the
 * resolution context, so its creatures aren't counted.
 *
 * *A free cast still owes the tax.* "Costs {W}{U} more" is a cost *increase*, and an alternative
 * cost or a "without paying its mana cost" permission waives the mana cost, not the increases
 * applied to the total cost (CR 601.2f) — the card's own ruling says so outright. The ordinary cast
 * path prices this inside `CostCalculator.calculateEffectiveCost`, which those branches never
 * reach, so `CostCalculator.selfPerTargetTax` is added on top of them in both the validate and the
 * execute half of the cast pipeline.
 *
 * `unlimited = true` is the right target shape for "any number of target players" — not
 * `count = <n>, optional = true`, which caps at a magic number, and not a `dynamicMaxCount`, which
 * is for "X target …". Zero targets is a legal (if inadvisable) cast, and the mana value stays 2
 * however many targets are chosen, because the increase is applied to the *total cost* and never to
 * the printed one.
 */
val OfficiousInterrogation = card("Officious Interrogation") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "This spell costs {W}{U} more to cast for each target beyond the first.\n" +
        "Choose any number of target players. Investigate X times, where X is the total number " +
        "of creatures those players control. (To investigate, create a Clue token. It's an " +
        "artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseColoredPerUnit(
                symbols = "{W}{U}",
                countSource = CostReductionSource.ChosenTargetsBeyondTheFirst
            )
        )
    }

    spell {
        target("targets", TargetPlayer(unlimited = true))
        effect = Effects.Investigate(
            count = DynamicAmount.Count(
                player = Player.EachTargetedPlayer,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a433ca4c-82d0-4e49-bc8e-98e18dd174e9.jpg?1783912842"

        ruling(
            "2024-02-02",
            "Any target players that are no longer legal targets by the time Officious " +
                "Interrogation resolves won't have their creatures counted when determining how " +
                "many tokens you create."
        )
        ruling(
            "2024-02-02",
            "You choose how many targets Officious Interrogation has and what those targets are " +
                "as you cast it. You can't choose the same target more than once. It's legal to " +
                "cast Officious Interrogation with no targets, although this particular option " +
                "should be placed under some serious scrutiny."
        )
        ruling(
            "2024-02-02",
            "Officious Interrogation's mana value doesn't change no matter how many targets it has."
        )
        ruling(
            "2024-02-02",
            "If a spell or ability allows you to cast Officious Interrogation without paying its " +
                "mana cost, you must still pay the additional cost for any targets beyond the " +
                "first."
        )
    }
}
