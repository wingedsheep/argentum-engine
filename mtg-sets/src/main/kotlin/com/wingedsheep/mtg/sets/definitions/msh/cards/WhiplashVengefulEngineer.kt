package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Whiplash, Vengeful Engineer — Marvel Super Heroes #121 (uncommon)
 * {B} · Legendary Creature — Human Artificer Villain · 2/2
 *
 * Whiplash enters tapped.
 * Whenever Whiplash attacks, if he's equipped, each opponent loses X life and you gain X life,
 * where X is the number of Equipment attached to him.
 *
 * Composed from existing primitives:
 *  - "enters tapped" is the standard self-replacement [EntersTapped].
 *  - The attack trigger's "if he's equipped" is an intervening-"if" (CR 603.4), so it goes on
 *    `triggerCondition` — checked both as the trigger would go on the stack and again on
 *    resolution. [Conditions.SourceMatches] with `GameObjectFilter.Any.equipped()` (the
 *    `StatePredicate.IsEquipped` state predicate) is the "he's equipped" test.
 *  - X is [DynamicAmounts.equipmentAttachedToSelf] — the Equipment-only attachment count on the
 *    source (Shagrat, Loot Bearer's amass amount), so Auras and other attachments don't inflate it.
 *  - The drain is *not* [Effects.DrainLife]: that gains life equal to the *total* lost across all
 *    opponents. Whiplash gains X once no matter how many opponents lost X, so it's a
 *    lose-life-per-opponent plus a single gain-life, the shape used by Glidedive Duo.
 */
val WhiplashVengefulEngineer = card("Whiplash, Vengeful Engineer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Artificer Villain"
    power = 2
    toughness = 2
    oracleText = "Whiplash enters tapped.\n" +
        "Whenever Whiplash attacks, if he's equipped, each opponent loses X life and you gain X " +
        "life, where X is the number of Equipment attached to him."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        effect = Effects.Composite(
            Effects.LoseLife(
                DynamicAmounts.equipmentAttachedToSelf(),
                EffectTarget.PlayerRef(Player.EachOpponent)
            ),
            Effects.GainLife(DynamicAmounts.equipmentAttachedToSelf())
        )
        description = "Whenever Whiplash attacks, if he's equipped, each opponent loses X life " +
            "and you gain X life, where X is the number of Equipment attached to him."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Alexander Gering"
        flavorText = "\"Wrap yourself in whatever armor you wish. I will shred you apart.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f6e6767-4d20-469c-9f4b-7286b8cc1979.jpg?1783902935"
    }
}
