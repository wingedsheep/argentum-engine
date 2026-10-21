package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Orb of Dragonkind
 * {1}{R}
 * Artifact
 * {1}, {T}: Add two mana in any combination of colors. Spend this mana only to cast Dragon spells
 * or activate abilities of Dragons.
 * {R}, {T}, Sacrifice this artifact: Look at the top seven cards of your library. You may reveal a
 * Dragon card from among them and put it into your hand. Put the rest on the bottom of your library
 * in a random order.
 */
val OrbOfDragonkind = card("Orb of Dragonkind") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Add two mana in any combination of colors. Spend this mana only to cast Dragon spells or activate abilities of Dragons.\n{R}, {T}, Sacrifice this artifact: Look at the top seven cards of your library. You may reveal a Dragon card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaInAnyCombination(
            amount = 2,
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Dragon"),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 7,
            filter = GameObjectFilter.Any.withSubtype("Dragon"),
            prompt = "You may reveal a Dragon card from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a1c2cdd-353c-4747-bf0e-b995dd37ffca.jpg?1783926473"
    }
}
