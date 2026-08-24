package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Haunted Dead
 * {3}{B}
 * Creature — Zombie
 * 2/2
 * When this creature enters, create a 1/1 white Spirit creature token with flying.
 * {1}{B}, Discard two cards: Return this card from your graveyard to the battlefield tapped.
 */
val HauntedDead = card("Haunted Dead") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "When this creature enters, create a 1/1 white Spirit creature token with flying.\n" +
        "{1}{B}, Discard two cards: Return this card from your graveyard to the battlefield tapped."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Discard(count = 2))
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14a7c8b7-ca77-47a0-8965-949723ec902d.jpg?1782711885"
    }
}
