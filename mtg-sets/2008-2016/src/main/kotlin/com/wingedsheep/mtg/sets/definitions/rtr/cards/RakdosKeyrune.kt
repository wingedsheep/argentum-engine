package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rakdos Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {B} or {R}.
 * {B}{R}: This artifact becomes a 3/1 black and red Devil artifact creature with first strike
 * until end of turn.
 *
 * Same shape as the other Keyrunes: the animation is additive, `addTypes` restates the printed
 * "artifact creature", and the default target/duration is the Keyrune itself until end of turn.
 */
val RakdosKeyrune = card("Rakdos Keyrune") {
    manaCost = "{3}"
    colorIdentity = "BR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B} or {R}.\n" +
        "{B}{R}: This artifact becomes a 3/1 black and red Devil artifact creature with first strike until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{B}{R}")
        effect = Effects.BecomeCreature(
            power = 3,
            toughness = 1,
            keywords = setOf(Keyword.FIRST_STRIKE),
            creatureTypes = setOf("Devil"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.BLACK.name, Color.RED.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Daniel Ljunggren"
        flavorText = "The Rakdos use it for everything you can think of and plenty more than that."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6124b4c-49a1-42e3-a6ff-62de231c7823.jpg?1783940323"
    }
}
