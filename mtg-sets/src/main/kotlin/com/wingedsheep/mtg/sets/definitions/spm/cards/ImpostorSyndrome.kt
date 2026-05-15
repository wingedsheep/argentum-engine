package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Impostor Syndrome
 * {4}{U}{U}
 * Enchantment
 * Whenever a nontoken creature you control deals combat damage to a player, create a token
 * that's a copy of it, except it isn't legendary.
 */
val ImpostorSyndrome = card("Impostor Syndrome") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control deals combat damage to a player, create a token that's a copy of it, except it isn't legendary."

    triggeredAbility {
        trigger = Triggers.NontokenCreatureYouControlDealsCombatDamageToPlayer
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            removedSupertypes = setOf(Supertype.LEGENDARY)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "Javier Charro"
        flavorText = "\"I'm the real Spider-Man!\"\n\"No, *I'm* the real Spider-Man!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08da9f92-0e25-4f39-aaa4-d8974af81a41.jpg?1757376952"
    }
}
