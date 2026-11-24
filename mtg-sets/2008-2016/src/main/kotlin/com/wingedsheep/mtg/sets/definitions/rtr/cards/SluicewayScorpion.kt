package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sluiceway Scorpion {2}{B}{G}
 * Creature — Scorpion
 * 2/2
 *
 * Deathtouch
 * Scavenge {1}{B}{G} ({1}{B}{G}, Exile this card from your graveyard: Put a number of +1/+1
 * counters equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val SluicewayScorpion = card("Sluiceway Scorpion") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Scorpion"
    power = 2
    toughness = 2
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "Scavenge {1}{B}{G} ({1}{B}{G}, Exile this card from your graveyard: Put a number of +1/+1 counters " +
        "equal to this card's power on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {1}{B}{G}"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b6dbadf-a6f7-4876-9c3f-44e4a33b2bee.jpg"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}
