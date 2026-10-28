package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.model.Rarity

/**
 * Primary Unimatrix Adjunct
 * {3}{W}
 * Artifact Creature — Borg
 * 2/3
 *
 * Artifact creatures you control have double strike.
 */
val PrimaryUnimatrixAdjunct = card("Primary Unimatrix Adjunct") {
    manaCost = "{3}{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "Artifact creatures you control have double strike."
    power = 2
    toughness = 3
    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Jodie Muir"
        flavorText = "Each drone is defined by its role, but even authority provides no individual self. A brain is more useful than a toe, but neither is the body whole."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b94a582c-11bb-4463-a826-bd0c79366752.jpg?1792753859"
    }
}
