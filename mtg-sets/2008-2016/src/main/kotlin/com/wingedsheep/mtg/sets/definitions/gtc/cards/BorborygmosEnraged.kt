package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Borborygmos Enraged
 * {4}{R}{R}{G}{G}
 * Legendary Creature — Cyclops
 * 7/6
 * Trample
 * Whenever Borborygmos Enraged deals combat damage to a player, reveal the top three cards of
 * your library. Put all land cards revealed this way into your hand and the rest into your
 * graveyard.
 * Discard a land card: Borborygmos Enraged deals 3 damage to any target.
 *
 * The combat-damage trigger is Mulch's recipe ([Patterns.Library.revealTopPutAllMatchingToHand]):
 * gather the top three, reveal them, and partition lands to hand and the rest to the graveyard in
 * their gathered order — no choice, so nothing pauses for input.
 */
val BorborygmosEnraged = card("Borborygmos Enraged") {
    manaCost = "{4}{R}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Cyclops"
    oracleText = "Trample\n" +
        "Whenever Borborygmos Enraged deals combat damage to a player, reveal the top three cards of your library. Put all land cards revealed this way into your hand and the rest into your graveyard.\n" +
        "Discard a land card: Borborygmos Enraged deals 3 damage to any target."
    power = 7
    toughness = 6

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.revealTopPutAllMatchingToHand(
            count = 3,
            filter = GameObjectFilter.Land,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    activatedAbility {
        cost = Costs.Discard(GameObjectFilter.Land)
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, anyTarget)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "147"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8644c60f-7d06-4026-bcf3-df054701ca0a.jpg"
    }
}
