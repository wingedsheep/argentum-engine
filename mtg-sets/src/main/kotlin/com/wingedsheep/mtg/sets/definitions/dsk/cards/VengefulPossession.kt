package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Vengeful Possession
 * {2}{R}
 * Sorcery
 *
 * Gain control of target creature until end of turn. Untap it. It gains haste until end of
 * turn. You may discard a card. If you do, draw a card.
 */
val VengefulPossession = card("Vengeful Possession") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap it. It gains haste until end of turn. You may discard a card. If you do, draw a card."

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.GainControl(t, Duration.EndOfTurn),
            Effects.Untap(t),
            Effects.GrantKeyword(Keyword.HASTE, t, Duration.EndOfTurn),
            MayEffect(
                effect = IfYouDoEffect(
                    action = Patterns.Hand.discardCards(1),
                    ifYouDo = Effects.DrawCards(1),
                ),
                descriptionOverride = "You may discard a card. If you do, draw a card.",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Dominik Mayer"
        flavorText = "As the ghost clawed its way into her skin, Sarissa forgot that the rage tearing through her heart wasn't her own."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6918d50-a4c3-40d8-9480-343f14773a69.jpg?1726286456"
    }
}
