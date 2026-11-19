package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Font of Return
 * {1}{B}
 * Enchantment
 * {3}{B}, Sacrifice this enchantment: Return up to three target creature cards from your
 * graveyard to your hand.
 *
 * Hazel's Nocturne's "up to N" graveyard return behind a mana + sacrifice-self activated cost.
 */
val FontOfReturn = card("Font of Return") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "{3}{B}, Sacrifice this enchantment: Return up to three target creature cards " +
        "from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.SacrificeSelf)
        target = TargetObject(
            count = 3,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard,
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND),
        )
        description = "{3}{B}, Sacrifice this enchantment: Return up to three target creature cards " +
            "from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Daarken"
        flavorText = "Drink deep, and death will recoil from you."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c13eacff-0fd1-4e2e-8213-5bf5fff973c8.jpg?1783939433"
    }
}
