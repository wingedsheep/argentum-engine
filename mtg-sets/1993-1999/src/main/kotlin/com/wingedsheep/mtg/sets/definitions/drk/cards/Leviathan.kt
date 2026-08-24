package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessSacrifice
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leviathan
 * {5}{U}{U}{U}{U}
 * Creature — Leviathan
 * 10/10
 * Trample
 * This creature enters tapped and doesn't untap during your untap step.
 * At the beginning of your upkeep, you may sacrifice two Islands. If you do, untap this creature.
 * This creature can't attack unless you sacrifice two Islands. (This cost is paid as attackers
 * are declared.)
 *
 * Two Islands to wake it, two more every time it swings. The first three lines are existing
 * vocabulary — `EntersTapped`, the [AbilityFlag.DOESNT_UNTAP] self-suppression Colossus of Sardia
 * uses, and an optional upkeep trigger whose cost is an ordinary sacrifice.
 *
 * The last line is not: it is a **cost** paid as attackers are declared — the clause is a
 * restriction (CR 508.1c) and its cost is determined and paid at CR 508.1h–j — and the engine only
 * had generic-mana attack taxes. `CantAttackUnlessSacrifice` adds the non-mana form,
 * split across two places that share one helper so they cannot disagree — the declaration is
 * illegal up front when the controller can't pay, and the declare-attackers step then pauses to
 * ask *which* Islands, in the same window the mana tax is paid.
 *
 * Note the cost is per creature, not per attack: two Leviathans attacking together owe two Islands
 * each, and are asked separately so each choice is made knowing the last one.
 */
private val islands = GameObjectFilter.Land.withSubtype(Subtype.ISLAND)

val Leviathan = card("Leviathan") {
    manaCost = "{5}{U}{U}{U}{U}"
    typeLine = "Creature — Leviathan"
    power = 10
    toughness = 10
    oracleText = "Trample\nThis creature enters tapped and doesn't untap during your untap step.\n" +
        "At the beginning of your upkeep, you may sacrifice two Islands. If you do, untap this " +
        "creature.\nThis creature can't attack unless you sacrifice two Islands. (This cost is " +
        "paid as attackers are declared.)"

    keywords(Keyword.TRAMPLE)
    flags(AbilityFlag.DOESNT_UNTAP)

    replacementEffect(EntersTapped())

    staticAbility {
        ability = CantAttackUnlessSacrifice(
            sacrificeFilter = islands,
            count = 2,
        )
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        // Gated on actually controlling two Islands, so accepting with none does not untap for
        // free: a triggered ability has no cost slot, and the sacrifice is the price of the untap.
        effect = ConditionalEffect(
            condition = Conditions.YouControlAtLeast(2, islands),
            effect = Effects.Composite(
                Effects.Sacrifice(islands, count = 2, target = EffectTarget.Controller),
                Effects.Untap(EffectTarget.Self),
            ),
        )
        description = "At the beginning of your upkeep, you may sacrifice two Islands. If you do, " +
            "untap this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b638d9be-c533-45c3-92f9-fabf56edc2df.jpg?1783947942"
    }
}
