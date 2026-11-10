package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shamble Back (Shadows over Innistrad #134)
 * {B}
 * Sorcery
 *
 * Exile target creature card from a graveyard. Create a 2/2 black Zombie creature token.
 * You gain 2 life.
 *
 * The creature card is the spell's only target, so if it has left the graveyard by resolution
 * the spell doesn't resolve at all — no token, no life (see ruling).
 */
val ShambleBack = card("Shamble Back") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile target creature card from a graveyard. Create a 2/2 black Zombie creature token. " +
        "You gain 2 life."

    spell {
        val t = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(t, Zone.EXILE) then
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
            ) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Izzy"
        flavorText = "Lost loved ones are never truly gone."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6913dce-05f5-4e1b-9dd9-67eb347b5be8.jpg?1783937766"
        ruling(
            "2016-04-08",
            "If the creature card becomes an illegal target, Shamble Back doesn't resolve. None of its effects happen.",
        )
    }
}
