package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Elvish Mariner
 * {2}{U}
 * Creature — Elf Pilot
 * 3/2
 *
 * Whenever this creature attacks, scry 1.
 * Whenever you scry, tap up to X target nonland permanents, where X is the
 * number of cards looked at while scrying this way.
 */
val ElvishMariner = card("Elvish Mariner") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elf Pilot"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, scry 1.\n" +
        "Whenever you scry, tap up to X target nonland permanents, where X is the " +
        "number of cards looked at while scrying this way."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = LibraryPatterns.scry(1)
    }

    triggeredAbility {
        trigger = Triggers.WheneverYouScry
        target(
            "up to X target nonland permanents",
            TargetPermanent(
                optional = true,
                filter = TargetFilter.NonlandPermanent,
                dynamicMaxCount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_SCRY_COUNT)
            )
        )
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "283"
        artist = "Axel Sauerwald"
        flavorText = "In the days of the Kings, most of the High Elves dwelt with Círdan or in the seaward lands of Lindon."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7604d357-4196-421f-a5c3-c7cbe79f35cb.jpg?1719684261"
    }
}
