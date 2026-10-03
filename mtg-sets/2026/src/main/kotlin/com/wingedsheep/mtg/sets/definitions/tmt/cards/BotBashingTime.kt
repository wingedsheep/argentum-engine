package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bot Bashing Time
 * {3}{R}
 * Sorcery
 *
 * Bot Bashing Time deals 6 damage to target creature. If that creature
 * would die this turn, exile it instead.
 */
val BotBashingTime = card("Bot Bashing Time") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Bot Bashing Time deals 6 damage to target creature. If that creature would die this turn, exile it instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(6, creature) then Effects.MarkExileOnDeath(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Xavier Ribeiro"
        flavorText = "\"Well, I'd say we redecorated the place nicely!\"\n—Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08b3af61-d516-45ca-aede-2f50ba23cb07.jpg?1771586935"
    }
}
