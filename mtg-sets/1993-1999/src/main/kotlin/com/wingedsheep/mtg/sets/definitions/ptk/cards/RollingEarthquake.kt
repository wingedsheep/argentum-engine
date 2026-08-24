package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rolling Earthquake
 * {X}{R}
 * Sorcery
 * Rolling Earthquake deals X damage to each creature without horsemanship and each player.
 *
 * The horsemanship-blind cousin of Earthquake: one sentence, two halves joined by
 * [Effects.Composite]. The board half is [Effects.ForEachInGroup] over every creature that lacks
 * [Keyword.HORSEMANSHIP], with the damage aimed at [EffectTarget.Self] — the current iteration
 * entity. The player half is [Effects.ForEachPlayer] over [Player.Each], each iteration rebinding
 * the controller so [EffectTarget.Controller] is the player being processed.
 */
val RollingEarthquake = card("Rolling Earthquake") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rolling Earthquake deals X damage to each creature without horsemanship and each player."

    spell {
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                Filters.Group.allCreatures.withoutKeyword(Keyword.HORSEMANSHIP),
                Effects.DealXDamage(EffectTarget.Self)
            ),
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealXDamage(EffectTarget.Controller))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Yang Hong"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c1bf210-ecdb-4b49-8504-51360c269e66.jpg"
    }
}
