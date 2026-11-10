package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Walker of the Wastes
 * {4}{C}
 * Creature — Eldrazi
 * 4/4
 * Trample
 * This creature gets +1/+1 for each land you control named Wastes.
 *
 * Counts by *name*, not by basic land type: Wastes has no land subtype, so only
 * [GameObjectFilter.named] picks it out.
 */
val WalkerOfTheWastes = card("Walker of the Wastes") {
    manaCost = "{4}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 4
    toughness = 4
    oracleText = "({C} represents colorless mana.)\n" +
        "Trample\n" +
        "This creature gets +1/+1 for each land you control named Wastes."

    keywords(Keyword.TRAMPLE)

    val wastesYouControl = DynamicAmounts.battlefield(
        Player.You,
        GameObjectFilter.Land.named("Wastes")
    ).count()

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = wastesYouControl,
            toughnessBonus = wastesYouControl
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Joseph Meehan"
        flavorText = "In the absence of life, something yet grows."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34f635ac-ad6d-4a13-a08e-64c8e2b9779c.jpg?1783937928"
        ruling("2016-01-22", "The last ability applies only if Walker of the Wastes is on the battlefield.")
    }
}
