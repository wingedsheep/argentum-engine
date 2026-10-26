package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Light Up the Stage
 * {2}{R}
 * Sorcery
 * Spectacle {R} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * Exile the top two cards of your library. Until the end of your next turn, you may play those cards.
 *
 * Spectacle (CR 702.137a) is "you may pay [cost] rather than pay this spell's mana cost if an
 * opponent lost life this turn" — exactly a [SelfAlternativeCost] gated on
 * [Conditions.OpponentLostLifeThisTurn] (the Blasphemous Edict shape). The gate reads the turn's
 * life-loss tracker, so an opponent who lost life and later gained it back still enables it, per
 * the ruling. The body is the impulse-draw recipe with an until-end-of-your-next-turn window.
 */
val LightUpTheStage = card("Light Up the Stage") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Spectacle {R} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "Exile the top two cards of your library. Until the end of your next turn, you may play those cards."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{R}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    spell {
        effect = Patterns.Exile.impulse(count = 2, expiry = MayPlayExpiry.UntilEndOfNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Dmitry Burmak"
        flavorText = "\"Places, everyone!\"\n—Judith"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9287b848-2aeb-4c70-ac4a-acafb871b7a4.jpg?1783933679"
        ruling("2024-01-12", "Spectacle cares only that an opponent lost life during the turn, not that the opponent's life total is currently lower than it was. For example, if an opponent loses 1 life and then gains 2 life in the same turn, you can cast a spell for its spectacle cost that turn.")
        ruling("2024-01-12", "Spectacle doesn't change when you can cast the spell. For example, you can't cast a sorcery with spectacle during an opponent's turn unless another effect allows you to do so, even if that player has lost life this turn.")
        ruling("2019-01-25", "Light Up the Stage doesn't change when you can play the exiled cards. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty. If you exile a land card, you can play it only during your main phase and only if you have an available land play remaining.")
    }
}
