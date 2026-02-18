package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardPredicate
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Akroma's Vengeance
 * {4}{W}{W}
 * Sorcery
 * Destroy all artifacts, creatures, and enchantments.
 * Cycling {3}
 */
val AkromasVengeance = card("Akroma's Vengeance") {
    manaCost = "{4}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Destroy all artifacts, creatures, and enchantments.\nCycling {3}"

    spell {
        effect = ForEachInGroupEffect(
            filter = GroupFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(
                                CardPredicate.IsArtifact,
                                CardPredicate.IsCreature,
                                CardPredicate.IsEnchantment
                            )
                        )
                    )
                )
            ),
            effect = MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "Ixidor had only to imagine their ruin and Akroma made it so."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e33aaf7-7490-4b64-a966-82fbf7ca8686.jpg?1562917166"
    }
}
