package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Wild Wasteland
 * {2}{R}
 * Enchantment
 *
 * Skip your draw step.
 * At the beginning of your upkeep, exile the top two cards of your library. You may play those
 * cards this turn.
 *
 * The skip is Colfenor's Plans' standing [SkipStepOrPhase] (`DRAW_STEP`); the upkeep trigger is the
 * shared gather → exile → grant impulse pipeline (Professional Face-Breaker) over two cards.
 */
val WildWasteland = card("Wild Wasteland") {
    manaCost = "{2}{R}"
    typeLine = "Enchantment"
    oracleText = "Skip your draw step.\n" +
        "At the beginning of your upkeep, exile the top two cards of your library. You may play those cards this turn."

    staticAbility {
        ability = SkipStepOrPhase(TurnPart.DRAW_STEP)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val exiledCards = gather(CardSource.TopOfLibrary(2))
            exile(exiledCards)
            run(Effects.GrantMayPlayFromExile(exiledCards))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Mathias Kollros"
        flavorText = "Some things in the wasteland are best left unexplained."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca1024d6-c5f1-48f5-9081-fb90d0c46fdb.jpg?1783912401"
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for cards played from exile with Wild Wasteland’s ability. For example, if one of the exiled cards is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
