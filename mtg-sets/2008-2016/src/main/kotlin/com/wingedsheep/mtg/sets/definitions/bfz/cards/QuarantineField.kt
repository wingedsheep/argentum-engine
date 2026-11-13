package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quarantine Field — Battle for Zendikar #43
 * {X}{X}{W}{W} · Enchantment · Mythic
 *
 * This enchantment enters with X isolation counters on it.
 * When this enchantment enters, for each isolation counter on it, exile up to one target nonland
 * permanent an opponent controls until this enchantment leaves the battlefield.
 *
 * - "For each isolation counter on it, up to one target" is one "up to N" requirement whose cap is
 *   `countersOnSelf(ISOLATION)`, snapshotted as the trigger goes on the stack (CR 603.3d) — the
 *   ruling's "the number of targets the ability has is locked in"; counters changing later don't
 *   change it.
 * - The exile is the modern single-ability "until" duration ([Effects.MoveUntilSourceLeaves]), not a
 *   linked pile plus a leaves trigger: if the Field is gone before the trigger resolves nothing is
 *   exiled, and every exiled card returns at once, immediately, when it leaves (both 2015-08-25
 *   rulings). Tokens don't return.
 */
val QuarantineField = card("Quarantine Field") {
    manaCost = "{X}{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with X isolation counters on it.\n" +
        "When this enchantment enters, for each isolation counter on it, exile up to one target nonland " +
        "permanent an opponent controls until this enchantment leaves the battlefield."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.ISOLATION,
            count = DynamicAmounts.xValue()
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter.NonlandPermanentOpponentControls,
            optional = true,
            dynamicMaxCount = DynamicAmounts.countersOnSelf(CounterType.ISOLATION),
        )
        effect = Effects.ForEachTarget(
            Effects.MoveUntilSourceLeaves(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
        description = "When this enchantment enters, for each isolation counter on it, exile up to one " +
            "target nonland permanent an opponent controls until this enchantment leaves the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "43"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30c525ab-08d5-4d29-aef1-01b0c75ee8d9.jpg?1783938216"

        ruling(
            "2015-08-25",
            "Use the number of isolation counters on Quarantine Field as its triggered ability is put on the " +
                "stack to determine how many targets you may choose. Once the targets are chosen, the number of " +
                "targets the ability has is locked in. Changing the number of isolation counters on Quarantine " +
                "Field won't change how many nonland permanents are exiled."
        )
        ruling(
            "2015-08-25",
            "If Quarantine Field exiles multiple permanents, those cards all return to the battlefield at the same time."
        )
        ruling(
            "2015-08-25",
            "If Quarantine Field leaves the battlefield before its triggered ability resolves, no permanents will be exiled."
        )
        ruling(
            "2015-08-25",
            "Auras attached to the exiled permanents will be put into their owners' graveyards. Equipment attached " +
                "to the exiled creatures will become unattached and remain on the battlefield. Any counters on the " +
                "exiled permanents will cease to exist."
        )
        ruling(
            "2015-08-25",
            "If a token is exiled, it ceases to exist. It won't be returned to the battlefield."
        )
        ruling(
            "2015-08-25",
            "The exiled cards return to the battlefield immediately after Quarantine Field leaves the battlefield. " +
                "Nothing happens between the two events, including state-based actions."
        )
    }
}
