package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Encroaching Mycosynth
 * {3}{U}
 * Artifact
 *
 * Nonland permanents you control are artifacts in addition to their other types. The same is true
 * for permanent spells you control and nonland permanent cards you own that aren't on the
 * battlefield.
 *
 * One [GrantCardType]: the filter is the battlefield half, and its card predicates (nonland,
 * permanent) are what qualify spells and cards off the battlefield for the two cross-zone flags.
 */
val EncroachingMycosynth = card("Encroaching Mycosynth") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "Nonland permanents you control are artifacts in addition to their other types. The same is true " +
        "for permanent spells you control and nonland permanent cards you own that aren't on the battlefield."

    staticAbility {
        ability = GrantCardType(
            cardType = "ARTIFACT",
            filter = GroupFilter(GameObjectFilter.NonlandPermanent.youControl()),
            includeControlledSpells = true,
            includeOwnedCardsOutsideBattlefield = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Martin de Diego Sádaba"
        flavorText = "\"Careful—however they might look, they're not human. A step too close and they'll drag you in.\"\n—Kara Vrist, resistance spymaster"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65a2fcc9-2317-48a1-a5eb-234fb3300364.jpg?1783918066"
    }
}
