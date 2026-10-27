package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Niko Aris
 * {X}{W}{U}{U}
 * Legendary Planeswalker — Niko
 * Loyalty 3
 * When Niko Aris enters, create X Shard tokens. (They're enchantments with "{2}, Sacrifice this
 * token: Scry 1, then draw a card.")
 * +1: Up to one target creature you control can't be blocked this turn. Whenever that creature
 * deals damage this turn, return it to its owner's hand.
 * −1: Niko Aris deals 2 damage to target tapped creature for each card you've drawn this turn.
 * −1: Create a Shard token.
 *
 * - The enters trigger reads [DynamicAmounts.castX] — the X Niko was cast with, which rides onto
 *   the permanent. Put onto the battlefield without being cast, X is 0 and no Shards are made
 *   (printed ruling).
 * - The +1's rider is a delayed triggered ability (CR 603.7) scoped to the targeted creature via
 *   `watchedTarget`, firing on *any* damage it deals this turn, not just combat damage (printed
 *   ruling). Choosing no target resolves no watched creature, so no watcher is created.
 * - The first −1 deals all its damage in one event: `2 × cards drawn this turn`.
 */
val NikoAris = card("Niko Aris") {
    manaCost = "{X}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Niko"
    startingLoyalty = 3
    oracleText = "When Niko Aris enters, create X Shard tokens. (They're enchantments with \"{2}, " +
        "Sacrifice this token: Scry 1, then draw a card.\")\n" +
        "+1: Up to one target creature you control can't be blocked this turn. Whenever that " +
        "creature deals damage this turn, return it to its owner's hand.\n" +
        "−1: Niko Aris deals 2 damage to target tapped creature for each card you've drawn this turn.\n" +
        "−1: Create a Shard token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePredefinedToken("Shard", DynamicAmounts.castX())
        description = "When Niko Aris enters, create X Shard tokens."
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.CreatureYouControl, optional = true)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature) then
            Effects.CreateDelayedTrigger(
                effect = Effects.ReturnToHand(creature),
                trigger = Triggers.self.dealsDamage(),
                watchedTarget = creature,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        description = "Up to one target creature you control can't be blocked this turn. " +
            "Whenever that creature deals damage this turn, return it to its owner's hand."
    }

    loyaltyAbility(-1) {
        val creature = target(TargetFilter.TappedCreature)
        effect = Effects.DealDamage(DynamicAmounts.cardsDrawnThisTurn() * 2, creature)
        description = "Niko Aris deals 2 damage to target tapped creature for each card you've " +
            "drawn this turn."
    }

    loyaltyAbility(-1) {
        effect = Effects.CreateShard(1)
        description = "Create a Shard token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "225"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c4e9cfe-e72e-46a1-a8d8-9fda5f2bae73.jpg?1783928191"
        ruling("2021-02-05", "If Niko Aris enters the battlefield without being cast, they were cast for a cost other than their mana cost, or if X was chosen to be 0, the enters-the-battlefield ability won't create any Shard tokens.")
        ruling("2021-02-05", "The delayed triggered ability created by Niko's first loyalty ability will trigger whenever the target creature deals any damage that turn, not just combat damage.")
        ruling("2021-02-05", "Niko deals all the damage for their second ability at the same time. For example, if you've drawn two cards this turn, Niko will deal 4 damage to the target tapped creature; they won't deal 2 damage, then deal 2 more damage.")
    }
}
