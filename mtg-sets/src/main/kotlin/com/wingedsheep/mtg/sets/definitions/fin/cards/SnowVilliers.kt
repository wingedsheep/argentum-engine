package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Snow Villiers — Final Fantasy #33
 * {2}{W} · Legendary Creature — Human Rebel Monk · * /3
 *
 * Vigilance
 * Snow Villiers's power is equal to the number of creatures you control.
 *
 * Power is a characteristic-defining ability (printed *), recomputed continuously via
 * `dynamicPower(...)` over the count of creatures you control (read through projected
 * control). Snow himself is a creature you control, so he counts toward his own power. Toughness
 * is the printed fixed value 3, so only `dynamicPower` is set.
 */
val SnowVilliers = card("Snow Villiers") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Rebel Monk"
    dynamicPower(
        DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
    )
    toughness = 3
    oracleText = "Vigilance\nSnow Villiers's power is equal to the number of creatures you control."

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Fariba Khamseh"
        flavorText = "\"All I can do is go forward. Keep fighting and surviving, until I find the answers I need.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/9/399bb699-e61d-4b41-b6e9-e594cbad6194.jpg?1748705880"
    }
}
