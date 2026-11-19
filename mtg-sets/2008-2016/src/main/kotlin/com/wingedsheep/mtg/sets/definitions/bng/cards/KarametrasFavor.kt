package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Karametra's Favor
 * {1}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature has "{T}: Add one mana of any color."
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val KarametrasFavor = card("Karametra's Favor") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted creature has \"{T}: Add one mana of any color.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                effect = Effects.AddManaOfChoice()
                manaAbility = true
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Chase Stone"
        flavorText = "The harvest god's cornucopia contains the fruits of the fields, the forest, and beyond."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc036932-38c2-42df-9922-2d901028f2b8.jpg?1783939535"
    }
}
