package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Great Hall of Starnheim
 * Land
 * This land enters tapped.
 * {T}: Add {B}.
 * {W}{W}{B}, {T}, Sacrifice this land and a creature you control: Create a 4/4 white Angel Warrior
 * creature token with flying and vigilance. Activate only as a sorcery.
 *
 * The Orzhov realm land. "This land and a creature you control" are two different objects, so the
 * creature half is `SacrificeAnother` — if the land has been animated it cannot pay both halves alone.
 */
val GreatHallOfStarnheim = card("Great Hall of Starnheim") {
    manaCost = ""
    colorIdentity = "WB"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B}.\n" +
        "{W}{W}{B}, {T}, Sacrifice this land and a creature you control: Create a 4/4 white Angel Warrior creature token with flying and vigilance. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    // {T}: Add {B}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK, 1)
        manaAbility = true
    }

    // {W}{W}{B}, {T}, Sacrifice this land and a creature you control: Create a 4/4 white Angel
    // Warrior creature token with flying and vigilance. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{W}{B}"),
            Costs.Tap,
            Costs.SacrificeSelf,
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel", "Warrior"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd871a3-3802-4bdc-8033-3ecfd01c0449.jpg?1783928084",
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "259"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a23c757e-5944-47ce-b06f-27b4c403044c.jpg?1783928176"
    }
}
