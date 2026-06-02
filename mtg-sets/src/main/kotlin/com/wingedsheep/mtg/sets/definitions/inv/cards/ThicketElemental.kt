package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.dsl.Effects

/**
 * Thicket Elemental
 * {3}{G}{G}
 * Creature — Elemental
 * 4/4
 * Kicker {1}{G}
 * When this creature enters, if it was kicked, you may reveal cards from the top of your library
 * until you reveal a creature card. If you do, put that card onto the battlefield and shuffle all
 * other cards revealed this way into your library.
 */
val ThicketElemental = card("Thicket Elemental") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "Kicker {1}{G} (You may pay an additional {1}{G} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, you may reveal cards from the top of your library " +
        "until you reveal a creature card. If you do, put that card onto the battlefield and shuffle all " +
        "other cards revealed this way into your library."

    keywordAbility(KeywordAbility.kicker("{1}{G}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = MayEffect(
            Effects.Composite(
                listOf(
                    GatherUntilMatchEffect(
                        filter = GameObjectFilter.Creature,
                        storeMatch = "found",
                        storeRevealed = "allRevealed"
                    ),
                    // fromZone/toZone tag this as a zone-transition reveal so the client shows
                    // the full reveal overlay including the matched creature (which lands on the
                    // battlefield in the same update).
                    RevealCollectionEffect(
                        from = "allRevealed",
                        fromZone = Zone.LIBRARY,
                        toZone = Zone.BATTLEFIELD
                    ),
                    MoveCollectionEffect(
                        from = "found",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                    ),
                    ShuffleLibraryEffect()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f80a56ed-3ebb-4e20-bf6a-e27127f762e8.jpg?1562945003"
    }
}
