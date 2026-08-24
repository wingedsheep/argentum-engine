package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasCreatureTypesOf
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Duplicant — Mirrodin #165
 * {6} · Artifact Creature — Shapeshifter · Rare · 2/4
 *
 * Imprint — When this creature enters, you may exile target nontoken creature.
 * As long as a card exiled with this creature is a creature card, this creature has the power,
 * toughness, and creature types of the last creature card exiled with it. It's still a Shapeshifter.
 *
 * Modelling notes:
 * - The two halves are a *linked* pair (CR 607): `Effects.ExileLinkedToSource` writes the pile and
 *   both statics read it through [EntityReference.LinkedExiledCard]. The exiled *card* is what is
 *   read — not the permanent that was exiled — so a creature that dies out of exile, or an imprint
 *   the controller declined, simply leaves Duplicant a printed 2/4 Shapeshifter.
 * - "As long as a card exiled with this creature is a creature card" is the gate, not a card-level
 *   flag: `Conditions.LinkedExiledCardMatches(Filters.Creature)` is dual-mode, so the projector
 *   re-asks it every pass and both halves switch on and off together.
 * - Two statics rather than one because the sentence spans two Rule 613 layers: the creature types
 *   are Layer 4 ([HasCreatureTypesOf]) and the P/T is Layer 7b
 *   ([SetBasePowerToughnessDynamicStatic] fed `EntityProperty(LinkedExiledCard, Power/Toughness)`).
 *   Both are dynamic, which is what the ruling "Duplicant's power and toughness are constantly
 *   updated if the exiled card's power and/or toughness change" requires — a snapshot taken at ETB
 *   would be wrong.
 * - Setting *base* P/T is also what makes the second ruling fall out for free: counters and other
 *   P/T modifiers still apply on top, because Layer 7b runs before 7c/7d.
 * - `retainedTypes = {"Shapeshifter"}` is the printed "It's still a Shapeshifter." rider. It rides
 *   on the type static rather than a separate `GrantSubtype`, which would land in the same layer
 *   with the same timestamp and could be wiped by the type set.
 */
val Duplicant = card("Duplicant") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    power = 2
    toughness = 4
    oracleText = "Imprint — When this creature enters, you may exile target nontoken creature.\n" +
        "As long as a card exiled with this creature is a creature card, this creature has the " +
        "power, toughness, and creature types of the last creature card exiled with it. It's " +
        "still a Shapeshifter."

    // "Imprint — When this creature enters, you may exile target nontoken creature."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val exiled = target(
            "target nontoken creature",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.nontoken()))
        )
        effect = Effects.ExileLinkedToSource(exiled)
        description = "Imprint — When this creature enters, you may exile target nontoken creature."
    }

    // "... this creature has the ... creature types of the last creature card exiled with it.
    //  It's still a Shapeshifter."
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = HasCreatureTypesOf(
                source = EntityReference.LinkedExiledCard(),
                retainedTypes = setOf("Shapeshifter")
            ),
            condition = Conditions.LinkedExiledCardMatches(Filters.Creature)
        )
    }

    // "... this creature has the power, toughness ... of the last creature card exiled with it."
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = SetBasePowerToughnessDynamicStatic(
                power = DynamicAmount.EntityProperty(
                    EntityReference.LinkedExiledCard(),
                    EntityNumericProperty.Power
                ),
                toughness = DynamicAmount.EntityProperty(
                    EntityReference.LinkedExiledCard(),
                    EntityNumericProperty.Toughness
                )
            ),
            condition = Conditions.LinkedExiledCardMatches(Filters.Creature)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d48a96f2-738f-433f-bfae-fbf378832a3b.jpg?1783944522"
    }
}
