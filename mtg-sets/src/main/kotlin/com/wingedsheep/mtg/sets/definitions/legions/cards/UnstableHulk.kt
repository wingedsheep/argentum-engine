package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unstable Hulk
 * {1}{R}{R}
 * Creature — Goblin Mutant
 * 2/2
 * Morph {3}{R}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When Unstable Hulk is turned face up, it gets +6/+6 and gains trample until end of turn. You skip your next turn.
 */
val UnstableHulk = card("Unstable Hulk") {
    manaCost = "{1}{R}{R}"
    typeLine = "Creature — Goblin Mutant"
    oracleText = "Morph {3}{R}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Unstable Hulk is turned face up, it gets +6/+6 and gains trample until end of turn. You skip your next turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.ModifyStats(6, 6, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self))
            .then(Effects.SkipNextTurn(EffectTarget.Controller))
    }

    morph = "{3}{R}{R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/889cfde2-42fa-4278-ae4e-7e4dd993cda8.jpg?1562922605"
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
    }
}
