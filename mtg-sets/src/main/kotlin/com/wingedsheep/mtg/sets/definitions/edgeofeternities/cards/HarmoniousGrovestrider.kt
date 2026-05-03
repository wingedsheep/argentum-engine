package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Harmonious Grovestrider
 * {3}{G}{G}
 * Creature — Beast
 * Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)
 * Harmonious Grovestrider's power and toughness are each equal to the number of lands you control.
 */
val HarmoniousGrovestrider = card("Harmonious Grovestrider") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Beast"
    dynamicStats(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).count()
    )
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\nHarmonious Grovestrider's power and toughness are each equal to the number of lands you control."

    // Ward ability
    staticAbility { ability = GrantWard(WardCost.Mana("{2}")) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Ron Spencer"
        flavorText = "Beyond the borders of Pinnacle space, alien worlds of untold wonder await."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e320abed-f145-42d3-b402-4f82e3a56389.jpg?1752947325"
    }
}
