package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Yathan Roadwatcher — Tarkir: Dragonstorm #236
 * {1}{W}{B}{G} · Creature — Human Scout · 3/3
 *
 * When this creature enters, if you cast it, mill four cards. When you do, return target
 * creature card with mana value 3 or less from your graveyard to the battlefield.
 *
 * The "if you cast it" clause is an intervening-if ([Conditions.WasCast]) on the enters
 * trigger. The mill is non-optional, so the reflexive return ("When you do") always fires
 * — the player then targets a creature card with mana value 3 or less in their graveyard
 * to put onto the battlefield. If no such target exists the reflexive payoff is skipped.
 */
val YathanRoadwatcher = card("Yathan Roadwatcher") {
    manaCost = "{1}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, if you cast it, mill four cards. " +
        "When you do, return target creature card with mana value 3 or less from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        effect = ReflexiveTriggerEffect(
            action = LibraryPatterns.mill(4),
            optional = false,
            reflexiveEffect = Effects.PutOntoBattlefield(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetObject(filter = TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e77339b-dd82-481c-9ee2-4156ca69ad35.jpg?1743204933"
    }
}
