package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angelic Intervention — March of the Machine #5
 * {1}{W} · Instant
 *
 * Target creature or planeswalker you control gains protection from colorless or from the color of
 * your choice until end of turn. If it's a creature, put a +1/+1 counter on it.
 */
val AngelicIntervention = card("Angelic Intervention") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature or planeswalker you control gains protection from colorless or from the color of your choice until end of turn. If it's a creature, put a +1/+1 counter on it. (It can't be blocked, targeted, dealt damage, enchanted, or equipped by anything with that quality.)"

    spell {
        val t = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.youControl()))
        effect = Effects.GrantProtectionFromColorlessOrChosenColor(t) then
            Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature, t),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Awanqi (Angela Wang)"
        flavorText = "Elspeth shone like a sixth sun, inspiring hope in the Mirrans and terror in New Phyrexia."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09fb5876-5b47-4a05-be57-7ad3890c8953.jpg?1783917071"
    }
}
