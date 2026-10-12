package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

private const val KARN_EMBLEM = "Tap an untapped artifact you control: This emblem deals 1 damage to any target."

/**
 * Karn, Living Legacy
 * {4}
 * Legendary Planeswalker — Karn
 * Loyalty 4
 * +1: Create a tapped Powerstone token.
 * −1: Pay any amount of mana. Look at that many cards from the top of your library, then put one
 * of those cards into your hand and the rest on the bottom of your library in a random order.
 * −7: You get an emblem with "Tap an untapped artifact you control: This emblem deals 1 damage to
 * any target."
 *
 * The −1's "pay any amount of mana" is [Effects.MayPayX]: paying 0 looks at no cards, which is
 * exactly the decline branch. The −7 emblem's ability is the emblem's *own*
 * (`ownedActivatedAbilities`), so the emblem is the source of the damage and the ability keeps
 * working after Karn is gone.
 */
val KarnLivingLegacy = card("Karn, Living Legacy") {
    val emblemPing = grantedActivatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Artifact)
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(1, anyTarget)
        description = KARN_EMBLEM
    }

    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Planeswalker — Karn"
    startingLoyalty = 4
    oracleText = "+1: Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")\n" +
        "−1: Pay any amount of mana. Look at that many cards from the top of your library, then put one of those cards into your hand and the rest on the bottom of your library in a random order.\n" +
        "−7: You get an emblem with \"$KARN_EMBLEM\""

    loyaltyAbility(+1) {
        effect = Effects.CreatePowerstone(tapped = true)
    }

    loyaltyAbility(-1) {
        effect = Effects.MayPayX(
            then = Patterns.Library.lookAtTopAndKeep(
                count = DynamicAmounts.xValue(),
                keepCount = DynamicAmounts.fixed(1),
                keepDestination = CardDestination.ToZone(Zone.HAND),
                restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                restOrder = CardOrder.Random
            )
        )
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            ownedActivatedAbilities = listOf(emblemPing),
            emblemDescription = KARN_EMBLEM
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e83b1b87-01d5-4624-8a2b-4b771e13e59c.jpg?1783921372"
    }
}
