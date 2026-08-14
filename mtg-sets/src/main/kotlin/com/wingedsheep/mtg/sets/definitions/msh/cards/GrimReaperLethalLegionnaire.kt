package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Reaper, Lethal Legionnaire — Marvel Super Heroes #98
 * {3}{B} · Legendary Creature — Human Villain · 3/4
 *
 * Whenever Grim Reaper attacks, you may pay {3}{B}. When you do, return target creature card from
 * your graveyard to the battlefield tapped and attacking with a finality counter on it. (If a
 * creature with a finality counter on it would die, exile it instead.)
 *
 * Implementation notes:
 * - The Thousand Moons Crackshot shape: the attack trigger is a [ReflexiveTriggerEffect] whose
 *   action is the optional [PayManaCostEffect]. The reanimation targets only *after* the payment
 *   is made — a reflexive triggered ability (CR 603.12) is created by the payment and picks its
 *   target as it goes on the stack, so declining the cost never locks a graveyard card as a target.
 * - "tapped and attacking with a finality counter on it" is one zone change:
 *   [ZonePlacement.TappedAndAttacking] plus `addCounterType`, so the counter rides along with the
 *   move (Coalstoke Gearhulk's idiom) instead of being added by a later effect — a creature that
 *   dies in the same window is still exiled rather than hitting the graveyard.
 * - No `controllerOverride` is needed: the card comes from *your* graveyard, so its owner and the
 *   ability's controller are the same player.
 */
val GrimReaperLethalLegionnaire = card("Grim Reaper, Lethal Legionnaire") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Villain"
    power = 3
    toughness = 4
    oracleText = "Whenever Grim Reaper attacks, you may pay {3}{B}. When you do, return target " +
        "creature card from your graveyard to the battlefield tapped and attacking with a " +
        "finality counter on it. (If a creature with a finality counter on it would die, exile " +
        "it instead.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ReflexiveTriggerEffect(
            // "you may pay {3}{B}"
            action = PayManaCostEffect(ManaCost.parse("{3}{B}")),
            optional = true,
            // "When you do, return target creature card from your graveyard to the battlefield
            // tapped and attacking with a finality counter on it."
            reflexiveEffect = Effects.Move(
                target = EffectTarget.ContextTarget(0),
                destination = Zone.BATTLEFIELD,
                placement = ZonePlacement.TappedAndAttacking,
                addCounterType = CounterType.FINALITY,
            ),
            reflexiveTargetRequirements = listOf(Targets.CreatureCardInYourGraveyard),
            descriptionOverride = "You may pay {3}{B}. When you do, return target creature card " +
                "from your graveyard to the battlefield tapped and attacking with a finality " +
                "counter on it.",
        )
        description = "Whenever Grim Reaper attacks, you may pay {3}{B}. When you do, return " +
            "target creature card from your graveyard to the battlefield tapped and attacking " +
            "with a finality counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Lie Setiawan"
        flavorText = "\"The corpses have been sown. Now, to harvest!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b1cedfe-4712-4602-a8a8-112bd54c9938.jpg?1783902943"
    }
}
