package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arachnogenesis — Commander 2015 #32 (canonical / earliest printing)
 * {2}{G} · Instant
 *
 * Create X 1/2 green Spider creature tokens with reach, where X is the number of creatures
 * attacking you. Prevent all combat damage that would be dealt this turn by non-Spider creatures.
 *
 * Galadhrim Ambush's shape with the count narrowed to creatures whose declared defender is you
 * yourself — `attackingPlayer(EffectTarget.Controller)` across all players (Sentry Bot's tally), so
 * creatures attacking another player or one of your planeswalkers don't count. X is read on
 * resolution, before the tokens exist; the prevention is the [Effects.PreventCombatDamageFrom]
 * shield over non-Spider creatures, judged as each combat damage event happens.
 */
val Arachnogenesis = card("Arachnogenesis") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Create X 1/2 green Spider creature tokens with reach, where X is the number of creatures " +
        "attacking you. Prevent all combat damage that would be dealt this turn by non-Spider creatures."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(
                Player.Each,
                GameObjectFilter.Creature.attackingPlayer(EffectTarget.Controller),
            ).count(),
            power = 1,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH),
            imageUri = "https://cards.scryfall.io/normal/front/e/8/e8efa0fc-e710-44f8-8128-485c2d43cc9e.jpg?1783938018",
        ) then Effects.PreventCombatDamageFrom(
            source = GameObjectFilter.Creature.notSubtype(Subtype.SPIDER),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Johannes Voss"
        flavorText = "Every movement is a summons—even your screams."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7967cb69-1868-45c0-92d1-39dabe30f2a2.jpg?1783938110"
    }
}
