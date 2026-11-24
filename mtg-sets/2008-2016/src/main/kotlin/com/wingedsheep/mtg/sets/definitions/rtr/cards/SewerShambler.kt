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
 * Sewer Shambler {2}{B}
 * Creature — Zombie
 * 2/1
 *
 * Swampwalk
 * Scavenge {2}{B} ({2}{B}, Exile this card from your graveyard: Put a number of +1/+1 counters
 * equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge uses the same graveyard-activated shape as Drudge Beetle.
 */
val SewerShambler = card("Sewer Shambler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)\n" +
        "Scavenge {2}{B} ({2}{B}, Exile this card from your graveyard: Put a number of +1/+1 counters " +
        "equal to this card's power on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.SWAMPWALK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {2}{B}"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ae7ba14-c901-4266-ae31-812e001916d3.jpg?1783940360"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}
