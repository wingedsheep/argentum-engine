package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Chartooth Cougar
 * {5}{R}
 * Creature — Cat Beast
 * 4/4
 * {R}: Chartooth Cougar gets +1/+0 until end of turn.
 * Mountaincycling {2} ({2}, Discard this card: Search your library for a Mountain card,
 * reveal it, put it into your hand, then shuffle.)
 */
val ChartoothCougar = card("Chartooth Cougar") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Cat Beast"
    power = 4
    toughness = 4
    oracleText = "{R}: Chartooth Cougar gets +1/+0 until end of turn.\nMountaincycling {2}"

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = ModifyStatsEffect(1, 0, EffectTarget.Self)
    }

    keywordAbility(KeywordAbility.Typecycling("Mountain", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Tony Szczudlo"
        flavorText = "The cats of Otaria are nothing if not adaptable."
        imageUri = "https://cards.scryfall.io/large/front/6/b/6b2c9c07-c3db-46ca-a204-b710c3a34ae9.jpg?1562530181"
    }
}
