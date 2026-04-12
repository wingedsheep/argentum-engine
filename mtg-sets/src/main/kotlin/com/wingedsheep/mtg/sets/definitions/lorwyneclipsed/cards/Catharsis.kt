package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Catharsis
 * {4}{R/W}{R/W}
 * Creature — Elemental Incarnation
 * 3/4
 *
 * When this creature enters, if {W}{W} was spent to cast it, create two 1/1 green
 * and white Kithkin creature tokens.
 * When this creature enters, if {R}{R} was spent to cast it, creatures you control
 * get +1/+1 and gain haste until end of turn.
 * Evoke {R/W}{R/W}
 */
val Catharsis = card("Catharsis") {
    manaCost = "{4}{R/W}{R/W}"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, if {W}{W} was spent to cast it, create two 1/1 green and white Kithkin creature tokens.\nWhen this creature enters, if {R}{R} was spent to cast it, creatures you control get +1/+1 and gain haste until end of turn.\nEvoke {R/W}{R/W}"

    evoke = "{R/W}{R/W}"

    // Red gate defined first so it goes on the stack first (bottom).
    // White gate defined second so it goes on top and resolves first,
    // creating tokens before the pump effect. This way the tokens
    // also receive the +1/+1 and haste buff.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredRed = 2)
        effect = CompositeEffect(
            listOf(
                ForEachInGroupEffect(
                    GroupFilter.AllCreaturesYouControl,
                    ModifyStatsEffect(1, 1, EffectTarget.Self)
                ),
                ForEachInGroupEffect(
                    GroupFilter.AllCreaturesYouControl,
                    GrantKeywordEffect(Keyword.HASTE, EffectTarget.Self)
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredWhite = 2)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "209"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/affb4500-2704-49fc-bbb4-02ed4bfb3b76.jpg?1767952383"
    }
}
