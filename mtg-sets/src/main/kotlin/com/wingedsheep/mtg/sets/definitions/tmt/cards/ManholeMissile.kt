package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Manhole Missile
 * {1}{R}
 * Instant
 *
 * Manhole Missile deals 3 damage to target creature. You may put a
 * card from your hand on the bottom of your library. If you do, draw
 * a card.
 */
val ManholeMissile = card("Manhole Missile") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Manhole Missile deals 3 damage to target creature. You may put a card from your hand on the bottom of your library. If you do, draw a card."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Pipeline {
            run(Effects.DealDamage(3, creature))
            val hand = gather(
                CardSource.FromZone(Zone.HAND, Player.You),
                name = "hand"
            )
            val (bottomed, kept) = chooseUpToSplit(
                1, from = hand,
                selectedLabel = "Put on bottom of library",
                remainderLabel = "Keep in hand",
                name = "bottomed",
                remainderName = "kept"
            )
            move(
                bottomed,
                CardDestination.ToZone(
                    Zone.LIBRARY,
                    placement = ZonePlacement.Bottom
                )
            )
            run(
                ConditionalOnCollectionEffect(
                    collection = "bottomed",
                    ifNotEmpty = Effects.DrawCards(1),
                    ifEmpty = Effects.Composite(listOf())
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Fajareka Setiawan"
        flavorText = "\"SAY YER PRAYERS, TOITLES!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/243e392b-48b0-4e90-ae22-9a696f45e878.jpg?1771502635"
    }
}
