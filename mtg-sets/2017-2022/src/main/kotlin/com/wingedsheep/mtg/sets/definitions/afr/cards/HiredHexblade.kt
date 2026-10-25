package com.wingedsheep.mtg.sets.definitions.afr.cards

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
 * Hired Hexblade
 * {1}{B}
 * Creature — Elf Warlock
 * 2/2
 * When this creature enters, if mana from a Treasure was spent to cast it, you draw a card and you
 * lose 1 life.
 *
 * The intervening "if" reads the permanent's recorded cast provenance
 * ([DynamicAmounts.manaSpentFromSubtype]), stamped onto its cast record when it resolved — merely
 * controlling a Treasure doesn't count, and a Hexblade put onto the battlefield without being cast
 * never triggers.
 */
val HiredHexblade = card("Hired Hexblade") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Warlock"
    oracleText = "When this creature enters, if mana from a Treasure was spent to cast it, you draw a card " +
        "and you lose 1 life."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Irina Nordsol"
        flavorText = "\"I'll take care of it. You just be sure to hold up your end of the deal.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c23cbf01-90d0-4962-92d5-5e1566e8ef4b.jpg?1783926493"
    }
}
