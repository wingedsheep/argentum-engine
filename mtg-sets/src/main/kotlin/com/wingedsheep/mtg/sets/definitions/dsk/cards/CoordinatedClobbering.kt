package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
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
 * Coordinated Clobbering
 * {G}
 * Sorcery
 *
 * Tap one or two target untapped creatures you control. They each deal damage equal to their
 * power to target creature an opponent controls.
 *
 * At resolution we gather the chosen targets into two collections by controller. This avoids
 * relying on a positional target index when the one-or-two-creature group is flattened into the
 * cast action. We then tap the clobberers and have each deal damage equal to its own power — read
 * per-iteration via [EntityReference.IterationEntity] — to the opponent's creature.
 */
val CoordinatedClobbering = card("Coordinated Clobbering") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Tap one or two target untapped creatures you control. They each deal damage " +
        "equal to their power to target creature an opponent controls."

    spell {
        // Keep the fixed-count victim first so the flattened target list remains unambiguous when
        // the controller chooses only one creature from the following one-or-two target group.
        target("creature an opponent controls", Targets.CreatureOpponentControls)
        target(
            "untapped creatures you control",
            TargetCreature(
                count = 2,
                minCount = 1,
                filter = TargetFilter(GameObjectFilter.Creature.untapped().youControl()),
            ),
        )

        effect = Effects.Composite(
            // Gather every chosen target, then separate the clobberers from the victim.
            GatherCardsEffect(
                source = CardSource.ChosenTargets,
                storeAs = "allTargets",
            ),
            FilterCollectionEffect(
                from = "allTargets",
                filter = CollectionFilter.MatchesFilter(GameObjectFilter.Creature.youControl()),
                storeMatching = "clobberers",
            ),
            FilterCollectionEffect(
                from = "allTargets",
                filter = CollectionFilter.MatchesFilter(GameObjectFilter.Creature.opponentControls()),
                storeMatching = "victim",
            ),
            // Tap all chosen creatures first ("Tap one or two target untapped creatures you control").
            ForEachInCollectionEffect(
                collection = "clobberers",
                effect = Effects.Tap(EffectTarget.Self),
            ),
            // Then each deals damage equal to its power to the opponent's creature.
            ForEachInCollectionEffect(
                collection = "clobberers",
                effect = Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(
                        EntityReference.IterationEntity,
                        EntityNumericProperty.Power,
                    ),
                    target = EffectTarget.PipelineTarget("victim"),
                    damageSource = EffectTarget.Self,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Fajareka Setiawan"
        flavorText = "Zimone's theory was that the fractalization of atmospheric aether would " +
            "increase kinetic energy. Tyvar's theory was that if you hit cultists in the face " +
            "really hard, they would fall down. They were both right."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d498cd5d-5807-4297-bc8a-c0941f2f5ce2.jpg?1726286504"
    }
}
