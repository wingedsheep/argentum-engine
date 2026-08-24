package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * High Tide
 * {U}
 * Instant
 * Until end of turn, whenever a player taps an Island for mana, that player adds an additional {U}.
 *
 * Symmetrical and board-wide: *any* player's Island, including Islands that arrive later in the
 * turn, and the extra {U} goes to whoever tapped it. That "including later" is what makes the
 * effect an `AdditionalManaOnSourceTap` static over a filter rather than a per-Island grant — the
 * filter is re-read on every tap.
 *
 * A spell has no permanent to carry a static, so it grants one to its own controller for the turn;
 * `ManaStaticsIndex` reads granted statics alongside printed ones. The grant holder only supplies
 * the filter's "you", and this filter has no controller predicate, so the holder is immaterial to
 * what the effect does.
 */
val HighTide = card("High Tide") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, whenever a player taps an Island for mana, that player adds an additional {U}."

    spell {
        effect = GrantStaticAbilityEffect(
            ability = AdditionalManaOnSourceTap(
                sourceFilter = GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
                color = Color.BLUE,
            ),
            target = EffectTarget.Controller,
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18a"
        artist = "Drew Tucker"
        flavorText = "\"When the very tides turn against you, it's time to consider retirement.\"\n—General Khurzog"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4686bbb9-517f-4cce-aa7a-5db41e22c02b.jpg?1783947913"
    }
}
