package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Golgari Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {B} or {G}.
 * {B}{G}: This artifact becomes a 2/2 black and green Insect artifact creature with deathtouch
 * until end of turn.
 *
 * Same shape as the other Keyrunes: the animation is additive, `addTypes` restates the printed
 * "artifact creature", and the default target/duration is the Keyrune itself until end of turn.
 */
val GolgariKeyrune = card("Golgari Keyrune") {
    manaCost = "{3}"
    colorIdentity = "BG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B} or {G}.\n" +
        "{B}{G}: This artifact becomes a 2/2 black and green Insect artifact creature with deathtouch until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{B}{G}")
        effect = Effects.BecomeCreature(
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.DEATHTOUCH),
            creatureTypes = setOf("Insect"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.BLACK.name, Color.GREEN.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Daniel Ljunggren"
        flavorText = "Like a single ant, it only hints at the colony below."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/913b803f-ba82-4660-86be-49677d1e32c9.jpg?1783940324"
    }
}
