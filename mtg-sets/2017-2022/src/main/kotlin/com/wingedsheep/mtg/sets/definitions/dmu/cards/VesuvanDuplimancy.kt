package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vesuvan Duplimancy (DMU #73)
 *
 * "That artifact or creature" is the spell's target as captured when the ability triggered, so a
 * later retarget doesn't change it; a target that left before resolution — a token included — is
 * copied from its copiable values as it last existed on the battlefield.
 */
val VesuvanDuplimancy = card("Vesuvan Duplimancy") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a spell that targets only a single artifact or creature you control, " +
        "create a token that's a copy of that artifact or creature, except it's not legendary."

    triggeredAbility {
        trigger = Triggers.you.casts(
            requires = setOf(
                SpellCastPredicate.TargetsMatching(
                    (GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl(),
                    only = true
                )
            )
        )
        effect = Effects.ForEachInCollection(
            collection = CollectionSlot.TriggerCaptured,
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.IterationEntity,
                removedSupertypes = setOf(Supertype.LEGENDARY)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "73"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a2119bc-a377-4156-89d4-56e7b5b494a8.jpg?1783921342"

        ruling("2022-09-09", "The triggered ability of Vesuvan Duplimancy triggers only if you cast a spell that targets a single artifact or a single creature and nothing else. The ability will resolve before the spell that caused it to trigger, and a token will be created even if the spell you cast was countered or permanent it was targeting is now an illegal target.")
        ruling("2022-09-09", "If the target of the spell leaves the battlefield before the triggered ability resolves, use its characteristics as it last existed on the battlefield to determine the characteristics of the token created.")
        ruling("2022-09-09", "If the target of the spell is changed after the triggered ability is put on the stack, the token that is created is a copy of the spell's original target, not the new target.")
        ruling("2022-09-09", "The token isn't legendary, and this exception is copiable. If something else copies the token later, that copy also won't be legendary.")
        ruling("2022-09-09", "If the copied permanent is a token, the token that's created copies the original characteristics of that token as stated by the effect that created that token.")
        ruling("2022-09-09", "If the copied permanent is copying something else (for example, if the copied creature is a Clone), then the token enters the battlefield as whatever that permanent copied.")
    }
}
