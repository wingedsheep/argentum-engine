package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Rain of Salt
 * {4}{R}{R}
 * Sorcery
 * Destroy two target lands.
 */
val RainOfSalt = card("Rain of Salt") {
    manaCost = "{4}{R}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetPermanent(count = 2, filter = TargetFilter.Land)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true) then
                MoveToZoneEffect(EffectTarget.ContextTarget(1), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Allen Williams"
        flavorText = "It pelts the land with its fury, spoiling what was once sweet."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/661ffab2-9cf5-492d-874f-de73d7a13e2b.jpg"
    }
}
