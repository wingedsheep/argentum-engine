package com.wingedsheep.mtg.sets.definitions.mh3.cards

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
 * Essence Reliquary — Modern Horizons 3 #24 (only printing)
 * {2}{W} · Artifact
 *
 * {T}: Return another target permanent you control and all Auras you control attached to it to
 * their owner's hand. Activate only during your turn.
 *
 * Gather-first, like Mark of Eviction: the Auras you control are collected while they are still
 * attached, then the target moves, then the collected Auras follow. State-based actions aren't
 * checked mid-resolution, so nothing falls off in between. Auras attached to it that you *don't*
 * control stay behind (and go to the graveyard as unattached Auras via SBA).
 */
val EssenceReliquary = card("Essence Reliquary") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "{T}: Return another target permanent you control and all Auras you control " +
        "attached to it to their owner's hand. Activate only during your turn."

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.PermanentYouControl.other())
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        effect = Effects.Pipeline {
            val auras = gather(
                CardSource.AttachedTo(
                    host = permanent,
                    filter = GameObjectFilter.Permanent.withSubtype(Subtype.AURA).youControl()
                )
            )
            run(Effects.ReturnToHand(permanent))
            toHand(auras)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Kevin Sidharta"
        flavorText = "\"It safeguards the greatest treasure of all: our memories of those most " +
            "precious to us.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e65f4ae3-6b21-4827-9851-8a53628e254f.jpg?1783911303"
    }
}
