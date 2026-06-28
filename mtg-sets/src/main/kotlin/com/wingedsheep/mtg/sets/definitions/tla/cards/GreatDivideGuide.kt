package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
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
 * Great Divide Guide
 * {1}{G}
 * Creature — Human Scout Ally
 * 2/3
 *
 * Each land and Ally you control has "{T}: Add one mana of any color."
 */
val GreatDivideGuide = card("Great Divide Guide") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout Ally"
    power = 2
    toughness = 3
    oracleText = "Each land and Ally you control has \"{T}: Add one mana of any color.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana()
            ),
            filter = GroupFilter(
                (GameObjectFilter.Land or GameObjectFilter().withSubtype(Subtype.ALLY)).youControl()
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Song Qijin"
        flavorText = "\"Who's ready to cross this here canyon?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc3063ec-5ea6-46c1-8331-c740cbaf6c76.jpg?1764121229"
    }
}
