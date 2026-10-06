package com.wingedsheep.mtg.sets.definitions.ons.cards

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
        collectorNumber = "176"
        artist = "Ron Spears"
        flavorText = "As Phage drank their energy, a vague memory of Jeska stirred. Then she lost herself again in the joy of her victims' suffering."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bdaef0f-9965-463b-902d-72ec24b2db7b.jpg?1562909040"
    }
}
