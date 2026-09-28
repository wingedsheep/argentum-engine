package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Phyrexian Awakening
 * {2}{W}
 * Enchantment
 *
 * When this enchantment enters, incubate 4. (Create an Incubator token with four +1/+1 counters on it
 * and "{2}: Transform this token." It transforms into a 0/0 Phyrexian artifact creature.)
 * Phyrexians you control have vigilance.
 */
val PhyrexianAwakening = card("Phyrexian Awakening") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, incubate 4. (Create an Incubator token with four +1/+1 counters " +
        "on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Phyrexians you control have vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(4)
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
            GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b9ed068-b6bb-4c9e-a8c9-56aa9b62037a.jpg?1783917056"
    }
}
