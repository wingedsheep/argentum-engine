package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Portal of Sanctuary (M20 #71)
 * {2}{U}  Artifact
 *
 * {1}, {T}: Return target creature you control and each Aura attached to it to their owners'
 * hands. Activate only during your turn.
 *
 * Mark of Eviction's gather-first shape with a targeted host: the Auras are collected while
 * still attached, then the creature and that collection move to their owners' hands. No Aura is
 * lost in between — the state-based action that would bin an unattached Aura (CR 704.5m) isn't
 * checked mid-resolution.
 */
val PortalOfSanctuary = card("Portal of Sanctuary") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Return target creature you control and each Aura attached to it to their " +
        "owners' hands. Activate only during your turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Pipeline {
            val auras = gather(
                CardSource.AttachedTo(
                    host = creature,
                    filter = GameObjectFilter.Permanent.withSubtype(Subtype.AURA)
                )
            )
            run(Effects.ReturnToHand(creature))
            toHand(auras)
        }
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Randy Vargas"
        flavorText = "\"If you need to disappear for a while, I know a place.\"\n—Bilivus, master archivist"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37c66692-2880-4ea2-abaf-8926c6950826.jpg?1783933005"
    }
}
