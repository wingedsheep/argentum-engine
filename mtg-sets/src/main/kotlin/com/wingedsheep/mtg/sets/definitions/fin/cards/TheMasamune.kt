package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalDeathTriggers
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Masamune
 * {3}
 * Legendary Artifact - Equipment
 * As long as equipped creature is attacking, it has first strike and must be blocked if able.
 * Equipped creature has "If a creature dying causes a triggered ability of this creature or an
 *   emblem you own to trigger, that ability triggers an additional time."
 * Equip {2}
 *
 * Modelled with three continuous statics on the Equipment (no new one-shot machinery):
 *  1. First strike while attacking - GrantKeyword of FIRST_STRIKE to the equipped creature
 *     (Filters.EquippedCreature), gated by a ConditionalStaticAbility on
 *     EntityMatches(EquippedCreature, attacking) so it applies only while the equipped creature is
 *     an attacker (and not while it blocks). The gate is required because an AttachedTo-scoped grant
 *     filter drops its base-filter predicates during projection, so ".attacking()" on the filter
 *     alone would not gate.
 *  2. Must be blocked if able while attacking - the filtered MustBeBlocked static
 *     (filter = attachedCreature(), allCreatures = false) projects the "must be blocked if able"
 *     requirement onto the equipped creature. Must-be-blocked only affects a creature while it
 *     attacks, so no separate attacking gate is needed.
 *  3. Death-trigger doubling - AdditionalDeathTriggers scoped to the equipped creature
 *     (attachedCreature = true) and emblems you own (includeEmblems = true). Per the printed rulings
 *     this doubles only the equipped creature's / an owned emblem's death and leave-the-battlefield
 *     triggers that fire because a creature died - not abilities that respond to the event that
 *     caused the death (e.g. "whenever you sacrifice a creature", which fires once). N copies are
 *     additive (N+1 firings). No emblem in the engine currently carries a triggered ability (emblems
 *     are static floating effects), so that leg is presently inert but modelled. Known limitation,
 *     shared with the engine's other death/leave doublers: the equipped creature's own "when this
 *     creature dies" trigger fired by itself dying is not doubled - trigger detection runs on
 *     post-death state and the doubler's attachment is gone by then.
 */
val TheMasamune = card("The Masamune") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "As long as equipped creature is attacking, it has first strike and must be " +
        "blocked if able.\n" +
        "Equipped creature has \"If a creature dying causes a triggered ability of this creature " +
        "or an emblem you own to trigger, that ability triggers an additional time.\"\n" +
        "Equip {2}"

    // 1. First strike while attacking (only while the equipped creature is an attacker).
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature),
            condition = Conditions.EntityMatches(
                EffectTarget.EquippedCreature,
                GameObjectFilter.Any.attacking()
            )
        )
    }

    // 1b. Must be blocked if able (relevant only while the equipped creature attacks).
    staticAbility {
        ability = MustBeBlocked(allCreatures = false, filter = GroupFilter.attachedCreature())
    }

    // 2. Equipped creature's death/leave triggers (and owned emblems') trigger an additional time.
    staticAbility {
        ability = AdditionalDeathTriggers(attachedCreature = true, includeEmblems = true)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Masateru Ikeda"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc408575-8ef7-4043-b6b7-b38cef7c97d1.jpg?1782686391"

        ruling(
            "2025-06-06",
            "An ability that triggers on an event that causes a creature to die doesn't trigger " +
                "twice. For example, an ability that triggers \"whenever you sacrifice a creature\" " +
                "triggers only once."
        )
        ruling(
            "2025-06-06",
            "The granted ability's effect doesn't copy the triggered ability; it just causes the " +
                "ability to trigger twice. Choices such as modes and targets are made separately " +
                "for each instance of the ability."
        )
        ruling(
            "2025-06-06",
            "An ability of the equipped creature or an emblem you own that triggers when a creature " +
                "\"leaves the battlefield\" will trigger twice if that creature leaves by dying."
        )
        ruling(
            "2025-06-06",
            "If you control two of The Masamune and they're equipped to creatures you control, a " +
                "creature dying causes abilities of emblems you own to trigger three times, not four."
        )
    }
}
