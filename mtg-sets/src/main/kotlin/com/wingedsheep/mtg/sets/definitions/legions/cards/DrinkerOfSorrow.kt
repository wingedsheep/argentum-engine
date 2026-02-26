package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Drinker of Sorrow
 * {2}{B}
 * Creature — Horror
 * 5/3
 * Drinker of Sorrow can't block.
 * Whenever Drinker of Sorrow deals combat damage, sacrifice a permanent.
 */
val DrinkerOfSorrow = card("Drinker of Sorrow") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Horror"
    power = 5
    toughness = 3
    oracleText = "Drinker of Sorrow can't block.\nWhenever Drinker of Sorrow deals combat damage, sacrifice a permanent."

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamage
        effect = SacrificeEffect(GameObjectFilter.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Carl Critchlow"
        flavorText = "It strikes at your soul, heedless of your prayers."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bc8758b-68cc-45ab-85d0-b870cef7dd85.jpg?1562903921"
    }
}
