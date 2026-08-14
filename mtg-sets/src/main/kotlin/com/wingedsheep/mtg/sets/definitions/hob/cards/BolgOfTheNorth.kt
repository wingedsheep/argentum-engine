package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Bolg of the North — The Hobbit #148
 * {3}{B}{R} · Legendary Creature — Goblin Soldier · Uncommon
 * 5/5
 *
 * When Bolg enters, you may sacrifice another creature. When you do, Bolg deals damage equal to
 * that creature's power to another target creature. If excess damage was dealt this way, amass
 * Goblins X, where X is that excess damage.
 *
 * Modeling notes:
 *  - Killmonger, Scourge of Wakanda's shape: the sacrifice is a resolution-time *choice*
 *    ([SelectTargetEffect] + [Effects.SacrificeTarget]), not a target, so declining commits to
 *    nothing. Only an actual sacrifice fires the "When you do" reflexive ability, whose own target
 *    is chosen as it goes on the stack (CR 603.11).
 *  - **The power snapshot is taken before the sacrifice, not after.** "That creature's power" is
 *    last-known information (CR 608.2h), and the reflexive ability resolves from a fresh
 *    `EffectContext` on the far side of a stack round-trip — the sacrificed creature's LKI
 *    snapshot does not survive that trip, only the pipeline does. So the action stores the power
 *    into the pipeline via [Effects.StoreNumber] while the creature is still on the battlefield
 *    (counters and pumps included), and the reflexive reads it back as a
 *    [DynamicAmount.VariableReference]. Reading `Power` off the card in the graveyard instead
 *    would silently drop every +1/+1 counter and Layer 7 bonus it had.
 *  - The excess-damage amass is the Orbital Plunge / Hell to Pay pair: [ConditionalEffect] gated on
 *    [Conditions.IfTargetTookExcessDamage] for the "if excess damage was dealt" clause, and
 *    [EntityNumericProperty.ExcessMarkedDamage] for the amount. Both read the target's post-damage
 *    marked damage in the same composite, which has no interleaved SBA pass — so the only marked
 *    damage in scope is what Bolg just dealt (CR 120.4a).
 *  - "Another target creature" excludes Bolg himself (`.other()`), and the sacrifice pool likewise
 *    excludes him ("another creature").
 */
val BolgOfTheNorth = card("Bolg of the North") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Goblin Soldier"
    power = 5
    toughness = 5
    oracleText = "When Bolg enters, you may sacrifice another creature. When you do, Bolg deals " +
        "damage equal to that creature's power to another target creature. If excess damage was " +
        "dealt this way, amass Goblins X, where X is that excess damage. (Put X +1/+1 counters on " +
        "an Army you control. It's also a Goblin. If you don't control an Army, create a 0/0 " +
        "black Goblin Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                        storeAs = "bolgSacrifice",
                    ),
                    Effects.StoreNumber(
                        "bolgSacrificedPower",
                        DynamicAmount.EntityProperty(
                            EntityReference.FromCostStorage("bolgSacrifice"),
                            EntityNumericProperty.Power,
                        ),
                    ),
                    Effects.SacrificeTarget(EffectTarget.PipelineTarget("bolgSacrifice")),
                ),
            ),
            optional = true,
            reflexiveEffect = Effects.Composite(
                Effects.DealDamage(
                    DynamicAmount.VariableReference("bolgSacrificedPower"),
                    EffectTarget.ContextTarget(0),
                ),
                ConditionalEffect(
                    condition = Conditions.IfTargetTookExcessDamage(),
                    effect = Effects.Amass(
                        DynamicAmount.EntityProperty(
                            EntityReference.Target(0),
                            EntityNumericProperty.ExcessMarkedDamage,
                        ),
                        "Goblin",
                    ),
                ),
            ),
            reflexiveTargetRequirements = listOf(
                TargetCreature(filter = TargetFilter.Creature.other()),
            ),
            descriptionOverride = "You may sacrifice another creature. When you do, Bolg deals " +
                "damage equal to that creature's power to another target creature. If excess " +
                "damage was dealt this way, amass Goblins X, where X is that excess damage.",
        )
        description = "When Bolg enters, you may sacrifice another creature. When you do, Bolg " +
            "deals damage equal to that creature's power to another target creature. If excess " +
            "damage was dealt this way, amass Goblins X, where X is that excess damage."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Miklós Ligeti"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b2d2a7f-88e0-45a9-8579-a6736bcd66eb.jpg?1784377021"
    }
}
