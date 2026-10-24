package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silverquill Apprentice — Strixhaven: School of Mages #231 (canonical printing)
 * {W}{B} · Creature — Human Warlock · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, target creature gets +1/+0
 * until end of turn.
 */
val SilverquillApprentice = card("Silverquill Apprentice") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, target " +
        "creature gets +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, creature)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, target " +
            "creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Mike Bierek"
        flavorText = "She had spoken the words a hundred times, but under the spotlight, in front of " +
            "everyone, something new emerged. And the crowd went wild."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d0770e-9084-4aa7-b543-2b6ba18378dc.jpg?1783927292"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
