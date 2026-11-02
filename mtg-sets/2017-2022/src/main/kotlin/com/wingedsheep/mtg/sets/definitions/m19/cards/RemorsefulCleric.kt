package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Remorseful Cleric — Core Set 2019 #33
 * {1}{W} · Creature — Spirit Cleric · 2/1
 *
 * Flying
 * Sacrifice this creature: Exile target player's graveyard.
 */
val RemorsefulCleric = card("Remorseful Cleric") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Cleric"
    power = 2
    toughness = 1
    oracleText = "Flying\nSacrifice this creature: Exile target player's graveyard."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeSelf
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            exile(graveyard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Grzegorz Rutkowski"
        flavorText = "A lifetime of keeping up appearances is not enough to earn an eternity of rest."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9620716d-9be8-4ebd-80d2-679373f4f897.jpg?1783934598"
    }
}
