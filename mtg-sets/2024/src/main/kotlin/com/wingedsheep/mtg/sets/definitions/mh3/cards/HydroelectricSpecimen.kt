package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hydroelectric Specimen {2}{U} // Hydroelectric Laboratory
 * Creature — Weird 1/4
 * Flash
 * When this creature enters, you may change the target of target instant or sorcery spell with a
 * single target to this creature.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {U}.
 *
 * "With a single target" is a targeting restriction (`withSingleTarget()`), so a spell with several
 * targets can't be chosen at all. The redirect names its new target, so nothing is offered: the
 * spell's target becomes this creature only if it's a legal target for that spell (CR 115.7a).
 */
private val HydroelectricSpecimenFront = card("Hydroelectric Specimen") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Weird"
    power = 1
    toughness = 4
    oracleText = "Flash\nWhen this creature enters, you may change the target of target instant or " +
        "sorcery spell with a single target to this creature."

    keywords(Keyword.FLASH)

    triggeredAbility {
        target(TargetFilter.InstantOrSorcerySpellOnStack.withSingleTarget())
        trigger = Triggers.self.enters()
        optional = true
        effect = Effects.ChangeTarget(to = EffectTarget.Self)
        description = "When this creature enters, you may change the target of target instant or " +
            "sorcery spell with a single target to this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8689ecd7-e9a6-458b-99d2-6dbaca527f00.jpg?1783911234"
        ruling("2024-06-07", "The spell's target is changed to Hydroelectric Specimen only if Hydroelectric Specimen is a legal target for that spell.")
        ruling("2024-06-07", "If a spell has multiple targets, you can't target it with Hydroelectric Specimen's ability, even if only one of those targets is a creature or all but one of those targets have become illegal.")
    }
}

private val HydroelectricLaboratoryBack = card("Hydroelectric Laboratory") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/back/8/6/8689ecd7-e9a6-458b-99d2-6dbaca527f00.jpg?1783911234"
    }
}

val HydroelectricSpecimen: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = HydroelectricSpecimenFront,
    backFace = HydroelectricLaboratoryBack,
)
