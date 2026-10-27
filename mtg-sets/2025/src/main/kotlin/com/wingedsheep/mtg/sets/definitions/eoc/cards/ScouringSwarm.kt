package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scouring Swarm — Edge of Eternities Commander #16 (canonical printing)
 * {1}{B}{G} · Creature — Insect · 1/1
 *
 * Flying
 * Whenever you sacrifice a land, create a tapped token that's a copy of this creature if seven or
 * more land cards are in your graveyard. Otherwise, create a tapped 1/1 black Insect creature
 * token with flying.
 *
 * Scute Swarm's shape: an [Effects.If] read at resolution (the ruling counts the land cards in
 * your graveyard as the ability resolves, usually including the sacrificed land), choosing between
 * a tapped self-copy and a tapped 1/1 flier. Per land sacrificed (CR 603.2c).
 */
val ScouringSwarm = card("Scouring Swarm") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Insect"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever you sacrifice a land, create a tapped token that's a copy of this creature if seven or " +
        "more land cards are in your graveyard. Otherwise, create a tapped 1/1 black Insect creature token with flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Land)
        effect = Effects.If(
            Conditions.CardsInGraveyardMatchingAtLeast(7, GameObjectFilter.Land),
            then = Effects.CreateTokenCopyOfTarget(target = EffectTarget.Self, tapped = true),
            otherwise = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Insect"),
                keywords = setOf(Keyword.FLYING),
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/2/1/2172a126-2ea6-4b5c-84c2-7879c1982a3a.jpg?1783905793"
            )
        )
        description = "Whenever you sacrifice a land, create a tapped token that's a copy of this creature if " +
            "seven or more land cards are in your graveyard. Otherwise, create a tapped 1/1 black Insect " +
            "creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Diana Franco"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fc25982-8668-43c4-93db-84e716e8783d.jpg?1783906062"

        ruling(
            "2025-07-25",
            "The token doesn't copy whether Scouring Swarm is tapped or untapped, whether it has any counters " +
                "on it or Auras or Equipment attached to it, or any non-copy effects that have changed its power, " +
                "toughness, types, color, and so on."
        )
        ruling(
            "2025-07-25",
            "Scouring Swarm's last ability counts the number of land cards in your graveyard as it resolves. " +
                "This will usually include the land you sacrificed."
        )
        ruling(
            "2025-07-25",
            "The token copy will have Scouring Swarm's abilities and will be able to create more copies of itself."
        )
        ruling(
            "2025-07-25",
            "If Scouring Swarm leaves the battlefield before its triggered ability resolves and seven or more land " +
                "cards are in your graveyard when the ability resolves, the token will still enter as a copy of " +
                "Scouring Swarm, using Scouring Swarm's copiable values from when it was last on the battlefield."
        )
    }
}
