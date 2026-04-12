package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sapling Nursery
 * {6}{G}{G}
 * Enchantment
 *
 * Affinity for Forests (This spell costs {1} less to cast for each Forest you control.)
 * Landfall — Whenever a land you control enters, create a 3/4 green Treefolk creature token with reach.
 * {1}{G}, Exile this enchantment: Treefolk and Forests you control gain indestructible until end of turn.
 */
val SaplingNursery = card("Sapling Nursery") {
    manaCost = "{6}{G}{G}"
    typeLine = "Enchantment"
    oracleText = "Affinity for Forests (This spell costs {1} less to cast for each Forest you control.)\n" +
        "Landfall — Whenever a land you control enters, create a 3/4 green Treefolk creature token with reach.\n" +
        "{1}{G}, Exile this enchantment: Treefolk and Forests you control gain indestructible until end of turn."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.FOREST))

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.CreateToken(
            power = 3,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Treefolk"),
            keywords = setOf(Keyword.REACH),
            imageUri = "https://cards.scryfall.io/normal/front/8/2/82e01706-ab45-4e52-9ee1-7070567234fd.jpg?1767955530"
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.ExileSelf)
        effect = EffectPatterns.grantKeywordToAll(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(
                (GameObjectFilter.Permanent.withSubtype(Subtype.TREEFOLK) or
                    GameObjectFilter.Permanent.withSubtype(Subtype.FOREST)).youControl()
            )
        )
        description = "Treefolk and Forests you control gain indestructible until end of turn"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Vincent Christiaens"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3199bea9-fef7-45fe-8777-2103d84a9347.jpg?1767862603"
    }
}
