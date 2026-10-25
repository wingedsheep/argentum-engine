package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jaded Sell-Sword
 * {3}{R}
 * Creature — Dragon Warrior
 * 4/3
 * When this creature enters, if mana from a Treasure was spent to cast it, it gains first strike
 * and haste until end of turn.
 *
 * The intervening "if" reads the permanent's recorded cast provenance, the same check Hired
 * Hexblade uses — a Sell-Sword put onto the battlefield without being cast never triggers.
 */
val JadedSellSword = card("Jaded Sell-Sword") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon Warrior"
    oracleText = "When this creature enters, if mana from a Treasure was spent to cast it, it gains " +
        "first strike and haste until end of turn."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Randy Vargas"
        flavorText = "\"Today? That'll cost you extra.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5219c0-fb43-47b1-b682-131ba7c70990.jpg?1783926475"
    }
}
