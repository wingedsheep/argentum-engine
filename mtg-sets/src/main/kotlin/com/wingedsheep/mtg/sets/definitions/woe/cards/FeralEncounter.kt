package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Feral Encounter
 * {G}{G}
 * Sorcery
 * Look at the top five cards of your library. You may exile a creature card from among them. Put
 * the rest on the bottom of your library in a random order. You may cast the exiled card this turn.
 * At the beginning of the next combat phase this turn, target creature you control deals damage
 * equal to its power to up to one target creature you don't control.
 *
 * Two independent halves in one `spell { }`:
 *
 *  1. The dig is the Pictures of Spider-Man pipeline — gather the top five, an optional
 *     [SelectionMode.ChooseUpTo]`(1)` filtered to creature cards, the pick to exile with a
 *     [GrantMayPlayFromExileEffect] play permission, the remainder to the bottom in a random order.
 *     `showAllCards` keeps all five visible so the player sees what they're passing up, and
 *     declining simply leaves the collection empty — the exile, the grant and the fight all no-op
 *     independently.
 *  2. The combat payoff is a **delayed** trigger, and per the card's ruling the spell itself takes
 *     no targets: both targets are chosen when the delayed ability goes on the stack at the
 *     beginning of combat. That's [CreateDelayedTriggerEffect.targetRequirement] plus
 *     [CreateDelayedTriggerEffect.additionalTargetRequirements], exposed to the effect as
 *     `ContextTarget(0)` / `ContextTarget(1)`.
 *
 * [DelayedTriggerTiming.THIS_TURN_ONLY] carries the "this turn" clause: cast in a postcombat main
 * phase, no combat phase follows and the ability never triggers (the second ruling) rather than
 * waiting around for the opponent's combat.
 *
 * "Up to one target creature you don't control" is an `optional` requirement, so the trigger still
 * goes on the stack with no second target and the damage simply doesn't happen — matching the
 * house reading of "you don't control" as [TargetFilter.Creature.opponentControls] (cf. Clear Shot,
 * whose oracle line is identical).
 */
val FeralEncounter = card("Feral Encounter") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may exile a creature card from " +
        "among them. Put the rest on the bottom of your library in a random order. You may cast " +
        "the exiled card this turn.\n" +
        "At the beginning of the next combat phase this turn, target creature you control deals " +
        "damage equal to its power to up to one target creature you don't control."

    spell {
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                storeAs = "feralLooked"
            ),
            SelectFromCollectionEffect(
                from = "feralLooked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Creature,
                storeSelected = "feralExiled",
                storeRemainder = "feralRest",
                prompt = "You may exile a creature card. You may cast it this turn.",
                selectedLabel = "Exile (you may cast it this turn)",
                remainderLabel = "Put on the bottom of your library",
                showAllCards = true
            ),
            MoveCollectionEffect(
                from = "feralExiled",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            MoveCollectionEffect(
                from = "feralRest",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            ),
            GrantMayPlayFromExileEffect("feralExiled"),
            CreateDelayedTriggerEffect(
                step = Step.BEGIN_COMBAT,
                timing = DelayedTriggerTiming.THIS_TURN_ONLY,
                targetRequirement = TargetCreature(filter = TargetFilter.Creature.youControl()),
                additionalTargetRequirements = listOf(
                    TargetCreature(optional = true, filter = TargetFilter.Creature.opponentControls())
                ),
                effect = DealDamageEffect(
                    DynamicAmounts.targetPower(0),
                    EffectTarget.ContextTarget(1),
                    damageSource = EffectTarget.ContextTarget(0)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de21251f-40bf-4f7e-9e85-9033207a788f.jpg?1783915082"

        ruling(
            "2023-09-01",
            "Casting Feral Encounter doesn't require any targets. As you put the delayed triggered " +
                "ability on the stack at the beginning of the next combat phase this turn, you " +
                "choose the target creature you control and up to one target creature you don't control."
        )
        ruling(
            "2023-09-01",
            "If you cast Feral Encounter and no combat phases occur later in that turn, the " +
                "delayed triggered ability won't trigger."
        )
    }
}
