package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elim Garak, Simple Tailor
 * {2}{B}
 * Legendary Creature — Cardassian Rogue
 * 3/3
 *
 * Menace
 * Whenever you face a dilemma, each opponent loses 2 life and you gain 2 life. (You face a dilemma
 * as you choose one or more modes for a spell or ability.)
 *
 * "Face a dilemma" fires once per mode choice, however many modes that choice took
 * (`Triggers.you.facesADilemma()`).
 */
val ElimGarakSimpleTailor = card("Elim Garak, Simple Tailor") {
    manaCost = "{2}{B}"
    typeLine = "Legendary Creature — Cardassian Rogue"
    colorIdentity = "B"
    oracleText = "Menace\nWhenever you face a dilemma, each opponent loses 2 life and you gain 2 life. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 3
    toughness = 3
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Alexander Gering"
        flavorText = "\"I never tell the truth because I don't believe there is such a thing. That's why I " +
            "prefer the straight-line simplicity of cutting cloth.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/1/113bd51a-dfbb-4106-adf3-e567057fcd06.jpg?1792668600"
    }
}
