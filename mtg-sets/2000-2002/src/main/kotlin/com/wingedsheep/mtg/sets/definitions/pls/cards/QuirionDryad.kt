package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quirion Dryad — Planeshift #89 (canonical printing)
 * {1}{G} · Creature — Dryad · 1/1
 *
 * Whenever you cast a spell that's white, blue, black, or red, put a +1/+1 counter on this creature.
 *
 * The Questing Druid shape: one cast trigger over a [CardPredicate.Or] of the four colors, so a
 * multicolored spell triggers it only once (ruling) and a green-only or colorless spell not at all.
 */
val QuirionDryad = card("Quirion Dryad") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 1
    oracleText = "Whenever you cast a spell that's white, blue, black, or red, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.withCardPredicate(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.HasColor(Color.WHITE),
                        CardPredicate.HasColor(Color.BLUE),
                        CardPredicate.HasColor(Color.BLACK),
                        CardPredicate.HasColor(Color.RED),
                    ),
                ),
            )
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Don Hazeltine"
        flavorText = "\"Never underestimate the ability of natural forces to adapt to unnatural influences.\"\n—Molimo, maro-sorcerer"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6841ae6-b15f-488e-9cae-2cc5ec668278.jpg?1783945611"
        ruling("2020-06-23", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-06-23", "If you cast a spell that's more than one color, Quirion Dryad's ability triggers just once.")
    }
}
