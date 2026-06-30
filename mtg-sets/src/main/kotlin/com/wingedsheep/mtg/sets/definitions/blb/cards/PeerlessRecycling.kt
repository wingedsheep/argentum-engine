package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Peerless Recycling
 * {1}{G}
 * Instant
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 * Return target permanent card from your graveyard to your hand.
 * If the gift was promised, instead return two target permanent cards
 * from your graveyard to your hand.
 */
val PeerlessRecycling = card("Peerless Recycling") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nReturn target permanent card from your graveyard to your hand. If the gift was promised, instead return two target permanent cards from your graveyard to your hand."

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 0: No gift — return 1 target permanent card from graveyard to hand
            Mode.withTarget(
                effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                target = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
                ),
                description = "Return target permanent card from your graveyard to your hand"
            ),
            // Mode 1: Gift — opponent draws a card, return 2 target permanent cards to hand
            Mode(
                effect = Effects.Composite(listOf(
                    DrawCardsEffect(1, EffectTarget.PlayerRef(Player.AnOpponent)),
                    Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                    Effects.ReturnToHand(EffectTarget.ContextTarget(1)),
                    Effects.GiftGiven()
                )),
                targetRequirements = listOf(
                    TargetObject(
                        count = 2,
                        filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
                    )
                ),
                description = "Gift a card — return two target permanent cards from your graveyard to your hand"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f72466c-505b-4371-9366-0fde525a37e6.jpg?1721426897"
    }
}
