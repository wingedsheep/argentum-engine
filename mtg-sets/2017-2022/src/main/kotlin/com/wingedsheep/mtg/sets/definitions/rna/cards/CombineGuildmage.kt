package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Combine Guildmage — Ravnica Allegiance #163
 * {G}{U} · Creature — Merfolk Wizard · 2 / 2
 *
 * The Simic entry in the RNA Guildmage cycle.
 *  - The {1}{G} ability is Arlinn, the Pack's Hope's counter rider with a "this turn" duration: an
 *    [EntersWithCounters] replacement granted to the controller via
 *    [Effects.GrantReplacementEffect] until end of turn, so it outlives the Guildmage.
 *  - The {1}{U} ability is Nesting Grounds' two-slot shape: the second target is a [TargetOther]
 *    (must differ from the first, may be the Guildmage itself), and [Effects.MoveCounters] moves
 *    one +1/+1 counter, capped at what the first target actually has.
 */
val CombineGuildmage = card("Combine Guildmage") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "{1}{G}, {T}: This turn, each creature you control enters with an additional +1/+1 counter on it.\n" +
        "{1}{U}, {T}: Move a +1/+1 counter from target creature you control onto another target creature you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
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
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val from = target(TargetFilter.CreatureYouControl)
        val onto = target(TargetOther(TargetObject(filter = TargetFilter.CreatureYouControl), excludeSource = false))
        effect = Effects.MoveCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.fixed(1),
            source = from,
            destination = onto
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a6e2816-d50b-41a5-b503-faa58dc7c94a.jpg?1783933655"
    }
}
