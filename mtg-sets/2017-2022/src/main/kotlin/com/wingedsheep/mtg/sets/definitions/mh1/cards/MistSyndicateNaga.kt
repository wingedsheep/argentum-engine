package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Mist-Syndicate Naga — Modern Horizons #58 (canonical printing)
 * {2}{U} · Creature — Snake Ninja · 3/1
 *
 * Ninjutsu {2}{U}
 * Whenever this creature deals combat damage to a player, create a token that's a copy of this
 * creature.
 *
 * [Effects.CreateTokenCopyOfSelf] copies the source's copiable values, falling back to its
 * last-known snapshot if the Naga left the battlefield before the trigger resolved (2019-06-14
 * ruling). The token carries this trigger too, so it makes copies of itself.
 */
val MistSyndicateNaga = card("Mist-Syndicate Naga") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Ninja"
    power = 3
    toughness = 1
    oracleText = "Ninjutsu {2}{U} ({2}{U}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, create a token that's a copy of " +
        "this creature."

    ninjutsu("{2}{U}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTokenCopyOfSelf()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65c8f074-e260-4f8e-85d1-1904ca928b51.jpg?1783933143"
        ruling(
            "2019-06-14",
            "If Mist-Syndicate Naga leaves the battlefield before its triggered ability resolves, the token " +
                "will still enter the battlefield as a copy of Mist-Syndicate Naga, using Mist-Syndicate " +
                "Naga's copiable values from when it was last on the battlefield."
        )
    }
}
