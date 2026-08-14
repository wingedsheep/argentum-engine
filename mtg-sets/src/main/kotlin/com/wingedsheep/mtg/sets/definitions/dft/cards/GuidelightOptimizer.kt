package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Guidelight Optimizer
 * {1}{U}
 * Artifact Creature — Robot
 * 2/1
 *
 * {T}: Add {U}. Spend this mana only to cast an artifact spell or activate an ability.
 *
 * The restriction is the disjunction the oracle prints: an *artifact spell* narrows by card type
 * (`CardTypeSpellsOrAbilitiesOnly(ARTIFACT, allowSpells = true)`), while "activate an ability" is
 * deliberately unqualified — [ManaRestriction.AbilityActivationOnly], not the artifact-scoped
 * ability branch. Per the printed ruling the mana pays for *any* activated ability, so scoping the
 * ability half to artifact sources would wrongly reject e.g. a creature's pump ability.
 */
val GuidelightOptimizer = card("Guidelight Optimizer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 1
    oracleText = "{T}: Add {U}. Spend this mana only to cast an artifact spell or activate an ability."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE, 1,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                        cardType = CardType.ARTIFACT,
                        allowSpells = true,
                        allowAbilities = false,
                    ),
                    ManaRestriction.AbilityActivationOnly,
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {U}. Spend this mana only to cast an artifact spell or activate an ability."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Mirko Failoni"
        flavorText = "\"Analysis: Systems operating at 125% efficiency. Stress within expected tolerances. Exclamatory: Punch it.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9fc07dd-05b1-49ed-a3ee-46c31b8e0a3d.jpg?1783907909"
        ruling(
            "2025-02-07",
            "You may use mana from this creature to pay the cost of any activated ability, not " +
                "just abilities of artifacts."
        )
    }
}
