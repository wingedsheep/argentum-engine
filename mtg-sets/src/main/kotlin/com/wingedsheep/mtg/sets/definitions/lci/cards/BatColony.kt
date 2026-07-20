package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.model.Rarity

/**
 * Bat Colony
 * {2}{W}
 * Enchantment
 *
 * When this enchantment enters, create a 1/1 black Bat creature token with flying for each mana
 * from a Cave spent to cast it.
 * Whenever a Cave you control enters, put a +1/+1 counter on target creature you control.
 *
 * The Bat count reads [DynamicAmount.ManaSpentFromSubtype]`(Subtype.CAVE)` — the mana-source
 * provenance the engine records at production ([ManaProvenanceTracker]) and stamps onto the resolved
 * permanent's `CastRecordComponent`, so the enters-the-battlefield ability can count how many of the
 * mana units spent to cast Bat Colony came from Caves. A Bat Colony put onto the battlefield without
 * being cast (or paid for with no Cave mana) makes zero Bats.
 *
 * The Cave-enters trigger is an ANY-binding enters trigger filtered to Caves you control, so it
 * fires for every Cave (land) that enters under your control after Bat Colony is out — including
 * the turn's later land drops — putting a +1/+1 counter on a target creature you control.
 */
val BatColony = card("Bat Colony") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 1/1 black Bat creature token with flying " +
        "for each mana from a Cave spent to cast it.\n" +
        "Whenever a Cave you control enters, put a +1/+1 counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            count = DynamicAmount.ManaSpentFromSubtype(Subtype.CAVE),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Bat"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/1/0/100c0127-49dd-4a78-9c88-1881e7923674.jpg?1721425184",
        )
        description = "When this enchantment enters, create a 1/1 black Bat creature token with " +
            "flying for each mana from a Cave spent to cast it."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Land.withSubtype("Cave").youControl(),
            binding = TriggerBinding.ANY,
        )
        val t = target("target creature you control", TargetCreature(filter = TargetFilter.Creature.youControl()))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Whenever a Cave you control enters, put a +1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c02134c-ec9f-4090-820e-8ba7ae4a8c2b.jpg?1782694607"
        ruling("2023-11-10", "The number of Bats created is equal to the number of mana from Caves you control that was spent to cast Bat Colony, not the number of Caves you control.")
    }
}
