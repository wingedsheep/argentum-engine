package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kraul Harpooner
 * {1}{G}
 * Creature — Insect Warrior
 * 3/2
 * Reach
 * Undergrowth — When this creature enters, choose up to one target creature you don't control with
 * flying. This creature gets +X/+0 until end of turn, where X is the number of creature cards in your
 * graveyard, then you may have this creature fight that creature.
 *
 * The pump always happens (even with no target chosen); X is locked in at resolution. The fight
 * offer is only made when a target was actually chosen — with no target there is nothing to fight.
 * A chosen target that has become illegal fizzles the whole ability (no pump), per the rulings.
 */
val KraulHarpooner = card("Kraul Harpooner") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Warrior"
    oracleText = "Reach\nUndergrowth — When this creature enters, choose up to one target creature you don't control with flying. " +
        "This creature gets +X/+0 until end of turn, where X is the number of creature cards in your graveyard, " +
        "then you may have this creature fight that creature."
    power = 3
    toughness = 2

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val flier = target(TargetFilter.CreatureOpponentControls.withKeyword(Keyword.FLYING), optional = true)
        effect = Effects.ModifyStats(
            DynamicAmounts.creatureCardsInYourGraveyard(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        ) then Effects.If(
            Conditions.TargetMatchesFilter(GameObjectFilter.Creature, flier),
            Effects.May(Effects.Fight(EffectTarget.Self, flier))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6d3c086-9ce4-41c3-8402-2818f1b27192.jpg?1783934150"
        ruling("2018-10-05", "The value of X is determined only as the undergrowth ability resolves. If the number of creature cards in your graveyard changes later in the turn, Kraul Harpooner is unaffected.")
        ruling("2018-10-05", "You choose the target of the triggered ability (or that it has no target) as it goes on the stack, but you choose whether the creatures fight as that ability resolves.")
        ruling("2018-10-05", "If you choose a target and the target creature is an illegal target when Kraul Harpooner's ability tries to resolve, the ability doesn't resolve and Kraul Harpooner doesn't get +X/+0. If the target creature is legal but Kraul Harpooner is no longer on the battlefield, the target creature won't deal or be dealt damage.")
        ruling("2018-10-05", "If you don't choose a target creature, Kraul Harpooner simply gets +X/+0 until end of turn.")
    }
}
