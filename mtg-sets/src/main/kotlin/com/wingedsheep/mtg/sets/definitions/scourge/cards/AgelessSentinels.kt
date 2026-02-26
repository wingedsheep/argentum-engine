package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

// Oracle errata: Originally printed as "Creature — Wall", gained Defender keyword
// with the Champions of Kamigawa rules update (all Walls gained Defender).
/**
 * Ageless Sentinels
 * {3}{W}
 * Creature — Wall
 * 4/4
 * Defender
 * Flying
 * When Ageless Sentinels blocks, it becomes a Bird Giant, and it loses defender.
 * (It's no longer a Wall. This effect lasts indefinitely.)
 */
val AgelessSentinels = card("Ageless Sentinels") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Wall"
    power = 4
    toughness = 4
    oracleText = "Defender (This creature can't attack.)\nFlying\nWhen this creature blocks, it becomes a Bird Giant, and it loses defender. (It's no longer a Wall. This effect lasts indefinitely.)"

    keywords(Keyword.DEFENDER, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.SetCreatureSubtypes(setOf("Bird", "Giant"), EffectTarget.Self, Duration.Permanent) then
                Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccaa4a19-8eba-4c43-8a9a-636e234df751.jpg?1562534734"
        ruling("2004-10-04", "It becomes both creature type Giant and creature type Bird.")
        ruling("2005-08-01", "Once it stops being a Wall, it can attack because it also loses Defender.")
    }
}
