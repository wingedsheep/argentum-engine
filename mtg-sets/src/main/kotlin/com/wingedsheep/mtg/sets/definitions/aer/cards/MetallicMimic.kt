package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantChosenSubtype

/**
 * Metallic Mimic
 * {2}
 * Artifact Creature — Shapeshifter
 * 2/1
 *
 * As this creature enters, choose a creature type.
 * This creature is the chosen type in addition to its other types.
 * Each other creature you control of the chosen type enters with an additional +1/+1 counter on it.
 *
 * Same shape as Adaptive Automaton, with a counter-granting replacement in place of the lord:
 * [EntersWithChoice] captures the creature type as Mimic enters, [GrantChosenSubtype] (default
 * filter: the source itself) makes it that type in addition to Shapeshifter, and
 * [EntersWithCounters] is a runtime replacement consulted from the battlefield — `appliesTo`
 * describes the *affected* permanents, so `withChosenSubtype()` reads Mimic's stored choice and
 * matches creatures you control entering as that type.
 *
 * "Each *other* creature" is `otherOnly = true`: the entering permanent's own entry path applies its
 * printed enters-with effects regardless of `appliesTo` (that's how the ordinary self-counter cards
 * work), so without the flag the Mimic would counter itself as it entered. Creatures entering
 * *simultaneously* with the Mimic get no counter for a different reason and need no flag — the
 * runtime replacement is only consulted from the battlefield, where the Mimic isn't yet.
 */
val MetallicMimic = card("Metallic Mimic") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    power = 2
    toughness = 1
    oracleText = "As this creature enters, choose a creature type.\n" +
        "This creature is the chosen type in addition to its other types.\n" +
        "Each other creature you control of the chosen type enters with an additional +1/+1 " +
        "counter on it."

    // As this creature enters, choose a creature type.
    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    // Each other creature you control of the chosen type enters with an additional +1/+1 counter.
    replacementEffect(
        EntersWithCounters(
            count = 1,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withChosenSubtype(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    // This creature is the chosen type in addition to its other types.
    staticAbility {
        ability = GrantChosenSubtype()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1aa4eba9-9e91-4beb-9296-a18baa73a318.jpg?1783936724"
        ruling(
            "2017-02-09",
            "The choice of creature type is made as Metallic Mimic enters the battlefield. Players " +
                "can't respond to this choice. Metallic Mimic's second ability starts applying " +
                "immediately."
        )
        ruling(
            "2017-02-09",
            "Even though Metallic Mimic is a Shapeshifter, other Shapeshifter creatures you control " +
                "won't get a +1/+1 counter unless you chose Shapeshifter as Metallic Mimic entered " +
                "the battlefield."
        )
        ruling(
            "2017-02-09",
            "You must choose an existing creature type. \"Artifact\" and \"Vehicle\" aren't creature " +
                "types."
        )
        ruling(
            "2017-02-09",
            "Creatures of the chosen type that enter the battlefield at the same time as Metallic " +
                "Mimic won't enter with an additional +1/+1 counter."
        )
    }
}
