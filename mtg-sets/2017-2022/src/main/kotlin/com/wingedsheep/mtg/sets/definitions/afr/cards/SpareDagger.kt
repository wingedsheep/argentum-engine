package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spare Dagger (AFR #250) — {1} Artifact — Equipment (common)
 *
 * Equipped creature gets +1/+0 and has "Whenever this creature attacks, you may sacrifice
 * Spare Dagger. When you do, this creature deals 1 damage to any target."
 * Equip {1}
 *
 * Implementation notes:
 * - The +1/+0 buff is a [ModifyStats] static scoped to [Filters.EquippedCreature].
 * - The quoted attack ability is granted to the equipped creature via [GrantTriggeredAbility]
 *   + `Triggers.<subject>.attacks(requires)` (SELF binding, the Pirate Hat / Dire Blunderbuss idiom), so it lives
 *   on the creature and fires when that creature attacks. The body is a [ReflexiveTriggerEffect]:
 *   the optional action sacrifices Spare Dagger; only "when you do" puts the reflexive part on
 *   the stack to choose any target and deal 1 damage. The damage source defaults to the granted
 *   ability's source — the equipped creature — matching "this creature deals 1 damage."
 *
 * "sacrifice Spare Dagger" — a card naming itself means that object: the Equipment that granted
 * the ability. A granted *triggered* ability resolves with the equipped creature as its source, so
 * the sacrifice is scoped to [StatePredicate.IsGrantingPermanent] (the Trickster's Talisman idiom)
 * rather than by name — a second Spare Dagger, even one on the same creature, is never offered —
 * and to `youControl()`, since you can only sacrifice a permanent you control.
 */
val SpareDagger = card("Spare Dagger") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has \"Whenever this creature attacks, you may " +
        "sacrifice Spare Dagger. When you do, this creature deals 1 damage to any target.\"\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    // Equipped creature gets +1/+0.
    staticAbility {
        ability = ModifyStats(+1, +0, Filters.EquippedCreature)
    }

    // ... and has "Whenever this creature attacks, you may sacrifice Spare Dagger. When you
    // do, this creature deals 1 damage to any target."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(),
                effect = Effects.ReflexiveTrigger(
                    action = Effects.Pipeline {
                        val toSacrifice = selectTarget(
                            TargetObject(
                                filter = TargetFilter(
                                    GameObjectFilter.Artifact
                                        .withStatePredicate(StatePredicate.IsGrantingPermanent)
                                        .youControl()
                                )
                            ),
                            nonTargeting = true,
                            prompt = "Sacrifice Spare Dagger"
                        )
                        run(Effects.SacrificeTarget(toSacrifice.asTarget))
                    },
                    optional = true,
                    descriptionOverride = "You may sacrifice Spare Dagger. When you do, this creature deals 1 damage to any target."
                ) {
                    val anyTarget = target(Targets.Any)
                    effect = Effects.DealDamage(1, anyTarget)
                }
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "250"
        artist = "Khurrum"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab6b0291-00f8-45bf-9f3a-ee43739c2b77.jpg?1783926437"
    }
}
