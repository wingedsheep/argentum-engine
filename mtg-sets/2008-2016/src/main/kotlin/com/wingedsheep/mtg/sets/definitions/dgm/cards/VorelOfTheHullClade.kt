package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vorel of the Hull Clade
 * {1}{G}{U}
 * Legendary Creature — Human Merfolk
 * 1/4
 * {G}{U}, {T}: Double the number of each kind of counter on target artifact, creature, or land.
 *
 * [Effects.DoubleAllCounters] doubles every counter kind on the target independently (each kind is
 * put on as its own placement, so per-kind placement replacements apply). Any controller's
 * permanent is a legal target.
 */
val VorelOfTheHullClade = card("Vorel of the Hull Clade") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Human Merfolk"
    power = 1
    toughness = 4
    oracleText = "{G}{U}, {T}: Double the number of each kind of counter on target artifact, creature, or land."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{U}"), Costs.Tap)
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land))
        effect = Effects.DoubleAllCounters(permanent)
        description = "Double the number of each kind of counter on target artifact, creature, or land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Mike Bierek"
        flavorText = "\"I used to hurl rocks and eat scraps of meat burned over a fire. Look at what I've become " +
            "and tell me Simic does not hold infinite possibility.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db0665d4-d974-4d5e-ba29-7bf40cbbe29c.jpg?1783940019"
        ruling(
            "2013-04-15",
            "The effect of Vorel's ability will essentially double the counters on the target artifact, creature, " +
                "or land. For example, if a creature has three +1/+1 counters and a divinity counter on it before " +
                "the ability resolves, it will have six +1/+1 counters and two divinity counters on it after the " +
                "ability resolves."
        )
    }
}
