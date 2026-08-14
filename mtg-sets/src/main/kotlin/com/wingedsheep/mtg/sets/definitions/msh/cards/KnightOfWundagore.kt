package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Knight of Wundagore — Marvel Super Heroes #175
 * {1}{G} · Creature — Cat Knight Villain · Common
 * 2/1
 *
 * Trample
 * Whenever you put a +1/+1 counter on another creature, put a +1/+1 counter on this creature.
 * This ability triggers only once each turn.
 *
 * Earth Kingdom General's shape. The "you put" scope comes from `placedBy = Player.You` (the
 * *placer*, CR 122.6a), not from the recipient filter — the recipient is left as any creature,
 * because the oracle says "another creature", not "another creature you control", so counters you
 * put on an opponent's creature count too. `binding = OTHER` supplies the "another": counters
 * landing on Knight of Wundagore itself never fire it.
 *
 * `firstTimeEachTurn = false` because the printed cap is on the *ability*, not on the recipient
 * permanent — the trigger's own per-permanent first-placement gate would let a second creature
 * fire it again in the same turn. `oncePerTurn = true` is the engine's "This ability triggers
 * only once each turn" tracker (cleared at end of turn), which is the printed clause verbatim.
 */
val KnightOfWundagore = card("Knight of Wundagore") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Knight Villain"
    power = 2
    toughness = 1
    oracleText = "Trample\n" +
        "Whenever you put a +1/+1 counter on another creature, put a +1/+1 counter on this " +
        "creature. This ability triggers only once each turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Creature,
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            firstTimeEachTurn = false,
            binding = TriggerBinding.OTHER,
            placedBy = Player.You,
        )
        oncePerTurn = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you put a +1/+1 counter on another creature, put a +1/+1 counter " +
            "on this creature. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Rafater"
        flavorText = "\"Well struck, Lord Gator! Let our atomic steeds carry us to victory!\"\n" +
            "—Lord Tyger"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3003766-9383-44e0-8b51-35ccbed137ed.jpg?1783902917"
    }
}
