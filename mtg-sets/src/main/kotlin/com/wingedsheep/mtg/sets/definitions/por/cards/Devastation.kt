package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Effects
/**
 * Devastation
 * {5}{R}{R}
 * Sorcery
 * Destroy all creatures and lands.
 */
val Devastation = card("Devastation") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"

    spell {
        effect = Effects.ForEachInGroup(GroupFilter(GameObjectFilter.CreatureOrLand), Effects.Move(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Eric Peterson"
        flavorText = "Nothing remains."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cce019-162c-4969-89ac-1cf94148a032.jpg"
    }
}
