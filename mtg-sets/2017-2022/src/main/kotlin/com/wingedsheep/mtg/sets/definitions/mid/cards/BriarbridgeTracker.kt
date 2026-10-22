package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Briarbridge Tracker (Innistrad: Midnight Hunt #172)
 * {2}{G} · Creature — Human Scout 2/3
 *
 * Vigilance
 * When this creature enters, investigate.
 * As long as you control a token, this creature gets +2/+0.
 *
 * Any token counts — the Clue it makes itself switches the bonus on until it's sacrificed.
 */
val BriarbridgeTracker = card("Briarbridge Tracker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 3
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "When this creature enters, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")\n" +
        "As long as you control a token, this creature gets +2/+0."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Investigate()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Token),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e215a7bd-112f-4228-a99e-5a8cb4be5cee.jpg?1783925585"
    }
}
