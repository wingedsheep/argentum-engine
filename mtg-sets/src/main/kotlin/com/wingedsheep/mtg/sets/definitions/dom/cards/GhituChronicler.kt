package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.dsl.Effects
/**
 * Ghitu Chronicler
 * {1}{R}
 * Creature — Human Wizard
 * 1/3
 * Kicker {3}{R}
 * When this creature enters, if it was kicked, return target instant or sorcery
 * card from your graveyard to your hand.
 */
val GhituChronicler = card("Ghitu Chronicler") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Kicker {3}{R}\nWhen this creature enters, if it was kicked, return target instant or sorcery card from your graveyard to your hand."

    keywordAbility(KeywordAbility.kicker("{3}{R}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        val t = target("target", Targets.InstantOrSorceryInGraveyard)
        effect = Effects.Move(
            target = t,
            destination = Zone.HAND
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Anthony Palumbo"
        flavorText = "\"What fire forged, fire remembers.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3ca6dd9-040a-4122-9308-8dcd0d506ada.jpg?1562743531"
    }
}
