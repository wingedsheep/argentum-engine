package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hymn of the Faller
 * {1}{B}
 * Sorcery
 * Surveil 1, then you draw a card and lose 1 life.
 * Void — If a nonland permanent left the battlefield this turn or a spell was warped this turn, draw another card.
 */
val HymnOfTheFaller = card("Hymn of the Faller") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Surveil 1, then you draw a card and lose 1 life. (To surveil 1, look at the top card of your library. You may put it into your graveyard.)\n" +
        "Void — If a nonland permanent left the battlefield this turn or a spell was warped this turn, draw another card."

    spell {
        effect = EffectPatterns.surveil(1)
            .then(Effects.DrawCards(1))
            .then(Effects.LoseLife(1, EffectTarget.Controller))
            .then(ConditionalEffect(Conditions.Void, Effects.DrawCards(1)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Danny Schwartz"
        flavorText = "\"Does truth lie in the void between stanzas?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e468d528-6cf0-4563-9da2-e388ba56cb9d.jpg?1752946985"
    }
}
