package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hooded Hydra
 * {X}{G}{G}
 * Creature — Snake Hydra
 * 0/0
 * Hooded Hydra enters the battlefield with X +1/+1 counters on it.
 * When Hooded Hydra dies, create a 1/1 green Snake creature token for each
 * +1/+1 counter on it.
 * Morph {3}{G}{G}
 * As Hooded Hydra is turned face up, put five +1/+1 counters on it.
 *
 * Note: The last ability is a replacement effect, not a triggered ability.
 * It modifies how the creature is turned face up, placing counters as part
 * of the face-up action (before state-based actions check).
 */
val HoodedHydra = card("Hooded Hydra") {
    manaCost = "{X}{G}{G}"
    typeLine = "Creature — Snake Hydra"
    power = 0
    toughness = 0
    oracleText = "Hooded Hydra enters the battlefield with X +1/+1 counters on it.\nWhen Hooded Hydra dies, create a 1/1 green Snake creature token for each +1/+1 counter on it.\nMorph {3}{G}{G}\nAs Hooded Hydra is turned face up, put five +1/+1 counters on it."

    // Enters with X +1/+1 counters (when cast normally, not face-down)
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.XValue))

    // When this creature dies, create 1/1 green Snake tokens equal to its +1/+1 counters
    triggeredAbility {
        trigger = Triggers.Dies
        effect = CreateTokenEffect(
            count = DynamicAmount.LastKnownCounterCount,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Snake"),
            imageUri = "https://cards.scryfall.io/normal/front/0/3/032e9f9d-b1e5-4724-9b80-e51500d12d5b.jpg?1562639651"
        )
    }

    // Morph {3}{G}{G} — as turned face up, put 5 +1/+1 counters on it
    morph = "{3}{G}{G}"
    morphFaceUpEffect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 5, EffectTarget.Self)

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "136"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d7e651f-4187-44c5-99bf-34042d6dd9a2.jpg?1562791088"
    }
}
