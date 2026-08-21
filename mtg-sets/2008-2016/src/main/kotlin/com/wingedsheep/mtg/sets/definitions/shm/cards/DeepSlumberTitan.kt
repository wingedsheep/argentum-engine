package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deep-Slumber Titan
 * {2}{R}{R}
 * Creature — Giant Warrior
 * 7 / 7
 *
 * This creature enters tapped.
 * This creature doesn't untap during your untap step.
 * Whenever this creature is dealt damage, untap it.
 *
 * - "Enters tapped" is a card-intrinsic self-replacement ([EntersTapped]), not a static or a
 *   trigger — it is applied as the Titan enters and never sees the untapped state.
 * - The untap restriction is the narrow [AbilityFlag.DOESNT_UNTAP] ("doesn't untap during your
 *   untap step"), *not* [AbilityFlag.CANT_BECOME_UNTAPPED]: an untap *effect* — including the
 *   Titan's own damage trigger below — still works.
 * - [Triggers.TakesDamage] is the SELF-bound incoming-damage event. It fires on damage from any
 *   source, combat or otherwise, and on any amount; the Titan surviving is not a condition, so a
 *   lethal hit still queues the (now pointless) untap.
 */
val DeepSlumberTitan = card("Deep-Slumber Titan") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Giant Warrior"
    power = 7
    toughness = 7
    oracleText = "This creature enters tapped.\n" +
        "This creature doesn't untap during your untap step.\n" +
        "Whenever this creature is dealt damage, untap it."

    // "This creature enters tapped."
    replacementEffect(EntersTapped())

    // "This creature doesn't untap during your untap step."
    flags(AbilityFlag.DOESNT_UNTAP)

    // "Whenever this creature is dealt damage, untap it."
    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever this creature is dealt damage, untap it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Steve Prescott"
        flavorText = "Do not disturb."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbe3a68e-c29e-48a8-a2d7-49c8bff3dd8a.jpg?1783942749"
    }
}
