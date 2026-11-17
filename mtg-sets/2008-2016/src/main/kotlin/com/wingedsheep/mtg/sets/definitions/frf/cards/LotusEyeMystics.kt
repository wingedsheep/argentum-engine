package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lotus-Eye Mystics
 * {3}{W}
 * Creature — Human Monk
 * 3/2
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * When this creature enters, return target enchantment card from your graveyard to your hand.
 *
 * The return is mandatory ("return", not "you may return"); with no enchantment card in your
 * graveyard the trigger has no legal target and is removed from the stack.
 */
val LotusEyeMystics = card("Lotus-Eye Mystics") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 3
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, return target enchantment card from your graveyard to your hand."

    prowess()

    triggeredAbility {
        trigger = Triggers.self.enters()
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(enchantment)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Dan Murayama Scott"
        flavorText = "Every action has a foreseen purpose."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8bf97a0-2b90-4006-8a54-7795dbc0dba1.jpg?1783938711"
    }
}
