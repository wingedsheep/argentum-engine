package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Castle Doom
 * Land
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast an artifact spell.
 * {3}, {T}, Sacrifice an artifact: Create a 3/3 colorless Robot Villain artifact creature token
 * named Doombot. Activate only as a sorcery.
 *
 * Implementation note: the filtered mana is spell-only — the oracle says "cast an artifact
 * spell" with no ability clause, hence `allowAbilities = false` on
 * [ManaRestriction.CardTypeSpellsOrAbilitiesOnly]. The Doombot is the registered
 * `PredefinedTokens.Doombot` (shared with Doctor Doom), so the name and type line come from one
 * canonical definition. "Activate only as a sorcery" is [TimingRule.SorcerySpeed]; the artifact
 * sacrifice is part of the activation cost, so it's paid on activation, not on resolution.
 */
val CastleDoom = card("Castle Doom") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast an artifact spell.\n" +
        "{3}, {T}, Sacrifice an artifact: Create a 3/3 colorless Robot Villain artifact creature " +
        "token named Doombot. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            amount = 1,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = true,
                allowAbilities = false,
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Artifact),
        )
        effect = CreatePredefinedTokenEffect("Doombot")
        timing = TimingRule.SorcerySpeed
        description = "Create a 3/3 colorless Robot Villain artifact creature token named " +
            "Doombot. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "263"
        artist = "Nino Is"
        flavorText = "Those in Doom's shadow know his protection. And his wrath."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b39d7a6-ca2d-4376-a18e-efd0138e83bc.jpg?1783902886"
    }
}
