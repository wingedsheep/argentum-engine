package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chaos Maw
 * {5}{R}{R}
 * Creature — Hellion
 * 6/6
 *
 * When this creature enters, it deals 3 damage to each other creature.
 */
val ChaosMaw = card("Chaos Maw") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    oracleText = "When this creature enters, it deals 3 damage to each other creature."
    power = 6
    toughness = 6

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.dealDamageToAll(3, GroupFilter.AllOtherCreatures)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Steve Argyle"
        flavorText = "The food chain doesn't end at sandwurms."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8094908b-1d3a-42df-bc27-8456fbb98e65.jpg?1783936032"
    }
}
