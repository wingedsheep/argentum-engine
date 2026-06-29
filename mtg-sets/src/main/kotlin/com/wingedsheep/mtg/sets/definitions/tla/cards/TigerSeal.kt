package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tiger-Seal
 * {U}
 * Creature — Cat Seal
 * 3/3
 *
 * Vigilance
 * At the beginning of your upkeep, tap this creature.
 * Whenever you draw your second card each turn, untap this creature.
 *
 * The "second card each turn" trigger is the existing [Triggers.NthCardDrawn] (n = 2), backed by
 * the engine's per-player `CardsDrawnThisTurnComponent`.
 */
val TigerSeal = card("Tiger-Seal") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Cat Seal"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "At the beginning of your upkeep, tap this creature.\n" +
        "Whenever you draw your second card each turn, untap this creature."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Tap(EffectTarget.Self)
        description = "At the beginning of your upkeep, tap this creature."
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever you draw your second card each turn, untap this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Jinho Bae"
        flavorText = "Every creature in the world felt the Avatar's return and paid respects in whatever way it could."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7aef2891-1269-4a1a-acea-0aa37ef544c4.jpg?1764120490"
    }
}
