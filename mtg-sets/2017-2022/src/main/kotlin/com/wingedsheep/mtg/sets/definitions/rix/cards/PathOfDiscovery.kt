package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Path of Discovery
 * {3}{G}
 * Enchantment
 * Whenever a creature you control enters, it explores. (Reveal the top card of your library.
 * Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on the creature,
 * then put the card back or put it into your graveyard.)
 */
val PathOfDiscovery = card("Path of Discovery") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on the creature, then put the card back or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.Explore(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad9d5518-34ea-418d-b34d-74d773db8bcb.jpg?1555040716"
        ruling("2018-01-19", "Path of Discovery's triggered ability triggers along with any other abilities that say that the creature explores when it enters the battlefield, including abilities that come from the creature itself or from multiples of Path of Discovery. You may take actions between each resolving ability's exploration.")
    }
}
