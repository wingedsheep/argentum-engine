package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

val Goatnap = card("Goatnap") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn. If that creature is a Goat, it also gets +3/+0 until end of turn."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.GainControl(t, Duration.EndOfTurn),
            Effects.Untap(t),
            Effects.GrantKeyword(Keyword.HASTE, t),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype("Goat")),
                effect = Effects.ModifyStats(3, 0, t)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Vincent Christiaens"
        flavorText = "Jealous of his sister's cloudgoat, the giant went to find the next best thing."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d8dbec5-ae71-4f82-898a-b930ec677403.jpg?1767658184"
    }
}
