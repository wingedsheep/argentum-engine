package com.wingedsheep.mtg.sets.definitions.mom.cards

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
 * Wildwood Escort (March of the Machine #216)
 * {4}{G} · Creature — Elf Warrior 3/3
 *
 * When this creature enters, return target creature or battle card from your graveyard to your hand.
 * If this creature would die, exile it instead.
 *
 * The enters trigger targets a creature-or-battle card you own in your graveyard (Heliod's shape);
 * the death clause is a self-scoped [RedirectZoneChange] battlefield → graveyard → exile.
 */
val WildwoodEscort = card("Wildwood Escort") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, return target creature or battle card from your " +
        "graveyard to your hand.\nIf this creature would die, exile it instead."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(
            TargetFilter(
                baseFilter = (GameObjectFilter.Creature or GameObjectFilter.Battle).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.ReturnToHand(card)
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Taras Susak"
        flavorText = "With Eldraine's human knights called away to defend the courts, lost travelers " +
            "were surprised to find themselves whisked to safety by the secretive folk of the wilds."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8c6fc26-df6e-44de-96e6-a6e34086edc2.jpg?1783916956"
    }
}
