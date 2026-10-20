package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Presence of the Master
 * {3}{W}
 * Enchantment
 * Whenever a player casts an enchantment spell, counter it.
 *
 * A cast trigger for every player (the caster included) that counters the triggering spell —
 * enchantments put onto the battlefield without being cast slip past it.
 */
val PresenceOfTheMaster = card("Presence of the Master") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a player casts an enchantment spell, counter it."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Enchantment)
        effect = Effects.CounterTriggeringSpell()
        description = "Whenever a player casts an enchantment spell, counter it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cb86b2f-116d-4952-b35a-1398341baaf5.jpg?1783948082"
        ruling("2004-10-04", "Does not stop enchantments from being put onto the battlefield by a spell or ability.")
        ruling("2004-10-04", "This effect triggers when the spell is announced and prior to allowing responses to it.")
    }
}
