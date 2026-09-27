package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Strange Inversion
 * {2}{R}
 * Instant — Arcane
 * Switch target creature's power and toughness until end of turn.
 * Splice onto Arcane {1}{R}
 */
val StrangeInversion = card("Strange Inversion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant — Arcane"
    oracleText = "Switch target creature's power and toughness until end of turn.\n" +
        "Splice onto Arcane {1}{R} (As you cast an Arcane spell, you may reveal this card from your " +
        "hand and pay its splice cost. If you do, add this card's effects to that spell.)"

    splice("{1}{R}")

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.SwitchPowerToughness(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Khang Le"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/316a78af-a991-4a5e-bdc2-f39947b89bb2.jpg?1783944295"
        ruling("2021-03-19", "Effects that switch a creature's power and toughness apply after all other effects, regardless of when those effects began to apply. For instance, if you target a 1/2 creature then give it +2/+0 later in the turn, it's a 2/3 creature, not a 4/1 creature.")
        ruling("2021-03-19", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature may become lethal if you switch its power and toughness during that turn.")
    }
}
