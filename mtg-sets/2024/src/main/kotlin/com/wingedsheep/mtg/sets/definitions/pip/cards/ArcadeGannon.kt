package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arcade Gannon — Fallout #92
 * {2}{W}{U} · Legendary Creature — Human Doctor · 2/3
 *
 * {T}: Draw a card, then discard a card. Put a quest counter on Arcade Gannon.
 * For Auld Lang Syne — Once during each of your turns, you may cast an artifact or Human spell from
 * your graveyard with mana value less than or equal to the number of quest counters on Arcade Gannon.
 *
 * The graveyard permission's filter is judged with the granting permanent as its `Self`, so
 * `countersOnSelf(QUEST)` reads this Arcade Gannon's quest counters when the cast is offered and
 * again when it is made. `oncePerTurn` is tracked per granting object, so a new Arcade Gannon grants
 * a fresh cast (per the card's rulings).
 */
val ArcadeGannon = card("Arcade Gannon") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Doctor"
    power = 2
    toughness = 3
    oracleText = "{T}: Draw a card, then discard a card. Put a quest counter on Arcade Gannon.\n" +
        "For Auld Lang Syne — Once during each of your turns, you may cast an artifact or Human spell " +
        "from your graveyard with mana value less than or equal to the number of quest counters on Arcade Gannon."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1) then
            Patterns.Hand.discardCards(1) then
            Effects.AddCounters(counterType = CounterType.QUEST, count = 1, target = EffectTarget.Self)
    }

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = (GameObjectFilter.Artifact or GameObjectFilter.Any.withSubtype("Human"))
                .manaValueAtMostDynamic(DynamicAmounts.countersOnSelf(CounterType.QUEST)),
            duringYourTurnOnly = true,
            oncePerTurn = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36356522-15fc-41e2-bf14-a745e5a96a50.jpg?1783912393"
    }
}
