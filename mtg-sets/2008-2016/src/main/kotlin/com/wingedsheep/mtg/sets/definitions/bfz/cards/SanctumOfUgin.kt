package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Sanctum of Ugin
 * Land
 * {T}: Add {C}.
 * Whenever you cast a colorless spell with mana value 7 or greater, you may sacrifice this land.
 * If you do, search your library for a colorless creature card, reveal it, put it into your hand,
 * then shuffle.
 *
 * The sacrifice is part of the trigger's resolution, not a cost of the trigger: a [Effects.MayPay]
 * whose cost is [SacrificeSelfEffect], so the search only happens if the land is actually
 * sacrificed (if it has already left the battlefield, nothing is searched).
 */
val SanctumOfUgin = card("Sanctum of Ugin") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "Whenever you cast a colorless spell with mana value 7 or greater, you may sacrifice this land. " +
        "If you do, search your library for a colorless creature card, reveal it, put it into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless).manaValueAtLeast(7)
        )
        effect = Effects.MayPay(
            cost = SacrificeSelfEffect,
            then = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true,
                reveal = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86798d03-9f2d-46bd-a660-13c8dd5535ce.jpg?1783938173"
    }
}
