package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Terrific Team-Up
 * {3}{G}
 * Instant
 *
 * This spell costs {2} less to cast if you control a permanent with mana value 4 or greater.
 * One or two target creatures you control each get +1/+0 until end of turn. They each deal
 * damage equal to their power to target creature an opponent controls.
 *
 * Two target slots: the single opponent's creature (declared first, so it stays addressable as
 * [EffectTarget.ContextTarget] index 0 across the per-creature loop) and one or two creatures you
 * control. At resolution we gather the chosen targets, filter to the creatures you control (excludes
 * the victim), pump each +1/+0 until end of turn, then have each deal damage equal to its own
 * (boosted) power — read per-iteration via [EntityReference.IterationEntity] — to the opponent's
 * creature.
 */
val TerrificTeamUp = card("Terrific Team-Up") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if you control a permanent with mana value 4 " +
        "or greater.\nOne or two target creatures you control each get +1/+0 until end of turn. " +
        "They each deal damage equal to their power to target creature an opponent controls."

    staticAbility {
        // Only the generic {2} is reduced; the {G} pip is untouched. The whole modification is
        // gated on a battlefield existence condition rather than baked into the amount — the shape
        // the SDK reference prescribes for conditional cost reduction (cf. Truck Toss).
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.YouControl(GameObjectFilter.Permanent.manaValueAtLeast(4)),
            ),
        )
    }

    spell {
        // Declared first so the victim is a stable target across the per-creature loop.
        target("creature an opponent controls", Targets.CreatureOpponentControls)
        target(
            "one or two creatures you control",
            TargetCreature(
                count = 2,
                minCount = 1,
                filter = TargetFilter(GameObjectFilter.Creature.youControl()),
            ),
        )

        effect = Effects.Composite(
            // Gather every chosen target, then keep only the creatures you control (the victim is
            // an opponent's creature, so it drops out).
            GatherCardsEffect(
                source = CardSource.ChosenTargets,
                storeAs = "allTargets",
            ),
            FilterCollectionEffect(
                from = "allTargets",
                filter = CollectionFilter.MatchesFilter(GameObjectFilter.Creature.youControl()),
                storeMatching = "team",
            ),
            // Each chosen creature gets +1/+0 until end of turn.
            ForEachInCollectionEffect(
                collection = "team",
                effect = Effects.ModifyStats(1, 0, EffectTarget.Self),
            ),
            // Then each deals damage equal to its (boosted) power to the opponent's creature.
            ForEachInCollectionEffect(
                collection = "team",
                effect = Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(
                        EntityReference.IterationEntity,
                        EntityNumericProperty.Power,
                    ),
                    target = EffectTarget.ContextTarget(0),
                    damageSource = EffectTarget.Self,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "InHyuk Lee"
        flavorText = "\"Hey, Miles! Are you busy at the moment?\"\n—Spider-Man, Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3c587b0-66b9-46bf-90ee-a6163c006c9e.jpg?1783905321"
    }
}
