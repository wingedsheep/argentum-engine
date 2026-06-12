package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Anticausal Vestige — {6}
 * Creature — Eldrazi
 * 7/5
 * When this creature leaves the battlefield, draw a card, then you may put a permanent card
 * with mana value less than or equal to the number of lands you control from your hand onto
 * the battlefield tapped.
 * Warp {4}
 */
val AnticausalVestige = card("Anticausal Vestige") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    oracleText = "When this creature leaves the battlefield, draw a card, then you may put a permanent card with mana value less than or equal to the number of lands you control from your hand onto the battlefield tapped.\nWarp {4} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 7
    toughness = 5

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.Pipeline {
            run(DrawCardsEffect(1))
            val handPermanents = gather(
                CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Permanent),
                name = "hand_permanents"
            )
            val eligiblePermanents = filter(
                handPermanents,
                CollectionFilter.ManaValueAtMost(
                    DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
                ),
                name = "eligible_permanents"
            )
            val chosenPermanent = chooseUpTo(
                1, from = eligiblePermanents,
                prompt = "Choose a permanent card to put onto the battlefield (optional)",
                name = "chosen_permanent"
            )
            move(
                chosenPermanent,
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped)
            )
        }
        description = "When this creature leaves the battlefield, draw a card, then you may put a permanent card with mana value less than or equal to the number of lands you control from your hand onto the battlefield tapped."
    }

    warp = "{4}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35372b69-6086-44e0-9f7c-681e362e5142.jpg?1752946556"
    }
}
