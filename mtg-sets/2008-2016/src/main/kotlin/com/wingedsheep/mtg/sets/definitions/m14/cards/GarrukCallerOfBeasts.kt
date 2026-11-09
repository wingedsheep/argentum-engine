package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SearchDestination

private const val GARRUK_EMBLEM = "Whenever you cast a creature spell, you may search your library for a " +
    "creature card, put it onto the battlefield, then shuffle."

/**
 * Garruk, Caller of Beasts — Magic 2014 #172
 * {4}{G}{G} · Legendary Planeswalker — Garruk · Starting loyalty 4
 *
 * +1: Reveal the top five cards of your library. Put all creature cards revealed this way into
 *     your hand and the rest on the bottom of your library in any order.
 * −3: You may put a green creature card from your hand onto the battlefield.
 * −7: You get an emblem with "Whenever you cast a creature spell, you may search your library for
 *     a creature card, put it onto the battlefield, then shuffle."
 *
 * The +1 is Ajani Unyielding's full reveal with the remainder ordered by the controller. The −3
 * picks the card as it resolves (ruling) and may pick none. The emblem is a global triggered
 * ability, so it outlives Garruk and resolves above the creature spell that caused it; the
 * fetched creature isn't cast, so it doesn't retrigger the emblem.
 */
val GarrukCallerOfBeasts = card("Garruk, Caller of Beasts") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 4
    oracleText = "+1: Reveal the top five cards of your library. Put all creature cards revealed this way " +
        "into your hand and the rest on the bottom of your library in any order.\n" +
        "−3: You may put a green creature card from your hand onto the battlefield.\n" +
        "−7: You get an emblem with \"$GARRUK_EMBLEM\""

    loyaltyAbility(+1) {
        effect = Patterns.Library.revealTopPutAllMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Creature,
            restOrder = CardOrder.ControllerChooses,
        )
        description = "Reveal the top five cards of your library. Put all creature cards revealed this way " +
            "into your hand and the rest on the bottom of your library in any order."
    }

    loyaltyAbility(-3) {
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.withColor(Color.GREEN),
            count = 1,
            prompt = "You may put a green creature card from your hand onto the battlefield.",
        )
        description = "You may put a green creature card from your hand onto the battlefield."
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.Creature)
                effect = Effects.May(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Creature,
                        destination = SearchDestination.BATTLEFIELD,
                    )
                )
                description = GARRUK_EMBLEM
            },
            emblem = true,
            descriptionOverride = GARRUK_EMBLEM,
        )
        description = "You get an emblem with \"$GARRUK_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "172"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a96d0c67-e9f4-46d9-bd74-13a8606fdfe3.jpg?1783939906"
        ruling(
            "2013-07-01",
            "You choose what green creature card to put onto the battlefield, if any, when the second ability resolves.",
        )
        ruling(
            "2013-07-01",
            "If you have Garruk's emblem, the creature card you search for will be put onto the battlefield before " +
                "the creature spell you cast resolves. The creature card you put onto the battlefield isn't cast, " +
                "so the ability of the emblem won't retrigger.",
        )
    }
}
