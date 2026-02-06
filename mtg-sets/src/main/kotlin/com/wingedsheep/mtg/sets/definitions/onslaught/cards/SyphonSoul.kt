package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.scripting.Player

/**
 * Syphon Soul
 * {2}{B}
 * Sorcery
 * Syphon Soul deals 2 damage to each other player. You gain life equal to the damage dealt this way.
 *
 * In a 1v1 game, this always deals 2 damage and gains 2 life.
 */
val SyphonSoul = card("Syphon Soul") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = DealDamageToPlayersEffect(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
                GainLifeEffect(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Ron Spears"
        flavorText = "As Phage drank their energy, a vague memory of Jeska stirred. Then she lost herself again in the joy of her victims' suffering."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bdaef0f-9965-463b-902d-72ec24b2db7b.jpg?1562909040"
    }
}
