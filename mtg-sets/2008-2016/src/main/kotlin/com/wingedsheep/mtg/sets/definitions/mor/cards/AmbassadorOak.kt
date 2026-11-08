package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ambassador Oak
 * {3}{G}
 * Creature — Treefolk Warrior
 * 3/3
 * When this creature enters, create a 1/1 green Elf Warrior creature token.
 */
val AmbassadorOak = card("Ambassador Oak") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk Warrior"
    oracleText = "When this creature enters, create a 1/1 green Elf Warrior creature token."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Steve Prescott"
        flavorText = "Treefolk and elves share a common interest in the forests, but really the elves just feel at home with the view from above."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/904d3416-70bf-4043-8078-f5f033ecfb31.jpg?1783942781"
    }
}
