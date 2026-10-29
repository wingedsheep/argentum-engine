package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Masked Blackguard
 * {1}{B}
 * Creature — Human Rogue
 * 2/1
 * Flash
 * {2}{B}: This creature gets +1/+1 until end of turn.
 */
val MaskedBlackguard = card("Masked Blackguard") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "{2}{B}: This creature gets +1/+1 until end of turn."

    keywords(Keyword.FLASH)

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Bryan Sola"
        flavorText = "Her footsteps are whispers, her passage a last breath."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d61b4b71-3cbb-4422-8ce7-657ca3bb6a82.jpg?1783930702"
    }
}
