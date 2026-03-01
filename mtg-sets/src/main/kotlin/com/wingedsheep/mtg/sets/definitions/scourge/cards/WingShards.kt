package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect

/**
 * Wing Shards
 * {1}{W}{W}
 * Instant
 * Target player sacrifices an attacking creature of their choice.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val WingShards = card("Wing Shards") {
    manaCost = "{1}{W}{W}"
    typeLine = "Instant"
    oracleText = "Target player sacrifices an attacking creature of their choice.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val player = target("target player", Targets.Player)
        effect = ForceSacrificeEffect(GameObjectFilter.Creature.attacking(), 1, player)
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65efa443-666a-45c1-8784-e98c510854b5.jpg?1562529751"
    }
}
