package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Steampath Charger
 * {1}{R}
 * Creature — Lizard Warlock
 * 2/1
 * Offspring {2}
 * When this creature dies, it deals 1 damage to target player.
 */
val SteampathCharger = card("Steampath Charger") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Lizard Warlock"
    power = 2
    toughness = 1
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhen this creature dies, it deals 1 damage to target player."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf()
    }

    triggeredAbility {
        trigger = Triggers.Dies
        val player = target("target player", Targets.Player)
        effect = Effects.DealDamage(1, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Ilse Gort"
        flavorText = "Hot-footed and cold-blooded."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03bf1296-e347-4070-8c6f-5c362c2f9364.jpg?1721426709"
    }
}
