package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kor Sky Climber
 * {2}{W}
 * Creature — Kor Soldier Ally
 * 3/2
 * {1}{W}: This creature gains flying until end of turn.
 */
val KorSkyClimber = card("Kor Sky Climber") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Soldier Ally"
    power = 3
    toughness = 2
    oracleText = "{1}{W}: This creature gains flying until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Victor Adame Minguez"
        flavorText = "\"With rope, I can fly as far as the rope can reach. But with light, the sky is the limit.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54938334-ebad-4dde-82e6-8c854aef4a91.jpg"
    }
}
