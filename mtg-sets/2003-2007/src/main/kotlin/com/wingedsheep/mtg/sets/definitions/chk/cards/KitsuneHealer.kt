package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kitsune Healer
 * {3}{W}
 * Creature — Fox Cleric
 * 2/2
 * {T}: Prevent the next 1 damage that would be dealt to any target this turn.
 * {T}: Prevent all damage that would be dealt to target legendary creature this turn.
 *
 * Two independent tap abilities: the Samite Healer shield on any target, and the Indestructible
 * Aura shield restricted to a legendary creature.
 */
val KitsuneHealer = card("Kitsune Healer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Cleric"
    oracleText = "{T}: Prevent the next 1 damage that would be dealt to any target this turn.\n" +
        "{T}: Prevent all damage that would be dealt to target legendary creature this turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        val t = target(Targets.Any)
        effect = Effects.PreventNextDamage(1, t)
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.legendary())
        effect = Effects.PreventDamage(target = creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11bb6894-b56a-4a63-859a-65a0c2d160fc.jpg?1783944336"
    }
}
