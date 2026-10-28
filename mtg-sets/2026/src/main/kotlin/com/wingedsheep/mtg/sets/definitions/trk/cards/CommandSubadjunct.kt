package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Command Subadjunct
 * {W}
 * Artifact Creature — Borg
 * 2/1
 *
 * Artifact creatures you control have vigilance.
 */
val CommandSubadjunct = card("Command Subadjunct") {
    manaCost = "{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "Artifact creatures you control have vigilance."
    power = 2
    toughness = 1
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Alex Negrea"
        flavorText = "The elegance of the Borg lies in how readily they integrate other species' solutions."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/853bf74d-0b61-43df-9018-0b40b624a9d1.jpg?1792753641"
    }
}
