package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Flywheel Racer
 * {2}
 * Artifact — Vehicle
 * 3/2
 * Vigilance
 * {T}: Add one mana of any color. Activate only if this permanent is a creature.
 * Crew 1
 *
 * The "only if this permanent is a creature" gate is `SourceMatches(Creature)`, read against
 * projected state — so it opens once Crew 1 animates the Vehicle. Being a creature then, its
 * {T} mana ability is also subject to summoning sickness (CR 302.6).
 */
val FlywheelRacer = card("Flywheel Racer") {
    manaCost = "{2}"
    typeLine = "Artifact — Vehicle"
    oracleText = "Vigilance\n" +
        "{T}: Add one mana of any color. Activate only if this permanent is a creature.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 2

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceMatches(GameObjectFilter.Creature))
        )
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c694485d-a753-4e55-929c-d8e4a53c7d08.jpg?1783916936"
    }
}
