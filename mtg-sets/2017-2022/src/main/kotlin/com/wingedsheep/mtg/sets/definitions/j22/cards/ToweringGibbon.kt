package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Towering Gibbon
 * {3}{G}
 * Creature — Ape
 * * / 4
 * Reach
 * Towering Gibbon's power is equal to the greatest mana value among creatures you control.
 *
 * A characteristic-defining power (CR 604.3), spelled as `dynamicPower(...)` (Sylvan Yeti):
 * the MAX mana value over the creatures you control — which includes the Gibbon itself.
 */
val ToweringGibbon = card("Towering Gibbon") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ape"
    oracleText = "Reach\nTowering Gibbon's power is equal to the greatest mana value among creatures you control."
    toughness = 4

    dynamicPower(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxManaValue())

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Chris Seaman"
        flavorText = "\"Don't worry, folks. We're perfectly safe here. That's a banana processing plant.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b14a22aa-0f79-4497-a7ff-08d92d14bc0f.jpg"
    }
}
