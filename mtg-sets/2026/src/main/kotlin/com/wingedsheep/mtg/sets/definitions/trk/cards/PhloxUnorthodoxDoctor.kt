package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Phlox, Unorthodox Doctor
 * {2}{G}{W}
 * Legendary Creature — Denobulan Doctor
 * 3/4
 *
 * Trample
 * Whenever a creature you control with a counter on it deals combat damage to a player, draw a card.
 *
 * "A creature" triggers once per creature that connects, Phlox included; any kind of counter counts.
 */
val PhloxUnorthodoxDoctor = card("Phlox, Unorthodox Doctor") {
    manaCost = "{2}{G}{W}"
    typeLine = "Legendary Creature — Denobulan Doctor"
    power = 3
    toughness = 4
    oracleText = "Trample\nWhenever a creature you control with a counter on it deals combat damage to a player, draw a card."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withAnyCounter())
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
        description = "Whenever a creature you control with a counter on it deals combat damage to a player, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Jason Seiler"
        flavorText = "\"If you're going to try to embrace new worlds, you must try to embrace new ideas.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2695e76e-c27d-49fc-9ef2-d9eaff449f3d.jpg?1792508387"
    }
}
