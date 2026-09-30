package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kinzu of the Bleak Coven
 * {4}{B}
 * Legendary Creature — Phyrexian Vampire
 * 5/4
 * Flying
 * Whenever another nontoken creature you control dies, you may pay 2 life and exile it. If you do,
 * create a token that's a copy of that creature, except it's 1/1 and has toxic 1.
 */
val KinzuOfTheBleakCoven = card("Kinzu of the Bleak Coven") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Vampire"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever another nontoken creature you control dies, you may pay 2 life and exile it. " +
        "If you do, create a token that's a copy of that creature, except it's 1/1 and has toxic 1. " +
        "(Players dealt combat damage by it also get a poison counter.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.MayPay(
            cost = Effects.PayLife(2),
            then = Effects.IfYouDo(
                action = Effects.Pipeline {
                    exile(gather(CardSource.TriggeringEntity))
                },
                then = Effects.CreateTokenCopyOfTarget(
                    EffectTarget.TriggeringEntity,
                    exceptions = CopyExceptions(
                        powerOverride = 1,
                        toughnessOverride = 1,
                        addedNumericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 1)),
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "406"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28934cf3-c1e0-49c9-93ce-fd60b1881884.jpg?1783917920"
        ruling("2023-02-04", "The token copies the creature as it last existed on the battlefield before it died, not as it existed in the graveyard before it was exiled.")
        ruling("2023-02-04", "Except for the listed exceptions, the token copies exactly what was printed on the original creature and nothing else (unless that creature is copying something else; see below). It doesn't copy whether that creature was tapped or untapped, whether it had any counters on it or Auras or Equipment attached to it, or any non-copy effects that had changed its power, toughness, types, color, or so on.")
        ruling("2023-02-04", "If the copied creature was copying something else, then the token enters the battlefield as whatever that creature copied, with the exceptions noted above.")
        ruling("2023-02-04", "If the token is a copy of a creature whose power and toughness are defined by an ability (usually printed as */* or similar), the token doesn't copy the ability that defines its power and toughness. It remains a 1/1 creature.")
        ruling("2023-02-04", "If something becomes a copy of the token, the copy also has base power and toughness 1/1 and has toxic 1.")
        ruling("2023-02-04", "Multiple instances of toxic are cumulative. For example, if a creature has toxic 2 and gains toxic 1 due to another effect, combat damage that creature deals to a player will cause that player to get 3 poison counters.")
    }
}
