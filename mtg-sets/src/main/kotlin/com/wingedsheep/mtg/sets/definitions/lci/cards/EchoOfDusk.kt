package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Echo of Dusk — {1}{B}
 * Creature — Vampire Spirit
 * 2/2
 *
 * Descend 4 — As long as there are four or more permanent cards in your graveyard, this
 * creature gets +1/+1 and has lifelink.
 *
 * Both bonuses are modelled as separate [ConditionalStaticAbility] instances gated by the
 * same [Conditions.CardsInGraveyardMatchingAtLeast](4, Permanent) descend-4 check.  The
 * stat bonus lives in Layer 7c (ModifyStats); the keyword grant lives in Layer 6 (GrantKeyword).
 * Both re-evaluate continuously as graveyard contents change.
 */
val EchoOfDusk = card("Echo of Dusk") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Spirit"
    oracleText = "Descend 4 — As long as there are four or more permanent cards in your graveyard, this creature gets +1/+1 and has lifelink."
    power = 2
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source()),
            condition = Conditions.CardsInGraveyardMatchingAtLeast(4, GameObjectFilter.Permanent)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.CardsInGraveyardMatchingAtLeast(4, GameObjectFilter.Permanent)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Domenico Cava"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/319a457c-89b7-47f6-a13c-20bb50d41138.jpg?1782694527"
    }
}
