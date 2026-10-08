package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Argoth, Sanctum of Nature
 * Land
 * This land enters tapped unless you control a legendary green creature.
 * {T}: Add {G}.
 * {2}{G}{G}, {T}: Create a 2/2 green Bear creature token, then mill three cards. Activate only as
 * a sorcery.
 * (Melds with Titania, Voice of Gaea.)
 *
 * The meld itself lives on Titania, Voice of Gaea; the reminder line has no rules meaning here.
 */
val ArgothSanctumOfNature = card("Argoth, Sanctum of Nature") {
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control a legendary green creature.\n" +
        "{T}: Add {G}.\n" +
        "{2}{G}{G}, {T}: Create a 2/2 green Bear creature token, then mill three cards. Activate only as a sorcery.\n" +
        "(Melds with Titania, Voice of Gaea.)"

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.YouControl(GameObjectFilter.Creature.legendary().withColor(Color.GREEN))
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Bear"),
            imageUri = "https://cards.scryfall.io/normal/front/7/7/772dac39-269b-4a35-aad3-320279af833f.jpg?1783919910"
        ) then Patterns.Library.mill(3)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b29c9e4f-7b98-4610-a681-ae6297e8fc72.jpg?1783920009"
    }
}
