package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Knight of Dusk's Shadow
 * {1}{B}
 * Creature — Human Knight
 * 2/2
 * Menace
 * Your opponents can't gain life.
 * {1}{B}: This creature gets +1/+1 until end of turn.
 */
val KnightOfDusksShadow = card("Knight of Dusk's Shadow") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Your opponents can't gain life.\n" +
        "{1}{B}: This creature gets +1/+1 until end of turn."

    keywords(Keyword.MENACE)

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33dfd2fe-e0e0-465c-b730-a30e6f5c271c.jpg?1783921331"
    }
}
