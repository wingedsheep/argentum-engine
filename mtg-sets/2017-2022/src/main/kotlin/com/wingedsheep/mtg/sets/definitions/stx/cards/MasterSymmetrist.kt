package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master Symmetrist — Strixhaven: School of Mages #138 (canonical printing)
 * {2}{G}{G} · Creature — Rhino Druid · 4/4
 *
 * Reach
 * Whenever a creature you control with power equal to its toughness attacks, it gains trample
 * until end of turn.
 *
 * The Symmetry Matrix shape on an attack trigger: "with power equal to its toughness" is part of
 * the trigger event, so it is a `triggerRestriction` comparing the attacker's power and
 * toughness once, when it attacks (ruling) — later P/T changes neither stop nor undo the
 * trample. [Effects.GrantKeyword] on the triggering creature, until end of turn.
 */
val MasterSymmetrist = card("Master Symmetrist") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Druid"
    oracleText = "Reach\n" +
        "Whenever a creature you control with power equal to its toughness attacks, it gains " +
        "trample until end of turn."
    power = 4
    toughness = 4

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.triggeringPower(),
            ComparisonOperator.EQ,
            DynamicAmounts.triggeringToughness()
        )
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Forrest Imel"
        flavorText = "Tondo lost his very first duel and has spent his entire life trying to get even."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/255cdc39-6ad8-4831-9718-6712cb17654c.jpg?1783927343"
        ruling("2021-04-16", "Master Symmetrist's triggered ability only looks at the power and toughness of a creature when it attacks. Once the ability has triggered, changing the creature's power or toughness will not stop it from gaining trample or remove trample from it.")
    }
}
