package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Joraga Auxiliary — Oath of the Gatewatch #154 (canonical printing)
 * {1}{G}{W} · Creature — Elf Soldier Ally · 2/3
 *
 * {4}{G}{W}: Support 2. (Put a +1/+1 counter on each of up to two other target creatures.)
 *
 * Support on a permanent targets *other* creatures (any controller); here it is an activated
 * ability rather than Expedition Raptor's ETB trigger, so "other" excludes the Auxiliary itself.
 */
val JoragaAuxiliary = card("Joraga Auxiliary") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elf Soldier Ally"
    power = 2
    toughness = 3
    oracleText = "{4}{G}{W}: Support 2. (Put a +1/+1 counter on each of up to two other target creatures.)"

    activatedAbility {
        cost = Costs.Mana("{4}{G}{W}")
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "Support 2 — put a +1/+1 counter on each of up to two other target creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "David Gaillet"
        flavorText = "\"Before the battle, I heard them take an oath near the ruins of Sea Gate. I hope " +
            "they inspire others to keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65d920b4-81ab-45be-b763-b6114b879357.jpg?1783937896"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
