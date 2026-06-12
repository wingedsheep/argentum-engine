package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thundertrap Trainer
 * {1}{U}
 * Creature — Otter Wizard
 * 1/2
 *
 * Offspring {4} (You may pay an additional {4} as you cast this spell.
 * If you do, when this creature enters, create a 1/1 token copy of it.)
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal a noncreature, nonland card from among them and put it
 * into your hand. Put the rest on the bottom of your library in a random order.
 */
val ThundertrapTrainer = card("Thundertrap Trainer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Otter Wizard"
    power = 1
    toughness = 2
    oracleText = "Offspring {4} (You may pay an additional {4} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhen this creature enters, look at the top four cards of your library. You may reveal a noncreature, nonland card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{4}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // ETB: look at top 4, may take a noncreature nonland card, rest on bottom
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)), name = "looked")
            val (kept, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsNoncreature,
                        CardPredicate.IsNonland
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                name = "kept",
                remainderName = "rest"
            )
            move(kept, CardDestination.ToZone(Zone.HAND), revealed = true)
            move(rest, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cf3af94-b7c8-415c-a5a1-d89967fd0bba.jpg?1721426312"
    }
}
