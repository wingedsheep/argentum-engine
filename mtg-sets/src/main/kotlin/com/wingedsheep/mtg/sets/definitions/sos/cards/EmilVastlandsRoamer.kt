package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Emil, Vastlands Roamer — Secrets of Strixhaven #146
 * {2}{G} · Legendary Creature — Elf Druid · 3/3
 *
 * Creatures you control with +1/+1 counters on them have trample.
 * {4}{G}, {T}: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it,
 * where X is the number of differently named lands you control.
 *
 * The static is a [GrantKeyword] lord over `creatures you control with a +1/+1 counter on them`
 * (`withCounter(PLUS_ONE_PLUS_ONE)`), so it applies in the projection layers and updates live as
 * counters come and go.
 *
 * The activated ability composes atoms:
 *  - [Effects.CreateToken] makes one 0/0 green/blue Fractal, publishing it under the well-known
 *    [CREATED_TOKENS] pipeline collection.
 *  - [Effects.AddCountersToCollection] (dynamic-amount overload) then puts X +1/+1 counters on
 *    exactly that token, X = the number of differently named lands you control
 *    (`DynamicAmounts.battlefield(You, Land).distinctNames()`, evaluated once at resolution).
 */
val EmilVastlandsRoamer = card("Emil, Vastlands Roamer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Druid"
    power = 3
    toughness = 3
    oracleText = "Creatures you control with +1/+1 counters on them have trample.\n" +
        "{4}{G}, {T}: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters " +
        "on it, where X is the number of differently named lands you control."

    // Creatures you control with +1/+1 counters on them have trample.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE),
            ),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value),
            imageUri = "https://cards.scryfall.io/normal/front/8/b/8b5f1fdb-04df-4224-acb4-7819c37565f5.jpg?1782723480"
        ).then(
            Effects.AddCountersToCollection(
                CREATED_TOKENS,
                Counters.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).distinctNames(),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3654416d-8558-4af2-9e10-18dbc8f2b376.jpg?1775937993"
    }
}
