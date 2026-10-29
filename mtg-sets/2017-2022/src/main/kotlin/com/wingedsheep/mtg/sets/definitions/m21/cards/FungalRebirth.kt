package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fungal Rebirth — Core Set 2021 #182 (canonical printing)
 * {2}{G} · Instant
 *
 * Return target permanent card from your graveyard to your hand. If a creature died this turn,
 * create two 1/1 green Saproling creature tokens.
 *
 * The "if a creature died this turn" rider is checked on resolution and adds the Saprolings in
 * addition to the returned card; it never scales with how many creatures died (rulings).
 */
val FungalRebirth = card("Fungal Rebirth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Return target permanent card from your graveyard to your hand. If a creature died this turn, " +
        "create two 1/1 green Saproling creature tokens."

    spell {
        val card = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(target = card, destination = Zone.HAND) then
            Effects.If(
                condition = Conditions.CreatureDiedThisTurn,
                then = Effects.CreateToken(
                    count = 2,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Saproling"),
                    imageUri = "https://cards.scryfall.io/normal/front/d/7/d71a5aa4-a960-4c42-b8ac-7003f6e83e95.jpg?1783930590"
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Nicholas Gregory"
        flavorText = "\"We're all food for something in the end.\"\n—Cevraya, Golgari shaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/4037e3b2-cb62-4f88-943d-3edcd6827c23.jpg?1783930677"
        ruling("2020-06-23", "If a creature died this turn, you get Saproling tokens in addition to the permanent card being returned to your hand.")
        ruling("2020-06-23", "Fungal Rebirth checks only whether a creature died this turn. It doesn't give you additional Saprolings if more than one creature died.")
    }
}
