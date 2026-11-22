package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fluxcharger
 * {2}{U}{R}
 * Creature — Weird
 * 1/5
 * Flying
 * Whenever you cast an instant or sorcery spell, you may switch this creature's power and
 * toughness until end of turn.
 */
val Fluxcharger = card("Fluxcharger") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Weird"
    power = 1
    toughness = 5
    oracleText = "Flying\nWhenever you cast an instant or sorcery spell, you may switch this creature's " +
        "power and toughness until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.May(Effects.SwitchPowerToughness(EffectTarget.Self))
        description = "Whenever you cast an instant or sorcery spell, you may switch this creature's " +
            "power and toughness until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Willian Murai"
        flavorText = "One part mizzium, two parts elemental energy, eleven parts danger."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c58f6ed-2544-4b58-8dc0-a0a37b9547e6.jpg?1783940029"
        ruling("2021-03-19", "Effects that switch a creature's power and toughness apply after all other effects, regardless of when those effects began to apply. For instance, if you target a 1/2 creature then give it +2/+0 later in the turn, it's a 2/3 creature, not a 4/1 creature.")
        ruling("2021-03-19", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature may become lethal if you switch its power and toughness during that turn.")
        ruling("2021-03-19", "Switching a creature's power and toughness twice (or any even number of times) effectively returns the creature to the power and toughness it had before any switches.")
    }
}
