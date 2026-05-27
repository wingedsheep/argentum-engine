package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Tolarian Emissary
 * {2}{U}
 * Creature — Human Wizard
 * 1/2
 * Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)
 * Flying
 * When this creature enters, if it was kicked, destroy target enchantment.
 */
val TolarianEmissary = card("Tolarian Emissary") {
    manaCost = "{2}{U}"
    colorIdentity = "UW"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)\n" +
        "Flying\n" +
        "When this creature enters, if it was kicked, destroy target enchantment."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.kicker("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        val t = target("enchantment", Targets.Enchantment)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cbc55e5-b84c-4449-a288-ec26cdd3997c.jpg?1562900709"
    }
}
