package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mai, Jaded Edge
 * {1}{R}
 * Legendary Creature — Human Noble
 * 1/3
 *
 * Prowess
 * Exhaust — {3}: Put a double strike counter on Mai. (Activate each exhaust ability only once.)
 *
 * The exhaust ability (isExhaust = true) puts a double strike keyword counter on Mai — projected to
 * the Double Strike keyword via StateProjector.KEYWORD_COUNTER_MAP.
 */
val MaiJadedEdge = card("Mai, Jaded Edge") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Noble"
    power = 1
    toughness = 3
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Exhaust — {3}: Put a double strike counter on Mai. (Activate each exhaust ability only once.)"

    prowess()

    activatedAbility {
        isExhaust = true
        cost = Costs.Mana("{3}")
        effect = Effects.AddCounters(Counters.DOUBLE_STRIKE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Toraji"
        flavorText = "\"Finally, something to do.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/732e6bc9-0798-4c00-aea0-5ef4298b45f5.jpg?1764121013"
    }
}
