package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Crossroads Watcher
 *
 * {2}{G} Creature — Kithkin Ranger 3/3
 * Trample
 * Whenever another creature you control enters, this creature gets +1/+0 until end of turn.
 */
object CrossroadsWatcher {
    val definition = CardDefinition.creature(
        name = "Crossroads Watcher",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.RANGER),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.TRAMPLE),
        oracleText = "Trample\nWhenever another creature you control enters, this creature gets +1/+0 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "173",
            rarity = Rarity.COMMON,
            artist = "Aurore Folny",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bbccddee-2345-6789-bcde-bbccddee2345.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Crossroads Watcher") {
        keywords(Keyword.TRAMPLE)

        // TODO: Needs OnOtherCreatureEnters trigger and ModifyStatsEffect with EffectTarget.Self
        // Whenever another creature you control enters, this creature gets +1/+0 until end of turn
    }
}
