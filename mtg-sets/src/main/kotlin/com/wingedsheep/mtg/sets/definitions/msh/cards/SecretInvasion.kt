package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Secret Invasion — Marvel Super Heroes #72 (rare)
 * {1}{U}{U} · Enchantment — Aura
 *
 * Enchant creature you control
 * When this Aura enters, exile up to one target creature other than enchanted creature until this
 * Aura leaves the battlefield. Enchanted creature becomes a copy of that creature until this Aura
 * leaves the battlefield.
 * Enchanted creature has ward {2}.
 *
 * The Assimilation Aegis machinery, re-pointed from an Equipment to an Aura:
 * - [Effects.ExileUntilLeaves] exiles the chosen creature into the Aura's linked-exile pile, and
 *   the [Triggers.LeavesBattlefield] ability's [Effects.ReturnLinkedExileUnderOwnersControl]
 *   returns it — the printed "until this Aura leaves the battlefield" duration (CR 400.7 / the
 *   Oblivion Ring template).
 * - [Effects.BecomeCopyOfLinkedExile] on [EffectTarget.EnchantedCreature] bakes the exiled
 *   creature card's copiable characteristics (CR 707.2) into the enchanted creature and tags it
 *   with the Aura's id, so a state-based check reverts the copy the moment the Aura is no longer
 *   attached to it — which for an Aura is exactly "until this Aura leaves the battlefield" (an
 *   Aura that comes unattached is put into its owner's graveyard by CR 704.5m anyway).
 * - "Other than enchanted creature" is the source-relative
 *   [GameObjectFilter.notAttachedToBySource] exclusion, evaluated against the Aura's own
 *   attachment. The target is "up to one", so declining is legal — then the linked exile holds no
 *   creature card and the copy half is a no-op, leaving the enchanted creature as itself.
 * - Ward {2} on the enchanted creature is [GrantWard], whose default scope is already
 *   `GroupFilter.attachedCreature()`.
 */
val SecretInvasion = card("Secret Invasion") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, exile up to one target creature other than enchanted creature " +
        "until this Aura leaves the battlefield. Enchanted creature becomes a copy of that " +
        "creature until this Aura leaves the battlefield.\n" +
        "Enchanted creature has ward {2}. (Whenever this creature becomes the target of a spell " +
        "or ability an opponent controls, counter it unless that player pays {2}.)"

    auraTarget = Targets.CreatureYouControl

    // ETB: exile up to one target creature other than enchanted creature, and turn the enchanted
    // creature into a copy of it — both for as long as this Aura is on the battlefield.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one target creature other than enchanted creature",
            TargetCreature(
                count = 1,
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature.notAttachedToBySource())
            )
        )
        effect = Effects.Composite(
            Effects.ExileUntilLeaves(creature),
            Effects.BecomeCopyOfLinkedExile(EffectTarget.EnchantedCreature)
        )
    }

    // LTB: return the exiled card to its owner's control.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    // "Enchanted creature has ward {2}."
    staticAbility {
        ability = GrantWard(WardCost.Mana("{2}"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e361b2f4-cd2f-44e9-a56d-c1d6b5ae742d.jpg?1783902953"
    }
}
