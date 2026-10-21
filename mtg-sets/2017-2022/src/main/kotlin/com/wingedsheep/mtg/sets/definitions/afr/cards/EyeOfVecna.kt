package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eye of Vecna
 * {2}
 * Legendary Artifact
 * When Eye of Vecna enters, you draw a card and you lose 2 life.
 * At the beginning of your upkeep, you may pay {2}. If you do, you draw a card and you lose 2 life.
 */
val EyeOfVecna = card("Eye of Vecna") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "When Eye of Vecna enters, you draw a card and you lose 2 life.\nAt the beginning of your upkeep, you may pay {2}. If you do, you draw a card and you lose 2 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            ManaCost.parse("{2}"),
            Effects.DrawCards(1) then Effects.LoseLife(2, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Irina Nordsol"
        flavorText = "\"I see all things as they truly are! I see . . . decay in all flesh, and betrayal in every heart.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b91e2431-500e-441a-881d-094ebef62283.jpg?1783926440"
    }
}
