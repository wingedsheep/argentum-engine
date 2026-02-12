package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Cabal Executioner
 * {2}{B}{B}
 * Creature — Human Cleric
 * 2/2
 * Whenever Cabal Executioner deals combat damage to a player, that player sacrifices a creature.
 * Morph {3}{B}{B}
 */
val CabalExecutioner = card("Cabal Executioner") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever Cabal Executioner deals combat damage to a player, that player sacrifices a creature.\nMorph {3}{B}{B}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ForceSacrificeEffect(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Opponent))
    }

    morph = "{3}{B}{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Rebecca Guay"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/c/d/cd7727a7-0cdf-4fd5-82b4-e6587c10ca80.jpg?1562943489"
    }
}
