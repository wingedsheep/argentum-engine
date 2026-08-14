package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Thorin, Mountain-king
 * {3}{R}
 * Legendary Creature — Dwarf Noble
 * 3/4
 *
 * Trample
 * When Thorin enters, attach any number of target Equipment you control to target creature you
 * control. When one or more Equipment become attached to that creature this way, that creature
 * deals damage equal to its power to up to one target creature.
 *
 * Modeling notes — composition over existing primitives:
 *  - **Target order is load-bearing.** The printed text names the Equipment first, but an unbounded
 *    (`unlimited`) requirement has to be declared *last*: targets are matched to requirements
 *    positionally, so anything after an unbounded slot loses its slice. Declaring the creature first
 *    also pins it to `ContextTarget(0)` no matter how many Equipment the unbounded slot swallowed
 *    (same trick as Graceful Takedown).
 *  - **The attach loop** gathers the still-legal [CardSource.ChosenTargets], splits them into the
 *    Equipment and the one creature (set difference, so no type test can mis-sort them), and runs
 *    [Effects.AttachTargetEquipmentToCreature] once per Equipment with `EffectTarget.Self` bound to
 *    the iterated Equipment (Beatrix, Loyal General's loop). Reading the targets through the gather
 *    is what gives CR 608.2b partial legality for free: an Equipment that became illegal simply
 *    isn't in the collection.
 *  - **"When one or more Equipment become attached … this way"** is a genuine CR 603.12 reflexive
 *    trigger — a second stack object whose "up to one target creature" is chosen when it goes on the
 *    stack, after the attaching has happened — so it is a [ReflexiveTriggerEffect] with
 *    `optional = false` (nothing here is a "may"). Its damage is dealt *by the equipped creature*,
 *    not by Thorin, so the payoff is a one-iteration [ForEachInCollectionEffect] over the creature
 *    collection: `EffectTarget.Self` makes the creature the damage source (lifelink, deathtouch and
 *    "dealt damage by a creature" reactions all see the creature) and `EntityReference.IterationEntity`
 *    reads *its* power, at reflexive-resolution time. The collection reaches the reflexive because
 *    the executor carries the action's pipeline onto the reflexive event.
 *  - **The "one or more" guard** is the [ConditionalEffect] wrapping the whole reflexive: both the
 *    Equipment collection and the creature collection must be non-empty. Without it, choosing zero
 *    Equipment (legal — "any number" allows none) or losing the creature target would still fire the
 *    damage half, which the printed trigger condition forbids.
 *
 * Edge cases: zero Equipment chosen → nothing attaches and no damage trigger; the creature target
 * illegal on resolution → the Equipment targets are still legal so the ability resolves, but nothing
 * attaches and no damage trigger; the reflexive's creature target is "up to one", so declining it
 * resolves the reflexive with no damage dealt. Re-attaching an already-equipped Equipment is
 * correct — the attach executor detaches it from its current host first.
 */
val ThorinMountainKing = card("Thorin, Mountain-king") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Noble"
    power = 3
    toughness = 4
    oracleText = "Trample\n" +
        "When Thorin enters, attach any number of target Equipment you control to target creature " +
        "you control. When one or more Equipment become attached to that creature this way, that " +
        "creature deals damage equal to its power to up to one target creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // Declared first so its ContextTarget index stays stable ahead of the unbounded slot.
        val equippedCreature = target("target creature you control", Targets.CreatureYouControl)
        target(
            "any number of target Equipment you control",
            TargetPermanent(
                unlimited = true,
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()
                )
            )
        )

        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val equipment = filter(
                chosen,
                GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
            )
            // Whatever is left of the chosen targets is the one "target creature you control".
            val creature = exclude(chosen, equipment)

            run(
                ConditionalEffect(
                    condition = Conditions.All(
                        CollectionContainsMatch(equipment.key),
                        CollectionContainsMatch(creature.key)
                    ),
                    effect = ReflexiveTriggerEffect(
                        optional = false,
                        action = ForEachInCollectionEffect(
                            collection = equipment.key,
                            effect = Effects.AttachTargetEquipmentToCreature(
                                equipmentTarget = EffectTarget.Self,
                                creatureTarget = equippedCreature
                            )
                        ),
                        reflexiveEffect = ForEachInCollectionEffect(
                            collection = creature.key,
                            effect = DealDamageEffect(
                                amount = DynamicAmount.EntityProperty(
                                    EntityReference.IterationEntity,
                                    EntityNumericProperty.Power
                                ),
                                target = EffectTarget.ContextTarget(0),
                                damageSource = EffectTarget.Self
                            )
                        ),
                        reflexiveTargetRequirements = listOf(
                            TargetCreature(optional = true)
                        ),
                        descriptionOverride = "Attach the chosen Equipment to that creature. When " +
                            "one or more Equipment become attached to it this way, it deals damage " +
                            "equal to its power to up to one target creature."
                    )
                )
            )
        }
        description = "When Thorin enters, attach any number of target Equipment you control to " +
            "target creature you control. When one or more Equipment become attached to that " +
            "creature this way, that creature deals damage equal to its power to up to one target " +
            "creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Javier Charro"
        flavorText = "\"To me! O my kinsfolk!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/1/117347af-0dd7-4350-901d-8c8a81387e22.jpg?1783902784"
    }
}
