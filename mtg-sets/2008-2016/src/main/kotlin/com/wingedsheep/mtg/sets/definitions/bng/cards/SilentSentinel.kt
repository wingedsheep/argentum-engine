package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silent Sentinel
 * {5}{W}{W}
 * Creature — Archon
 * 4/6
 *
 * Flying
 * Whenever this creature attacks, you may return target enchantment card from your graveyard to the
 * battlefield.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing (reprinted in C15 and C18).
 */
val SilentSentinel = card("Silent Sentinel") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Archon"
    power = 4
    toughness = 6
    oracleText = "Flying\nWhenever this creature attacks, you may return target enchantment card from " +
        "your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.May(Effects.PutOntoBattlefield(enchantment, fromZone = Zone.GRAVEYARD))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Slawomir Maniak"
        flavorText = "It serves a justice higher than the whims of the gods."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/177ece7c-b979-4a31-ba5f-6a8a382f2862.jpg?1783939576"
    }
}
