package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hazardous Conditions — Kaladesh #182
 * {2}{B}{G} · Sorcery
 *
 * Creatures with no counters on them get -2/-2 until end of turn.
 *
 * "No counters" is counters of any kind (Damning Verdict's `withoutCounters()`). The affected set is
 * locked in as the spell resolves ([Effects.ForEachInGroup], Demon of Dark Schemes shape): creatures
 * that gain a counter later keep the -2/-2, while creatures that enter or lose their counters later
 * are untouched — exactly the rulings.
 */
val HazardousConditions = card("Hazardous Conditions") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Creatures with no counters on them get -2/-2 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withoutCounters()),
            Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"There are many reasons we invent, of which survival is not the least.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daa9b08b-c56f-480e-874e-069e72d979c8.jpg?1783937168"
        ruling(
            "2016-09-20",
            "Hazardous Conditions doesn't care what kind of counter a creature has on it. Whether +1/+1, -1/-1, " +
                "charge, or fate, any kind of counter causes a creature to be unaffected."
        )
        ruling(
            "2016-09-20",
            "The set of creatures affected by Hazardous Conditions is determined as the spell resolves. Creatures " +
                "that get a counter later in the turn will continue to get -2/-2. Creatures that enter the " +
                "battlefield or lose their counters later in the turn and noncreature permanents that become " +
                "creatures later in the turn won't get -2/-2."
        )
    }
}
