package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Retched Wretch
 * {2}{B}
 * Creature — Goblin
 * 4/2
 *
 * When this creature dies, if it had a -1/-1 counter on it, return it to the
 * battlefield under its owner's control and it loses all abilities.
 */
val RetchedWretch = card("Retched Wretch") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Goblin"
    power = 4
    toughness = 2
    oracleText = "When this creature dies, if it had a -1/-1 counter on it, return it to the " +
        "battlefield under its owner's control and it loses all abilities."

    triggeredAbility {
        trigger = Triggers.Dies
        triggerCondition = Conditions.TriggeringEntityHadMinusOneMinusOneCounter
        effect = MoveToZoneEffect(
            target = EffectTarget.Self,
            destination = Zone.BATTLEFIELD
        ) then RemoveAllAbilitiesEffect(
            target = EffectTarget.Self,
            duration = Duration.Permanent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Raph Lomotan"
        flavorText = "\"Told ya I taste terrible!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7a4d7f1-976a-4a28-97a9-ff089a241c9d.jpg?1767871902"
    }
}
