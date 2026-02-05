package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Stone Rain
 * {2}{R}
 * Sorcery
 * Destroy target land.
 */
val StoneRain = card("Stone Rain") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetPermanent(filter = TargetFilter.Land)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Graveyard, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "John Matson"
        flavorText = "I cast a thousand tiny suns— Beware my many dawns."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57f84a13-d7dc-491b-a77c-1b99b6797d7e.jpg"
    }
}
