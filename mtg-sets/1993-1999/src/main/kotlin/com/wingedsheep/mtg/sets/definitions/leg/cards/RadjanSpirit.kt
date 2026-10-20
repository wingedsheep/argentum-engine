package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Radjan Spirit
 * {3}{G}
 * Creature — Spirit
 * 3/2
 * {T}: Target creature loses flying until end of turn.
 *
 * Any creature is a legal target, flying or not — a creature without flying is simply unaffected.
 */
val RadjanSpirit = card("Radjan Spirit") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "{T}: Target creature loses flying until end of turn."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Tap
        effect = Effects.RemoveKeyword(Keyword.FLYING, target = creature)
        description = "{T}: Target creature loses flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adf3ab1a-5714-4b69-bc51-3752312b2d1f.jpg?1783948045"
    }
}
