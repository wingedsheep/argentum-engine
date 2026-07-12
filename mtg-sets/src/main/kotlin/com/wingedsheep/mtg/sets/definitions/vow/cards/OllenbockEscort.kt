package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ollenbock Escort
 * {W}
 * Creature — Human Cleric
 * 1/1
 * Vigilance
 * Sacrifice this creature: Target creature you control with a +1/+1 counter on it gains lifelink
 * and indestructible until end of turn.
 */
val OllenbockEscort = card("Ollenbock Escort") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Vigilance\nSacrifice this creature: Target creature you control with a +1/+1 counter on it gains lifelink and indestructible until end of turn."
    power = 1
    toughness = 1
    keywords(Keyword.VIGILANCE)
    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target(
            "target",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.withCounter(Counters.PLUS_ONE_PLUS_ONE).youControl())
            )
        )
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.LIFELINK, t),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Eric Wilkerson"
        flavorText = "\"Stay in the light. I don't know what lurks in those shadows, and I'd like to keep it that way.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a659bf-9d85-4011-980e-c3a8dc4513e9.jpg?1782703176"
    }
}
