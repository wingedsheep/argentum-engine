package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Splash Portal
 * {U}
 * Sorcery
 *
 * Exile target creature you control, then return it to the battlefield under its owner's
 * control. If that creature is a Bird, Frog, Otter, or Rat, draw a card.
 */
val SplashPortal = card("Splash Portal") {
    manaCost = "{U}"
    typeLine = "Sorcery"
    oracleText = "Exile target creature you control, then return it to the battlefield under its owner's control. If that creature is a Bird, Frog, Otter, or Rat, draw a card."

    spell {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = MoveToZoneEffect(creature, Zone.EXILE)
            .then(MoveToZoneEffect(creature, Zone.BATTLEFIELD))
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetMatchesFilter(
                        filter = GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.Or(
                                    listOf(
                                        CardPredicate.HasSubtype(Subtype("Bird")),
                                        CardPredicate.HasSubtype(Subtype("Frog")),
                                        CardPredicate.HasSubtype(Subtype("Otter")),
                                        CardPredicate.HasSubtype(Subtype("Rat"))
                                    )
                                )
                            )
                        ),
                        targetIndex = 0
                    ),
                    effect = Effects.DrawCards(1)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Caio Monteiro"
        flavorText = "\"Don't ask me how it works. If I stopped long enough to question it, I'd lose my legs.\"\n—Lorb, frogfolk wizard"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adbaa356-28ba-487f-930a-a957d9960ab0.jpg?1721426280"
    }
}
