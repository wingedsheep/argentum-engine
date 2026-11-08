package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deepwood Denizen — Modern Horizons 2 #155 (canonical printing)
 * {2}{G} · Creature — Elf Warrior · 3/2 · Common
 *
 * Vigilance
 * {5}{G}, {T}: Draw a card. This ability costs {1} less to activate for each +1/+1 counter on
 * creatures you control.
 *
 * The discount rides `genericCostReduction` (Battlefield Butcher's shape), fed the total +1/+1
 * counters across your creatures (Rumbling Ruin's count). Only the generic {5} shrinks, so the
 * ability bottoms out at {G}, {T}.
 */
val DeepwoodDenizen = card("Deepwood Denizen") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 3
    toughness = 2
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "{5}{G}, {T}: Draw a card. This ability costs {1} less to activate for each +1/+1 counter " +
        "on creatures you control."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}"), AbilityCost.Tap)
        effect = Effects.DrawCards(1)
        genericCostReduction = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature)
            .totalCounters(CounterType.PLUS_ONE_PLUS_ONE)
        description = "{5}{G}, {T}: Draw a card. This ability costs {1} less to activate for each " +
            "+1/+1 counter on creatures you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Josu Hernaiz"
        flavorText = "\"You didn't come all this way just to bother me, did you? Say your piece and be gone.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/333f02f7-3b8a-41e3-9ae5-2151539e64ad.jpg?1783926833"
    }
}
