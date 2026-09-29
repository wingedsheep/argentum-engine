package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Placid Rottentail
 * {G}
 * Creature — Fungus Rabbit
 * 1/1
 * Vigilance
 * {2}{G}, Exile this card from your graveyard: Put two +1/+1 counters on target creature.
 * Activate only as a sorcery.
 */
val PlacidRottentail = card("Placid Rottentail") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus Rabbit"
    power = 1
    toughness = 1
    oracleText = "Vigilance\n{2}{G}, Exile this card from your graveyard: Put two +1/+1 counters on " +
        "target creature. Activate only as a sorcery."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.ExileSelf)
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 2, target = creature)
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Filip Burburan"
        flavorText = "It had always loved munching on daisies and crystalweed, but lately its favorite " +
            "snacks were the strange, round roots popping up everywhere."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a87c5d2-3ebc-442b-8618-963cfc63855f.jpg?1783916964"
    }
}
