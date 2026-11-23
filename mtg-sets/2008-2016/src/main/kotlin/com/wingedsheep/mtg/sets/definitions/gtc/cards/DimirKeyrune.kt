package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dimir Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {U} or {B}.
 * {U}{B}: This artifact becomes a 2/2 blue and black Horror artifact creature until end of turn
 * and can't be blocked this turn.
 *
 * Same shape as the other Keyrunes: an additive `BecomeCreature` on itself, followed by an
 * end-of-turn `CANT_BE_BLOCKED` grant on itself.
 */
val DimirKeyrune = card("Dimir Keyrune") {
    manaCost = "{3}"
    colorIdentity = "UB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U} or {B}.\n" +
        "{U}{B}: This artifact becomes a 2/2 blue and black Horror artifact creature until end of turn and can't be blocked this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{U}{B}")
        effect = Effects.BecomeCreature(
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Horror"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.BLUE.name, Color.BLACK.name)
        ) then Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d91bb34-5d8d-48c9-ad19-d28884e083bc.jpg?1783940093"
    }
}
