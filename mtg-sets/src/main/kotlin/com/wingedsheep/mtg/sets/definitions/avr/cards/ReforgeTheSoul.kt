package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reforge the Soul
 * {3}{R}{R}
 * Sorcery
 *
 * Each player discards their hand, then draws seven cards.
 * Miracle {1}{R}
 *
 * A symmetric wheel: [Effects.ForEachPlayer] over [Player.Each] rebinds the body's controller to
 * the current player in APNAP order, so `Patterns.Hand.discardHand()` (defaulting to the
 * controller) and the draw both resolve for them. Discard-then-draw is sequenced per player rather
 * than globally, which matters for a player whose library runs out: they still lose to the empty
 * draw at the next state check, and the other players' hands are already gone.
 *
 * Miracle is the standard [KeywordAbility.miracle] first-draw-of-turn alternative cost.
 */
val ReforgeTheSoul = card("Reforge the Soul") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each player discards their hand, then draws seven cards.\n" +
        "Miracle {1}{R} (You may cast this card for its miracle cost when you draw it if it's the " +
        "first card you drew this turn.)"

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effects = listOf(
                Patterns.Hand.discardHand(),
                Effects.DrawCards(7),
            ),
        )
    }

    keywordAbility(KeywordAbility.miracle("{1}{R}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Jaime Jones"
        flavorText = "In a wave of spells called the Cursemute, Avacyn cleansed the world with divine fire."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36506caa-2630-46ec-9aa0-e1885749ad90.jpg?1783940679"
    }
}
