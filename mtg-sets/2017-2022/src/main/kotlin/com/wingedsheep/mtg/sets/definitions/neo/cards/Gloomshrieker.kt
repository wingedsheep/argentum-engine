package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gloomshrieker — Kamigawa: Neon Dynasty #219 (canonical printing)
 * {1}{B}{G} · Enchantment Creature — Cat Beast · 2/1
 *
 * Menace
 * When this creature enters, return target permanent card from your graveyard to your hand.
 * If this creature would die, exile it instead.
 *
 * The enters trigger targets any permanent card you own in your graveyard (Spring-Leaf Avenger's
 * shape); the death clause is a self-scoped [RedirectZoneChange] battlefield → graveyard → exile.
 */
val Gloomshrieker = card("Gloomshrieker") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment Creature — Cat Beast"
    power = 2
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, return target permanent card from your graveyard to your hand.\n" +
        "If this creature would die, exile it instead."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(t)
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Fiona Hsieh"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2b50751-7f65-4321-86da-eef735bf8b67.jpg?1783923836"
    }
}
