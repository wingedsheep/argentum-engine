package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liege of the Axe
 * {3}{W}
 * Creature — Human Soldier
 * 2/3
 * Vigilance
 * Morph {1}{W}
 * When this creature is turned face up, untap it.
 */
val LiegeOfTheAxe = card("Liege of the Axe") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Vigilance\nMorph {1}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, untap it."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.Untap(EffectTarget.Self)
    }

    morph = "{1}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb518bf0-17ad-4bbf-b922-42ee76ffcbea.jpg?1562942251"
    }
}
