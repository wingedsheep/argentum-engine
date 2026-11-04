package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Siren's Ruse
 * {1}{U}
 * Instant
 * Exile target creature you control, then return that card to the battlefield under its owner's
 * control. If a Pirate was exiled this way, draw a card.
 *
 * "A Pirate was exiled" reads the creature *as it was exiled* (ruling: a Pirate token that never
 * returns, or a returned copy that is no longer a Pirate, still draws). So the Pirate check runs
 * on the permanent before the flicker — nothing happens between that check and the exile — and
 * picks the draw branch. That branch also checks that the exile actually happened before
 * returning the creature, including tokens that stay in exile until state-based actions.
 */
val SirensRuse = card("Siren's Ruse") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile target creature you control, then return that card to the battlefield " +
        "under its owner's control. If a Pirate was exiled this way, draw a card."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        val exile = Effects.Move(creature, Zone.EXILE)
        val returnExiled = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.EXILE)
        val flicker = exile then returnExiled
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.PIRATE),
                creature
            ),
            then = exile then Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Any.currentlyIn(Zone.EXILE), creature),
                then = returnExiled then Effects.DrawCards(1)
            ),
            otherwise = flicker
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Ben Maier"
        flavorText = "Deception and misdirection are the heart of the pirate's arsenal."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f389872-d1f4-4828-a527-e820a6f2ac21.jpg?1783935771"
        ruling("2017-09-29", "Once the exiled creature returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled creature will be put into their owners' graveyards. Equipment attached to the exiled creature will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
        ruling("2017-09-29", "The returned creature won't be the target of any spells or abilities that targeted it before. Any spells that don't target it, such as Star of Extinction, will still affect it.")
        ruling("2017-09-29", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2017-09-29", "You'll draw a card if the creature was a Pirate as it was exiled, even if it doesn't return to the battlefield (most likely because it's a token) or if it returns to the battlefield but isn't a Pirate anymore (most likely because it's copying something else).")
    }
}
