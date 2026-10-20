package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shelkin Brownie
 * {1}{G}
 * Creature — Ouphe
 * 1/1
 *
 * {T}: Target creature loses all "bands with other" abilities until end of turn.
 */
val ShelkinBrownie = card("Shelkin Brownie") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Ouphe"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature loses all \"bands with other\" abilities until end of turn."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Tap
        // Strips every "bands with other" quality, but leaves plain banding in place.
        effect = Effects.RemoveAllBandsWithOther(creature)
        description = "{T}: Target creature loses all \"bands with other\" abilities until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fddcc557-871d-425b-b4ee-bc0c9bc717aa.jpg?1783948044"
    }
}
