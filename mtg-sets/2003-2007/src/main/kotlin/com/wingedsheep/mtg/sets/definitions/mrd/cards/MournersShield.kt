package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Mourner's Shield — Mirrodin #209
 * {4} · Artifact · Uncommon
 *
 * Imprint — When this artifact enters, you may exile target card from a graveyard.
 * {2}, {T}: Prevent all damage that would be dealt this turn by a source of your choice that
 * shares a color with the exiled card.
 *
 * Modelling notes:
 * - Linked pair (CR 607), same as its Mirrodin cousins: `Effects.ExileLinkedToSource` writes the
 *   pile and [EntityReference.LinkedExiledCard] reads it back, so "the exiled card" can only ever
 *   mean the one *this* Shield imprinted.
 * - The colour clause is an *eligibility filter on the choice*, not a restriction applied after the
 *   fact: only sources sharing a colour with the exiled card are offered. With no imprint (or a
 *   colourless one) nothing qualifies and the ability resolves without a choice — a colourless
 *   source shares a colour with nothing, exactly as printed.
 * - The prevention has **no recipient clause** — it stops that source's damage to anything, unlike
 *   Samite Ministration's "dealt to you … by a source of your choice". That is what
 *   `PreventAllDamageFromChosenSourceMatching` expresses, and it installs the same silence shield a
 *   targeted "prevent all damage target creature would deal" does, so combat and noncombat damage
 *   are both covered.
 * - Activating with the pile empty is legal but does nothing; that is a real play pattern, since the
 *   imprint is a "may" that can be declined.
 */
val MournersShield = card("Mourner's Shield") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Imprint — When this artifact enters, you may exile target card from a graveyard.\n" +
        "{2}, {T}: Prevent all damage that would be dealt this turn by a source of your choice " +
        "that shares a color with the exiled card."

    // "Imprint — When this artifact enters, you may exile target card from a graveyard."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val exiled = target("target card from a graveyard", Targets.CardInGraveyard)
        effect = Effects.ExileLinkedToSource(exiled)
        description = "Imprint — When this artifact enters, you may exile target card from a graveyard."
    }

    // "{2}, {T}: Prevent all damage that would be dealt this turn by a source of your choice that
    // shares a color with the exiled card."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.PreventAllDamageFromChosenSourceMatching(
            GameObjectFilter.Any.sharingColorWith(EntityReference.LinkedExiledCard())
        )
        description = "{2}, {T}: Prevent all damage that would be dealt this turn by a source of " +
            "your choice that shares a color with the exiled card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/783fe67f-59f7-46f0-bfa4-fa4a65c9c33c.jpg?1783944512"
    }
}
