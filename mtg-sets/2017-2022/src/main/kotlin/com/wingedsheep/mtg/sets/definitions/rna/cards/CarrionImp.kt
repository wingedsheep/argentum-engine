package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Carrion Imp — Ravnica Allegiance #66
 * {3}{B} · Creature — Imp · 2 / 3
 *
 * Flying. An optional enters trigger: exiling the targeted creature card gates the 2 life
 * through `Effects.IfYouDo`, so the life is only gained when the exile actually happens.
 */
val CarrionImp = card("Carrion Imp") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 2
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, you may exile target creature card from a graveyard. " +
        "If you do, you gain 2 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        // "If you do" can only fail when the lone target is gone, and then the whole ability
        // fizzles — so an optional trigger exiling then gaining life is exact.
        optional = true
        effect = Effects.Exile(creatureCard, fromZone = Zone.GRAVEYARD) then Effects.GainLife(2)
        description = "When this creature enters, you may exile target creature card from a graveyard. " +
            "If you do, you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Kev Walker"
        flavorText = "No scrap of nutrition goes to waste in the undercity."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/958caf1d-b159-4c27-8248-00c345f880be.jpg?1783933698"
    }
}
