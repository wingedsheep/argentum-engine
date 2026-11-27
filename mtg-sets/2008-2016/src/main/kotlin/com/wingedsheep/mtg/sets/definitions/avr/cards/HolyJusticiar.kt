package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Holy Justiciar
 * {3}{W}
 * Creature — Human Cleric
 * 2/1
 * {2}{W}, {T}: Tap target creature. If that creature is a Zombie, exile it.
 *
 * The Warren Pilferers shape: the "if that creature is a Zombie" rider is a
 * [Conditions.TargetMatchesFilter] read off the target after it has been tapped.
 */
val HolyJusticiar = card("Holy Justiciar") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "{2}{W}, {T}: Tap target creature. If that creature is a Zombie, exile it."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE), creature),
            then = Effects.Exile(creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "David Rapoza"
        flavorText = "Her eyes see through the rotting flesh and glimpse a soul in agony, begging for the Blessed Sleep."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/640cad49-1db3-4611-a80d-7ce95f000fad.jpg?1783940731"
    }
}
