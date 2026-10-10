package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spellskite
 * {2}
 * Artifact Creature — Phyrexian Horror
 * 0/4
 *
 * {U/P}: Change a target of target spell or ability to this creature.
 *
 * No "with a single target" restriction: on a multi-target spell or ability, the controller picks
 * which target changes, and only to a slot this creature is a legal target for (CR 115.7a).
 */
val Spellskite = card("Spellskite") {
    manaCost = "{2}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Phyrexian Horror"
    power = 0
    toughness = 4
    oracleText = "{U/P}: Change a target of target spell or ability to this creature. " +
        "({U/P} can be paid with either {U} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{U/P}")
        target(TargetFilter.SpellOrAbilityOnStack)
        effect = Effects.ChangeTarget(to = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Chippy"
        flavorText = "\"Let's show Vorinclex that progress doesn't always need teeth or claws.\"\n—Malcator, Executor of Synthesis"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a84bada-ed6a-4e97-8a0c-05b7cb32d66f.jpg?1783941290"
        ruling("2020-08-07", "You can activate Spellskite's ability even if Spellskite isn't a legal target for the target spell or ability—or even if that spell or ability has no targets. In this case, no targets are changed.")
        ruling("2020-08-07", "If Spellskite leaves the battlefield before its ability resolves or otherwise becomes an illegal target for the target spell or ability before its ability resolves, no targets are changed.")
        ruling("2020-08-07", "If changing one target of a spell or ability to Spellskite would make other targets of that spell or ability illegal, that target can't be changed to Spellskite.")
        ruling("2020-08-07", "If the spell or ability has multiple instances of the word \"target,\" you choose which one target you're changing to Spellskite as Spellskite's ability resolves.")
        ruling("2020-08-07", "If a spell or ability has multiple targets but doesn't use the word \"target\" multiple times, such as the ability of Deepglow Skate, you can only change one of the targets to Spellskite.")
        ruling("2020-08-07", "If a spell or ability has a variable number of targets, you can't change the number of targets.")
    }
}
