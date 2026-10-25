package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Trickster's Talisman (AFR #79) — {U} Artifact — Equipment (uncommon)
 *
 * Invoke Duplicity — Equipped creature gets +1/+1 and has "Whenever this creature deals combat
 * damage to a player, you may sacrifice Trickster's Talisman. If you do, create a token that's a
 * copy of this creature."
 * Equip {2}
 *
 * Implementation notes:
 * - The +1/+1 buff is a [ModifyStats] static scoped to [Filters.EquippedCreature].
 * - The quoted ability is granted to the equipped creature via [GrantTriggeredAbility] with a
 *   SELF-bound `dealsCombatDamage(Recipient.AnyPlayer)` trigger, so "this creature" is the
 *   creature and [Effects.CreateTokenCopyOfSelf] copies it (the granted ability's source).
 * - "You may sacrifice Trickster's Talisman. If you do, …" is [Effects.May] over
 *   [Effects.IfYouDo] scored by [SuccessCriterion.PermanentsSacrificed] — a one-shot "if you do"
 *   in the same resolution, not a reflexive "when you do". The payoff only happens if the
 *   Talisman actually went to the graveyard.
 * - "Trickster's Talisman" names the specific granting Equipment (CR 201.5a), so the sacrifice
 *   is scoped to [StatePredicate.IsGrantingPermanent] — exactly that object, not a second
 *   Talisman, and still the right object if the creature left the battlefield in response.
 *   `youControl()` carries the ruling that a player whose creature wears an opponent's Talisman
 *   can't sacrifice it (and so gets no token).
 */
val TrickstersTalisman = card("Trickster's Talisman") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Invoke Duplicity — Equipped creature gets +1/+1 and has \"Whenever this creature " +
        "deals combat damage to a player, you may sacrifice Trickster's Talisman. If you do, create " +
        "a token that's a copy of this creature.\"\n" +
        "Equip {2}"

    // Equipped creature gets +1/+1.
    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    // ... and has "Whenever this creature deals combat damage to a player, you may sacrifice
    // Trickster's Talisman. If you do, create a token that's a copy of this creature."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.May(
                    Effects.IfYouDo(
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
                                prompt = "Sacrifice Trickster's Talisman"
                            )
                            run(Effects.SacrificeTarget(toSacrifice.asTarget))
                        },
                        then = Effects.CreateTokenCopyOfSelf(),
                        successCriterion = SuccessCriterion.PermanentsSacrificed
                    ),
                    descriptionOverride = "You may sacrifice Trickster's Talisman. If you do, " +
                        "create a token that's a copy of this creature."
                )
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Sam White"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33cb9973-c9ce-48f1-9972-651092624751.jpg?1783926505"
    }
}
