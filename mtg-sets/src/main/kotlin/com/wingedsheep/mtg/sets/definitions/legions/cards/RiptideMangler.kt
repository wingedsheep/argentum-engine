package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Riptide Mangler
 * {1}{U}
 * Creature — Beast
 * 0/3
 * {1}{U}: Change this creature's base power to target creature's power.
 * (This effect lasts indefinitely.)
 */
val RiptideMangler = card("Riptide Mangler") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Beast"
    power = 0
    toughness = 3
    oracleText = "{1}{U}: Change this creature's base power to target creature's power. (This effect lasts indefinitely.)"

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{1}{U}"))
        target("target creature", Targets.Creature)
        effect = Effects.SetBasePower(
            target = EffectTarget.Self,
            power = DynamicAmount.TargetPower(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Arnie Swekel"
        flavorText = "It wants you to be its chum."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5314a802-85d6-4d7b-ae9a-ca64eec652cf.jpg?1562911887"
    }
}
