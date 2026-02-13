package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DiscardCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.Player

/**
 * Silent Specter
 * {4}{B}{B}
 * Creature — Specter
 * 4/4
 * Flying
 * Whenever Silent Specter deals combat damage to a player, that player discards two cards.
 * Morph {3}{B}{B}
 */
val SilentSpecter = card("Silent Specter") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Specter"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever Silent Specter deals combat damage to a player, that player discards two cards.\nMorph {3}{B}{B}"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = DiscardCardsEffect(2, EffectTarget.PlayerRef(Player.Opponent))
    }

    morph = "{3}{B}{B}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Daren Bader"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/b/f/bfd891ba-cf6a-4b83-a421-3a7c346ada31.jpg?1562940249"
    }
}
