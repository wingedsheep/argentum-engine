package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Woodcloaker
 * {5}{G}
 * Creature — Elf
 * 3/3
 * Morph {2}{G}{G}
 * When Woodcloaker is turned face up, target creature gains trample until end of turn.
 */
val Woodcloaker = card("Woodcloaker") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Elf"
    power = 3
    toughness = 3
    oracleText = "Morph {2}{G}{G}\nWhen Woodcloaker is turned face up, target creature gains trample until end of turn."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.Creature
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0))
    }

    morph = "{2}{G}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Thomas M. Baxa"
        flavorText = "The Mirari's mutating effects touched every living thing on Otaria."
        imageUri = "https://cards.scryfall.io/large/front/0/8/08edd742-4837-4635-b32c-2e80933df878.jpg?1562527975"
    }
}
