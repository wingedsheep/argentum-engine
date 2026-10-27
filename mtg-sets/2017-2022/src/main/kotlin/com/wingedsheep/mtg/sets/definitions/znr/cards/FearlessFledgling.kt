package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fearless Fledgling — Zendikar Rising #15
 * {1}{W} · Creature — Griffin · 1/1
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.
 * It gains flying until end of turn.
 */
val FearlessFledgling = card("Fearless Fledgling") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 1
    toughness = 1
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature. " +
        "It gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature. " +
            "It gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Jesper Ejsing"
        flavorText = "Griffins nest in precarious places, so fledglings often find themselves taking their " +
            "first flights at surprising moments."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02d73eb7-4e76-48d4-90fa-fa972dc0fccb.jpg?1783929420"

        ruling(
            "2024-11-08",
            "A landfall ability triggers whenever a land you control enters for any reason. It " +
                "triggers whenever you play a land, as well as whenever a spell or ability puts a " +
                "land onto the battlefield under your control."
        )
        ruling(
            "2024-11-08",
            "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land."
        )
        ruling(
            "2024-11-08",
            "Whenever a land you control enters, each landfall ability of the permanents you control " +
                "will trigger. You can put them on the stack in any order. The last ability you put " +
                "on the stack will be the first one to resolve (As a result, you can have those " +
                "abilities resolve in the order of your choosing.)."
        )
    }
}
