package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Taborax, Hope's Demise — Zendikar Rising #129
 * {2}{B} · Legendary Creature — Demon Cleric · Rare · 2/2
 *
 * Flying
 * Taborax has lifelink as long as it has five or more +1/+1 counters on it.
 * Whenever another nontoken creature you control dies, put a +1/+1 counter on Taborax. If that
 * creature was a Cleric, you may draw a card. If you do, you lose 1 life.
 *
 * The Cleric check is not an intervening-if — the counter always lands — so it is a resolution-time
 * `Effects.If` over `Conditions.TriggeringEntityWas`, which reads the dying creature's last-known
 * type line (CR 603.10) rather than the card now in the graveyard.
 */
val TaboraxHopesDemise = card("Taborax, Hope's Demise") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon Cleric"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Taborax has lifelink as long as it has five or more +1/+1 counters on it.\n" +
        "Whenever another nontoken creature you control dies, put a +1/+1 counter on Taborax. If that " +
        "creature was a Cleric, you may draw a card. If you do, you lose 1 life."

    keywords(Keyword.FLYING)

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 5)
        ability = GrantKeyword(Keyword.LIFELINK, Filters.Self)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.If(
                Conditions.TriggeringEntityWas(GameObjectFilter.Creature.withSubtype("Cleric")),
                Effects.May(Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller))
            )
        description = "Whenever another nontoken creature you control dies, put a +1/+1 counter on " +
            "Taborax. If that creature was a Cleric, you may draw a card. If you do, you lose 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "G-host Lee"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/331bf97d-f447-4c68-bfc7-2bb593106d66.jpg?1783929365"
        ruling(
            "2020-09-25",
            "If Taborax is dealt lethal damage at the same time as one or more nontoken creatures you " +
                "control die or are dealt lethal damage, it won't receive a counter from its ability in " +
                "time to save it."
        )
        ruling(
            "2020-09-25",
            "If another nontoken Cleric dies at the same time as Taborax, you won't put a +1/+1 counter " +
                "on anything, but you may draw a card as its last ability resolves. You still lose 1 life " +
                "if you do."
        )
    }
}
