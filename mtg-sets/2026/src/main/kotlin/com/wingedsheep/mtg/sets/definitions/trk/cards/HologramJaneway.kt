package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hologram Janeway — Star Trek #21
 * {2}{W} · Legendary Artifact Creature — Illusion Officer · 3/3
 *
 * Whenever you attack while creatures you control have total power 8 or greater, put a +1/+1
 * counter on target creature.
 *
 * "While …" gates the trigger itself, so the total-power check is a `triggerRestriction` made as
 * attackers are declared, not an intervening-if rechecked on resolution.
 */
val HologramJaneway = card("Hologram Janeway") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact Creature — Illusion Officer"
    oracleText = "Whenever you attack while creatures you control have total power 8 or greater, " +
        "put a +1/+1 counter on target creature."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Khadijah Khatib"
        flavorText = "\"I am hologram Janeway, your training advisor for exploring the greater " +
            "Galaxy. On behalf of Starfleet, welcome aboard.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02d29047-a7b8-43fb-8b90-b35ba34da5f9.jpg?1793100636"
    }
}
