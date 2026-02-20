package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.triggers.OnCreatureWithSubtypeDealsCombatDamageToPlayer
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cabal Slaver
 * {2}{B}
 * Creature — Human Cleric
 * 2/1
 * Whenever a Goblin deals combat damage to a player, that player discards a card.
 */
val CabalSlaver = card("Cabal Slaver") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "Whenever a Goblin deals combat damage to a player, that player discards a card."

    triggeredAbility {
        trigger = OnCreatureWithSubtypeDealsCombatDamageToPlayer(Subtype("Goblin"))
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Thomas M. Baxa"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9c04fd3-021a-4011-be9b-0d268557aa06.jpg?1562938749"
    }
}
