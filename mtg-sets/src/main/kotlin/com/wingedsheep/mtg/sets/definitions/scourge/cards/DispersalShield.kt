package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dispersal Shield
 * {1}{U}
 * Instant
 * Counter target spell if its mana value is less than or equal to the
 * greatest mana value among permanents you control.
 */
val DispersalShield = card("Dispersal Shield") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell if its mana value is less than or equal to the greatest mana value among permanents you control."

    spell {
        target = Targets.Spell
        effect = ConditionalEffect(
            condition = Conditions.TargetSpellManaValueAtMost(
                DynamicAmounts.battlefield(Player.You).maxManaValue()
            ),
            effect = Effects.CounterSpell()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Dave Dorman"
        flavorText = "Maybe next time."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c257df6-f275-40db-bfe3-a9291356cdf7.jpg?1562525399"
    }
}
