package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Raise Dead
 * {B}
 * Sorcery
 * Return target creature card from your graveyard to your hand.
 */
val RaiseDead = card("Raise Dead") {
    manaCost = "{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Jeff A. Menges"
        flavorText = "\"The dead serve as well as the living.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0584553-a25e-4030-ab39-53550cba3f0b.jpg"
    }
}
