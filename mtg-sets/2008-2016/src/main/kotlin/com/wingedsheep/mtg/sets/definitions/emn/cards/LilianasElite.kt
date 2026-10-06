package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Liliana's Elite — Eldritch Moon #94 (earliest printing; reprinted in Jumpstart 2022)
 * {2}{B} · Creature — Zombie · 1/1
 *
 * This creature gets +1/+1 for each creature card in your graveyard.
 *
 * The Myr Adapter shape over a graveyard count: a [GrantDynamicStats] on [GroupFilter.source]
 * whose bonus is [DynamicAmounts.creatureCardsInYourGraveyard], recomputed continuously as a
 * layer-7c +N/+N. Being a static ability, it only applies while the Elite is on the battlefield
 * (per the ruling).
 */
val LilianasElite = card("Liliana's Elite") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+1 for each creature card in your graveyard."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.creatureCardsInYourGraveyard(),
            toughnessBonus = DynamicAmounts.creatureCardsInYourGraveyard()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Deruchenko Alexander"
        flavorText = "\"You have to admit, Gideon—he has great form.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad542aa9-5c0e-4962-b167-9071e66f9c03.jpg?1783937484"
        ruling("2016-07-13", "The ability applies only while Liliana's Elite is on the battlefield.")
    }
}
