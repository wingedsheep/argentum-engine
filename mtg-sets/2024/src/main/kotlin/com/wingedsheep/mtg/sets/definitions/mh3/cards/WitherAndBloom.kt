package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wither and Bloom
 * {1}{B}
 * Instant
 *
 * Target creature gets -3/-3 until end of turn.
 * {1}{B}, Exile this card from your graveyard: Put a +1/+1 counter on target creature you control.
 * Activate only as a sorcery.
 */
val WitherAndBloom = card("Wither and Bloom") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-3 until end of turn.\n" +
        "{1}{B}, Exile this card from your graveyard: Put a +1/+1 counter on target creature you control. " +
        "Activate only as a sorcery."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-3, -3, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Richard Kane Ferguson"
        flavorText = "\"Death itself is nothing to cry about. The real tragedy is letting a death go to waste.\"\n" +
            "—Dina, Witherbloom mage-student"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95c2390f-71f1-4e42-83da-d603ca86a8d0.jpg?1783911274"
    }
}
