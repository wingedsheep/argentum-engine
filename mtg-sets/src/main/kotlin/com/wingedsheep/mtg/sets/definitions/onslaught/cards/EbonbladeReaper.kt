package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ebonblade Reaper
 * {2}{B}
 * Creature — Human Cleric
 * 1/1
 * Whenever Ebonblade Reaper attacks, you lose half your life, rounded up.
 * Whenever Ebonblade Reaper deals combat damage to a player, that player loses half their life, rounded up.
 * Morph {3}{B}{B}
 */
val EbonbladeReaper = card("Ebonblade Reaper") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Whenever Ebonblade Reaper attacks, you lose half your life, rounded up.\nWhenever Ebonblade Reaper deals combat damage to a player, that player loses half their life, rounded up.\nMorph {3}{B}{B}"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.LoseHalfLife(roundUp = true, target = EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.LoseHalfLife(
            roundUp = true,
            target = EffectTarget.PlayerRef(Player.Opponent),
            lifePlayer = Player.Opponent
        )
    }

    morph = "{3}{B}{B}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Wayne England"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16ebef2c-8bb2-4816-a628-0062f95e512e.jpg?1562900542"
    }
}
