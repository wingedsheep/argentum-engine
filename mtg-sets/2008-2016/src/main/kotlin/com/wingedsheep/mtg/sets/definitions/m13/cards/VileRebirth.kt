package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vile Rebirth
 * {B}
 * Instant
 * Exile target creature card from a graveyard. Create a 2/2 black Zombie creature token.
 *
 * Canonical printing: Magic 2013, the card's earliest real-expansion printing. Reprinted in M14 as
 * a `Printing` row. The spell's only target is the graveyard card, so if it has left the graveyard
 * by resolution the spell doesn't resolve and no Zombie is created (2012 ruling).
 */
val VileRebirth = card("Vile Rebirth") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature card from a graveyard. Create a 2/2 black Zombie creature token."

    spell {
        val t = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(t, Zone.EXILE) then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Erica Yang"
        flavorText = "\"'Here lies our beloved Gartu, the gentlest of all men.' Ooh, I love irony.\"\n—Jadar, ghoulcaller of Nephalia"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/965b5a48-d0ff-47ce-b44e-a1611fab1876.jpg?1783940489"
        ruling(
            "2012-07-01",
            "If the creature card is an illegal target when Vile Rebirth tries to resolve, it won't resolve and none of its effects will happen. You won't put a Zombie token onto the battlefield."
        )
    }
}
