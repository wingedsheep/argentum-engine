package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zameck Guildmage — Gatecrash #209
 * {G}{U} · Creature — Elf Wizard · 2 / 2
 *
 *  - The first ability is Combine Guildmage's "this turn" counter rider: an [EntersWithCounters]
 *    replacement granted to the controller until end of turn, so it outlives the Guildmage.
 *  - The second is Shapers of Nature's draw ability: removing a +1/+1 counter from a creature you
 *    control is part of the cost.
 */
val ZameckGuildmage = card("Zameck Guildmage") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Wizard"
    power = 2
    toughness = 2
    oracleText = "{G}{U}: This turn, each creature you control enters with an additional +1/+1 counter on it.\n" +
        "{G}{U}, Remove a +1/+1 counter from a creature you control: Draw a card."

    activatedAbility {
        cost = Costs.Mana("{G}{U}")
        effect = Effects.GrantReplacementEffect(
            EntersWithCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.youControl(),
                    to = Zone.BATTLEFIELD
                )
            ),
            EffectTarget.Self,
            Duration.EndOfTurn
        )
        description = "This turn, each creature you control enters with an additional +1/+1 counter on it."
    }
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}{U}"),
            Costs.RemovePlusOnePlusOneCounters(GameObjectFilter.Creature.youControl(), 1)
        )
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feeaf99b-7720-42e3-8cb1-23218b646458.jpg?1783940097"
    }
}
