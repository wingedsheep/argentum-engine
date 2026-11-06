package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Metalspinner's Puzzleknot
 * {2}
 * Artifact
 * When this artifact enters, you draw a card and you lose 1 life.
 * {2}{B}, Sacrifice this artifact: You draw a card and you lose 1 life.
 */
val MetalspinnersPuzzleknot = card("Metalspinner's Puzzleknot") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you draw a card and you lose 1 life.\n" +
        "{2}{B}, Sacrifice this artifact: You draw a card and you lose 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b1639c3-f22d-4246-9252-219a4b2b2999.jpg"
    }
}
