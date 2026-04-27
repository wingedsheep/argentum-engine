package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Moon-Vigil Adherents
 * {2}{G}{G}
 * Creature — Elf Druid
 * 0/0
 * Trample
 * This creature gets +1/+1 for each creature you control and each creature card in your graveyard.
 */
val MoonVigilAdherents = card("Moon-Vigil Adherents") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Elf Druid"
    power = 0
    toughness = 0
    oracleText = "Trample\nThis creature gets +1/+1 for each creature you control and each creature card in your graveyard."

    keywords(Keyword.TRAMPLE)

    val bonus = DynamicAmount.Add(
        DynamicAmounts.creaturesYouControl(),
        DynamicAmounts.creatureCardsInYourGraveyard()
    )

    staticAbility {
        ability = GrantDynamicStatsEffect(
            target = StaticTarget.SourceCreature,
            powerBonus = bonus,
            toughnessBonus = bonus
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "David Palumbo"
        flavorText = "Adherents of the moon seek to lay claim over the entire plane and bring it under Isilu's domain."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60621c37-62e1-4261-ae76-3946b4a0cfa3.jpg?1767957171"
    }
}
