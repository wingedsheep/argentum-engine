package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thraben Sentry // Thraben Militia (Innistrad #38)
 * {3}{W}
 * Creature — Human Soldier 2/2 // Creature — Human Soldier 5/4
 *
 * Front — "Vigilance
 *          Whenever another creature you control dies, you may transform this creature."
 * Back  — "Trample"
 *
 * The back face has no mana cost, so its white comes from a color indicator (CR 204).
 */
private val ThrabenSentryFront = card("Thraben Sentry") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\nWhenever another creature you control dies, you may transform this creature."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
        description = "Whenever another creature you control dies, you may transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "David Rapoza"
        flavorText = "\"Looks like it's going to be a quiet night.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58ae9cbc-d88d-42df-ab76-63ab5d05c023.jpg?1783940988"
    }
}

private val ThrabenMilitia = card("Thraben Militia") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Soldier"
    power = 5
    toughness = 4
    oracleText = "Trample"

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "David Rapoza"
        flavorText = "\"The time for grief is over. The time for retribution has begun.\""
        imageUri = "https://cards.scryfall.io/normal/back/5/8/58ae9cbc-d88d-42df-ab76-63ab5d05c023.jpg?1783940988"
    }
}

val ThrabenSentry: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ThrabenSentryFront,
    backFace = ThrabenMilitia,
)
