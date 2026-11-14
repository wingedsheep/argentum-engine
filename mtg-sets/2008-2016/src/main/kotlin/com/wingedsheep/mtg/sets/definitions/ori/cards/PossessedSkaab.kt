package com.wingedsheep.mtg.sets.definitions.ori.cards

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
 * Possessed Skaab — Magic Origins #215 (canonical printing)
 * {3}{U}{B} · Creature — Zombie · 3/2
 *
 * When this creature enters, return target instant, sorcery, or creature card from your graveyard
 * to your hand.
 * If this creature would die, exile it instead.
 *
 * Gloomshrieker's shape: the enters trigger targets an instant, sorcery, or creature card you own in
 * your graveyard; the death clause is a self-scoped [RedirectZoneChange] battlefield → graveyard → exile.
 */
val PossessedSkaab = card("Possessed Skaab") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, return target instant, sorcery, or creature card from your graveyard to your hand.\n" +
        "If this creature would die, exile it instead."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(
            TargetFilter(
                (GameObjectFilter.InstantOrSorcery or GameObjectFilter.Creature).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
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
        collectorNumber = "215"
        artist = "John Stanko"
        flavorText = "A stitcher's attempt at a skaab with a soul."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ebc94e5-a595-43ab-929b-426009190669.jpg?1783938313"
        ruling(
            "2015-06-22",
            "Possessed Skaab's last ability will apply no matter what caused it to die (lethal damage, being sacrificed, enough -1/-1 counters, and so on). Possessed Skaab won't go to the graveyard. It will be put from the battlefield into exile. Abilities that trigger whenever a creature dies won't trigger."
        )
    }
}
