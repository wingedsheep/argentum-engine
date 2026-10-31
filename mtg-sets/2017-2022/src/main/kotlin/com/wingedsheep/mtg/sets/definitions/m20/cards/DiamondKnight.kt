package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Diamond Knight
 * {3}
 * Artifact Creature — Knight
 * 1/1
 *
 * Vigilance
 * As this creature enters, choose a color.
 * Whenever you cast a spell of the chosen color, put a +1/+1 counter on this creature.
 *
 * Same shape as Diamond Mare (M19): the color is stored at ETB via [EntersWithChoice]
 * (ChoiceType.COLOR) and the cast trigger filters on
 * [GameObjectFilter.sharingChosenColorWithSource].
 */
val DiamondKnight = card("Diamond Knight") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Knight"
    power = 1
    toughness = 1
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "As this creature enters, choose a color.\n" +
        "Whenever you cast a spell of the chosen color, put a +1/+1 counter on this creature."

    keywords(Keyword.VIGILANCE)

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.sharingChosenColorWithSource())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast a spell of the chosen color, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeab4819-dd26-4f5c-8538-50f2caca292d.jpg?1783932945"
    }
}
