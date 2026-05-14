package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Steelswarm Operator
 * {1}{U}
 * Artifact Creature — Robot Soldier
 * Flying
 * {T}: Add {U}. Spend this mana only to cast an artifact spell.
 * {T}: Add {U}{U}. Spend this mana only to activate abilities of artifact sources.
 * 1/1
 */
val SteelswarmOperator = card("Steelswarm Operator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Soldier"
    power = 1
    toughness = 1
    oracleText = "Flying\n{T}: Add {U}. Spend this mana only to cast an artifact spell.\n{T}: Add {U}{U}. Spend this mana only to activate abilities of artifact sources."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE, 1, restriction = ManaRestriction.ArtifactSpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {U}. Spend this mana only to cast an artifact spell."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE, 2, restriction = ManaRestriction.ArtifactSourceAbilitiesOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {U}{U}. Spend this mana only to activate abilities of artifact sources."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Cristi Balanescu"
        flavorText = "It harnesses energy from the brutal, mana-rich storms found deep within Uthros."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca468b86-f31a-4cd7-a574-eb984bc4bc3e.jpg?1753683154"
        ruling(
            "2025-07-25",
            "An \"artifact source\" is any object with the card type artifact. This means you could " +
                "spend the mana from Steelswarm Operator's last ability to activate an ability of an " +
                "artifact you control or an artifact card in your hand or graveyard, for example."
        )
    }
}
