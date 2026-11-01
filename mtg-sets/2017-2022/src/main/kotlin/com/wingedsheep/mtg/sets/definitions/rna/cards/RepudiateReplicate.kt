package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Repudiate // Replicate (RNA 227) — split-layout spell (CR 709).
 *
 * Repudiate {G/U}{G/U} — Instant
 *   Counter target activated or triggered ability. (Mana abilities can't be targeted.)
 *
 * Replicate {1}{G}{U} — Sorcery
 *   Create a token that's a copy of target creature you control.
 */
val RepudiateReplicate = card("Repudiate // Replicate") {
    layout = CardLayout.SPLIT
    colorIdentity = "GU"

    face("Repudiate") {
        manaCost = "{G/U}{G/U}"
        typeLine = "Instant"
        oracleText = "Counter target activated or triggered ability. (Mana abilities can't be targeted.)"

        spell {
            target(TargetFilter.ActivatedOrTriggeredAbilityOnStack)
            effect = Effects.CounterAbility()
        }
    }

    face("Replicate") {
        manaCost = "{1}{G}{U}"
        typeLine = "Sorcery"
        oracleText = "Create a token that's a copy of target creature you control."

        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.CreateTokenCopyOfTarget(creature)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6200937-3146-4972-ab83-051ade3b7a52.jpg?1783933626"
        ruling(
            "2024-01-12",
            "Abilities that create replacement effects, such as a permanent entering the battlefield tapped or with counters on it, can't be targeted."
        )
        ruling(
            "2024-01-12",
            "The token copies exactly what was printed on the original creature and nothing else (unless that creature is copying something else or is a token). " +
                "It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects."
        )
    }
}
