package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DiscardCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.SacrificeSelfEffect

/**
 * Haunted Cadaver
 * {3}{B}
 * Creature — Zombie
 * 2/2
 * Whenever Haunted Cadaver deals combat damage to a player, you may sacrifice it.
 * If you do, that player discards three cards.
 * Morph {1}{B}
 */
val HauntedCadaver = card("Haunted Cadaver") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "Whenever Haunted Cadaver deals combat damage to a player, you may sacrifice it. If you do, that player discards three cards.\nMorph {1}{B}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayEffect(
            SacrificeSelfEffect then DiscardCardsEffect(3, EffectTarget.PlayerRef(Player.Opponent))
        )
    }

    morph = "{1}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Randy Gallegos"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/a/1/a164420c-3619-4f5e-81cf-2aa5a4553bc3.jpg?1562933106"
    }
}
