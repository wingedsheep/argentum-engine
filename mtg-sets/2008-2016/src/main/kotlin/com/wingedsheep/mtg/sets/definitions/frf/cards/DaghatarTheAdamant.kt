package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Daghatar the Adamant — Fate Reforged #9
 * {3}{W} · Legendary Creature — Human Warrior · 0/0
 *
 * The move ability is Combine Guildmage's two-slot shape over any creatures: the second slot is a
 * [TargetOther] (the two targets must differ; either may be Daghatar), and [Effects.MoveCounters]
 * moves one +1/+1 counter, capped at what the first target has. Resolution-time legality drops an
 * illegal target's slot, so the counter moves only while both targets are still legal.
 */
val DaghatarTheAdamant = card("Daghatar the Adamant") {
    manaCost = "{3}{W}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Human Warrior"
    power = 0
    toughness = 0
    oracleText = "Vigilance\n" +
        "Daghatar enters with four +1/+1 counters on it.\n" +
        "{1}{B/G}{B/G}: Move a +1/+1 counter from target creature onto a second target creature."

    keywords(Keyword.VIGILANCE)

    replacementEffect(EntersWithCounters(count = 4, selfOnly = true))

    activatedAbility {
        cost = Costs.Mana("{1}{B/G}{B/G}")
        val from = target(TargetFilter.Creature)
        val onto = target(TargetOther(TargetObject(filter = TargetFilter.Creature), excludeSource = false))
        effect = Effects.MoveCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.fixed(1),
            source = from,
            destination = onto
        )
        description = "Move a +1/+1 counter from target creature onto a second target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Zack Stella"
        flavorText = "\"Our victories will protect a thousand generations.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b7eed1e-7b28-4900-a6ac-67f167476c11.jpg?1783938714"
        ruling("2014-11-24", "The two targets of the last ability must be different creatures. Either one may be Daghatar the Adamant.")
        ruling("2014-11-24", "The +1/+1 counter is moved only if both targets are still legal as the ability resolves.")
        ruling("2014-11-24", "To move a counter from one creature to another, the counter is removed from the first creature and placed on the second. Any abilities that care about a counter being placed on the second creature will apply.")
    }
}
