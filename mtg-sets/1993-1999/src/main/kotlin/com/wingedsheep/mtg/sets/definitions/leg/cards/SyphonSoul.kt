package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Syphon Soul
 * {2}{B}
 * Sorcery
 * Syphon Soul deals 2 damage to each other player. You gain life equal to the damage dealt this way.
 *
 * The gain reads the damage actually dealt (`damageDealtVariable`), so it scales with the number
 * of opponents and ignores prevented damage.
 */
val SyphonSoul = card("Syphon Soul") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Syphon Soul deals 2 damage to each other player. You gain life equal to the damage dealt this way."

    spell {
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent), damageDealtVariable = it)
            }
            run(Effects.GainLife(dealt.amount))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3020304-7a39-411e-b055-3ade72b4bff8.jpg?1783948063"
        flavorText = "\"Her lips suck forth; see, where it flies!\"\n—Christopher Marlowe, *The Tragical History of Doctor Faustus*"
    }
}
