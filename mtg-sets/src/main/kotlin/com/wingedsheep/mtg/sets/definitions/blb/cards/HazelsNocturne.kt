package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hazel's Nocturne
 * {3}{B}
 * Instant
 *
 * Return up to two target creature cards from your graveyard to your hand.
 * Each opponent loses 2 life and you gain 2 life.
 */
val HazelsNocturne = card("Hazel's Nocturne") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Return up to two target creature cards from your graveyard to your hand. " +
        "Each opponent loses 2 life and you gain 2 life."

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard,
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)),
        ).then(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)))
            .then(Effects.GainLife(2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Dominik Mayer"
        flavorText = "On stormy autumn days, the leaves in the trees chitter and whisper."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/239363df-4de8-4b64-80fc-a1f4b5c36027.jpg?1721426431"
    }
}
