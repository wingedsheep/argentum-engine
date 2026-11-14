package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Prism Ring
 * {1}
 * Artifact
 *
 * As this artifact enters, choose a color.
 * Whenever you cast a spell of the chosen color, you gain 1 life.
 *
 * Same shape as Diamond Mare: the color is stored at ETB via [EntersWithChoice]
 * (ChoiceType.COLOR → CastChoicesComponent), and the cast trigger is filtered by
 * [GameObjectFilter.sharingChosenColorWithSource], so any spell whose colors include the
 * chosen color (multicolored ones too) triggers it on cast.
 */
val PrismRing = card("Prism Ring") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a color.\n" +
        "Whenever you cast a spell of the chosen color, you gain 1 life."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.sharingChosenColorWithSource())
        effect = Effects.GainLife(1)
        description = "Whenever you cast a spell of the chosen color, you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Daniel Ljunggren"
        flavorText = "Wear it on your dominant hand and it will follow your lead."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/403f99de-493a-4f0f-81e4-fcd29d6e9340.jpg?1783938308"
    }
}
