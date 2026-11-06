package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wander in Death
 * {2}{B}
 * Sorcery
 * Return up to two target creature cards from your graveyard to your hand.
 * Cycling {2}
 */
val WanderInDeath = card("Wander in Death") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards from your graveyard to your hand.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Seb McKinnon"
        flavorText = "Forever restless. Forever growing in number."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aea7ad5c-d9b9-4b12-90dd-c02399d73b26.jpg?1783936496"
    }
}
