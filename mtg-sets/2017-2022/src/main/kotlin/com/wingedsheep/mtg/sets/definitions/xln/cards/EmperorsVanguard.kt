package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emperor's Vanguard
 * {3}{G}
 * Creature — Human Scout
 * 4/3
 * Whenever this creature deals combat damage to a player, it explores.
 */
val EmperorsVanguard = card("Emperor's Vanguard") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 4
    toughness = 3
    oracleText = "Whenever this creature deals combat damage to a player, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f10c22b9-bdb7-4933-a58a-c53d040ae3bb.jpg?1783935726"
    }
}
