package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Irencrag Feat
 * {1}{R}{R}{R}
 * Sorcery
 * Add seven {R}. You can cast only one more spell this turn.
 */
val IrencragFeat = card("Irencrag Feat") {
    manaCost = "{1}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Add seven {R}. You can cast only one more spell this turn."
    spell {
        effect = Effects.AddMana(Color.RED, 7) then
            Effects.CantCastSpells(EffectTarget.Controller, moreAllowed = 1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Yongjae Choi"
        flavorText = "The greatest challenge isn't the stifling heat but the crushing weight of those who failed before."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bcf822-e129-45f6-9403-310ce9410f3b.jpg?1783932622"
    }
}
