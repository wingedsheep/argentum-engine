package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Lembas
 * {2}
 * Artifact — Food
 *
 * When this artifact enters, scry 1, then draw a card.
 * {2}, {T}, Sacrifice this artifact: You gain 3 life.
 * When this artifact is put into a graveyard from the battlefield, its owner shuffles it into their library.
 */
val Lembas = card("Lembas") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Food"
    oracleText = "When this artifact enters, scry 1, then draw a card.\n" +
        "{2}, {T}, Sacrifice this artifact: You gain 3 life.\n" +
        "When this artifact is put into a graveyard from the battlefield, its owner shuffles it into their library."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(1) then Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.GainLife(3)
    }

    triggeredAbility {
        trigger = Triggers.PutIntoGraveyardFromBattlefield
        effect = Effects.ShuffleIntoLibrary(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Viko Menezes"
        flavorText = "They could eat of it and find new strength even as they ran."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b46aacf-b31a-4380-9e4b-82795fbaba3b.jpg?1686970202"
    }
}
