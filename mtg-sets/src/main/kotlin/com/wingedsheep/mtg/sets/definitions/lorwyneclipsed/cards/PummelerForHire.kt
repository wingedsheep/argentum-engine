package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pummeler for Hire
 * {4}{G}
 * Creature — Giant Mercenary
 * 4/4
 *
 * Vigilance, reach
 * Ward {2}
 * When this creature enters, you gain X life, where X is the greatest power among Giants you control.
 */
val PummelerForHire = card("Pummeler for Hire") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Giant Mercenary"
    power = 4
    toughness = 4
    oracleText = "Vigilance, reach\n" +
        "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\n" +
        "When this creature enters, you gain X life, where X is the greatest power among Giants you control."

    keywords(Keyword.VIGILANCE, Keyword.REACH)
    keywordAbility(KeywordAbility.ward("{2}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(
            amount = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.GIANT)
            ).maxPower()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Steve Ellis"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42208996-7b99-474e-aba7-75190d7ee8e2.jpg?1767957240"
        ruling("2025-11-17", "The value of X is calculated only once, as Pummeler for Hire's last ability resolves.")
    }
}
