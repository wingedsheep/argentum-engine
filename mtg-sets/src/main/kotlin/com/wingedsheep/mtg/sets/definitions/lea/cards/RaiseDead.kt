package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Raise Dead
 * {B}
 * Sorcery
 * Return target creature card from your graveyard to your hand.
 */
val RaiseDead = card("Raise Dead") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"

    spell {
        target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce07bede-2219-427c-a61a-56518751de42.jpg?1559591346"
    }
}
