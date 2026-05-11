package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Decode Transmissions
 * {2}{B}
 * Sorcery
 * You draw two cards and lose 2 life.
 * Void — If a nonland permanent left the battlefield this turn or a spell was warped this turn,
 * instead you draw two cards and each opponent loses 2 life.
 */
val DecodeTransmissions = card("Decode Transmissions") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw two cards and lose 2 life.\n" +
        "Void — If a nonland permanent left the battlefield this turn or a spell was warped this turn, instead you draw two cards and each opponent loses 2 life."

    spell {
        effect = ConditionalEffect(
            condition = Conditions.Void,
            effect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            elseEffect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Josh Hass"
        flavorText = "\"You would kill the Faller—our messiah—and call *us* monsters?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cecb4936-14ca-49f9-b209-6519cab54b30.jpg?1752946935"
    }
}
