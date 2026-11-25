package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * War Falcon
 * {W}
 * Creature — Bird
 * 2/1
 * Flying
 * This creature can't attack unless you control a Knight or a Soldier.
 *
 * "A Knight or a Soldier" is any permanent with either subtype (not only creatures), so the
 * filter is [GameObjectFilter.Permanent] over the Knight / Soldier union.
 */
val WarFalcon = card("War Falcon") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 1
    oracleText = "Flying\nThis creature can't attack unless you control a Knight or a Soldier."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantAttackUnless(
            Conditions.YouControl(
                GameObjectFilter.Permanent.withAnyOfSubtypes(listOf(Subtype.KNIGHT, Subtype.SOLDIER))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Volkan Baǵa"
        flavorText = "The falcon is the picture of nobility, maintaining a regal bearing even as it conducts the bloody business of war."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e092a0d-c031-4a76-86c1-7f83878a06e8.jpg?1783940511"
    }
}
