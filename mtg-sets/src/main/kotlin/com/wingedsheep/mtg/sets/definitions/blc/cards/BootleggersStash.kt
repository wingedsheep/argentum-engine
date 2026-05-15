package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bootleggers' Stash
 * {5}{G}
 * Artifact
 *
 * Lands you control have "{T}: Create a Treasure token."
 */
val BootleggersStash = card("Bootleggers' Stash") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "Lands you control have \"{T}: Create a Treasure token.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.CreateTreasure()
            ),
            filter = GroupFilter(GameObjectFilter.Land.youControl())
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c10525b5-067c-4a30-a069-875f11f2ff19.jpg?1721429213"
    }
}
