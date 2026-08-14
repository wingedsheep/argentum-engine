package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kellan, the Fae-Blooded // Birthright Boon
 * {2}{R}
 * Legendary Creature — Human Faerie
 * 2/2
 * Double strike
 * Other creatures you control get +1/+0 for each Aura and Equipment attached to Kellan.
 *
 * Adventure: Birthright Boon — {1}{W}, Sorcery — Adventure
 * Search your library for an Aura or Equipment card, reveal it, put it into your hand, then shuffle.
 *
 * The anthem reads Kellan's live attachment count, so attaching or removing an Aura/Equipment
 * immediately changes every other creature's projected power. Birthright Boon uses the shared
 * library-search recipe and the engine's existing Adventure resolution/exile permission.
 */
val KellanTheFaeBlooded = card("Kellan, the Fae-Blooded") {
    manaCost = "{2}{R}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Faerie"
    oracleText = "Double strike\n" +
        "Other creatures you control get +1/+0 for each Aura and Equipment attached to Kellan."
    power = 2
    toughness = 2

    keywords(Keyword.DOUBLE_STRIKE)

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.OtherCreaturesYouControl,
            powerBonus = DynamicAmounts.attachmentsOnSelf(),
            toughnessBonus = DynamicAmount.Fixed(0),
        )
    }

    adventure("Birthright Boon") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Search your library for an Aura or Equipment card, reveal it, put it into " +
            "your hand, then shuffle. (Then exile this card. You may cast the creature later from exile.)"

        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.withAnySubtype("Aura", "Equipment"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true,
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec5e2680-8b42-4571-ab45-4936aec51901.jpg?1783915063"
    }
}
