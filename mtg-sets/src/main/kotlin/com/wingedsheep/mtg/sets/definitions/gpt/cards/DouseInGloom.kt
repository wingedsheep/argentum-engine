package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Douse in Gloom
 * {2}{B}
 * Instant
 * Douse in Gloom deals 2 damage to target creature and you gain 2 life.
 */
val DouseInGloom = card("Douse in Gloom") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Douse in Gloom deals 2 damage to target creature and you gain 2 life."

    spell {
        val t = target("creature", Targets.Creature)
        effect = Effects.DealDamage(2, t)
            .then(Effects.GainLife(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Kev Walker"
        flavorText = "Orzhov prisoners are steeped in a blackened brew that robs their souls of strength. Patriarchs drink that brew to extend their own lives."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50a4fe4d-460d-4143-9a8e-14e16b211722.jpg?1593272187"
    }
}
