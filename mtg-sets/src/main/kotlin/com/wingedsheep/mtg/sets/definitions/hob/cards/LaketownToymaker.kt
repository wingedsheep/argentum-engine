package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lake-town Toymaker
 * {3}{W}
 * Creature — Human Artificer
 * 3/4
 *
 * At the beginning of combat on your turn, if you've drawn two or more cards this turn, another
 * target creature you control gets +3/+0 and gains first strike until end of turn.
 *
 * The "if you've drawn two or more cards this turn" clause is an intervening-if (CR 603.4) —
 * checked both when the trigger would go on the stack and again on resolution — so it's
 * [triggerCondition], not a [com.wingedsheep.sdk.scripting.effects.ConditionalEffect]. It reads the
 * controller's `CardsDrawnThisTurnComponent`, which counts every draw this turn regardless of source.
 */
val LaketownToymaker = card("Lake-town Toymaker") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    oracleText = "At the beginning of combat on your turn, if you've drawn two or more cards this " +
        "turn, another target creature you control gets +3/+0 and gains first strike until end of turn."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.YouDrewCardsThisTurn(2)
        val t = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.Composite(
            Effects.ModifyStats(3, 0, t),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, t),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Marina Ortega Lorente"
        flavorText = "In the great days of old, when Dale in the North was rich and prosperous, Lake-town flourished."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67304269-c595-4cf0-8dbf-fcb2e9e01fe2.jpg?1785496951"
    }
}
