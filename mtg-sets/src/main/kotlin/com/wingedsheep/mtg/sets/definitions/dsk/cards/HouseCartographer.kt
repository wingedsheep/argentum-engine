package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * House Cartographer
 * {1}{G}
 * Creature — Human Scout Survivor
 * 2/2
 * Survival — At the beginning of your second main phase, if this creature is tapped, reveal
 * cards from the top of your library until you reveal a land card. Put that card into your hand
 * and the rest on the bottom of your library in a random order.
 *
 * "Survival" is an ability word (no rules meaning) — modeled as a postcombat-main-phase trigger
 * ([Triggers.YourPostcombatMain]) with an intervening-if ([Conditions.SourceIsTapped], CR 603.4 —
 * checked both when it would trigger and on resolution). The reveal-until-land body reuses the
 * Clifftop Lookout pipeline (GatherUntilMatch → Reveal → Filter → Move), but lands the found card
 * in hand rather than onto the battlefield.
 */
val HouseCartographer = card("House Cartographer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout Survivor"
    power = 2
    toughness = 2
    oracleText = "Survival — At the beginning of your second main phase, if this creature is tapped, reveal cards from the top of your library until you reveal a land card. Put that card into your hand and the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.YourPostcombatMain
        triggerCondition = Conditions.SourceIsTapped
        effect = Effects.Composite(
            listOf(
                GatherUntilMatchEffect(
                    filter = GameObjectFilter.Land,
                    storeMatch = "revealedLand",
                    storeRevealed = "allRevealed"
                ),
                RevealCollectionEffect(from = "allRevealed"),
                // allRevealed includes the matched land, so subtract it before bottoming.
                FilterCollectionEffect(
                    from = "allRevealed",
                    filter = CollectionFilter.ExcludeOtherCollection("revealedLand"),
                    storeMatching = "nonLandRevealed"
                ),
                MoveCollectionEffect(
                    from = "revealedLand",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "nonLandRevealed",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
        description = "Survival — At the beginning of your second main phase, if this creature is " +
            "tapped, reveal cards from the top of your library until you reveal a land card. Put " +
            "that card into your hand and the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a534918-a009-4f7d-87c9-5ef600b6e7c2.jpg?1726286553"
    }
}
