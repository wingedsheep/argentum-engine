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
 * Tangled Skyline — March of the Machine #209
 * {4}{G} · Enchantment
 *
 * When this enchantment enters, you gain 5 life and incubate 5.
 * Phyrexians you control have reach.
 */
val TangledSkyline = card("Tangled Skyline") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you gain 5 life and incubate 5. (Create an Incubator " +
        "token with five +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into " +
        "a 0/0 Phyrexian artifact creature.)\n" +
        "Phyrexians you control have reach."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(5) then Effects.Incubate(5)
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.REACH,
            GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Martin de Diego Sádaba"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e08e0ef1-a8e0-4c0a-a996-3eee89e37fee.jpg?1783916961"
    }
}
