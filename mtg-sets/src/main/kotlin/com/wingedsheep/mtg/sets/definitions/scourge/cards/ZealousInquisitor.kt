package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zealous Inquisitor
 * {2}{W}
 * Creature — Human Cleric
 * 2/2
 * {1}{W}: The next 1 damage that would be dealt to this creature this turn
 * is dealt to target creature instead.
 */
val ZealousInquisitor = card("Zealous Inquisitor") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{1}{W}: The next 1 damage that would be dealt to this creature this turn is dealt to target creature instead."

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val creature = target("creature", Targets.Creature)
        effect = RedirectNextDamageEffect(
            protectedTargets = listOf(EffectTarget.Self),
            redirectTo = creature,
            amount = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Wayne England"
        flavorText = "\"I only return what rightly belongs to another.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fb821b6-4e73-4970-b1ac-b67c93990ec0.jpg?1562526171"
    }
}
