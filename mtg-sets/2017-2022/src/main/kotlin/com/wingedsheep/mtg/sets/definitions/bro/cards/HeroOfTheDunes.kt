package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hero of the Dunes
 * {3}{W}{B}
 * Creature — Human Soldier
 * 3/2
 * When this creature enters, return target artifact or creature card with mana value 3 or less
 * from your graveyard to the battlefield.
 * Creatures you control with mana value 3 or less get +1/+0.
 */
val HeroOfTheDunes = card("Hero of the Dunes") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, return target artifact or creature card with mana value 3 or less from your graveyard to the battlefield.\n" +
        "Creatures you control with mana value 3 or less get +1/+0."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(
            TargetFilter(
                (GameObjectFilter.Artifact or GameObjectFilter.Creature).manaValueAtMost(3).ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter(GameObjectFilter.Creature.manaValueAtMost(3).youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Taras Susak"
        flavorText = "History forgot her name but not her victories."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c6c72d5-f08e-4e46-a5cb-26d1fc0a71c8.jpg?1783920028"
    }
}
