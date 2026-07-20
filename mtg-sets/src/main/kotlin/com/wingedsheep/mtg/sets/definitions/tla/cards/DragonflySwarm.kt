package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dragonfly Swarm
 * {1}{U}{R}
 * Creature — Dragon Insect
 * * / 3
 *
 * Flying, ward {1} (Whenever this creature becomes the target of a spell or ability an opponent
 * controls, counter it unless that player pays {1}.)
 * This creature's power is equal to the number of noncreature, nonland cards in your graveyard.
 * When this creature dies, if there's a Lesson card in your graveyard, draw a card.
 */
val DragonflySwarm = card("Dragonfly Swarm") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Dragon Insect"
    oracleText = "Flying, ward {1} (Whenever this creature becomes the target of a spell or " +
        "ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "This creature's power is equal to the number of noncreature, nonland cards in your graveyard.\n" +
        "When this creature dies, if there's a Lesson card in your graveyard, draw a card."

    // Power is the number of noncreature, nonland cards in the controller's graveyard (CDA).
    dynamicPower(
        DynamicAmount.Count(
            player = Player.You,
            zone = Zone.GRAVEYARD,
            filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland
        )
    )
    toughness = 3

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.ward("{1}"))

    triggeredAbility {
        trigger = Triggers.Dies
        triggerCondition = Conditions.GraveyardContainsSubtype(Subtype.LESSON)
        effect = Effects.DrawCards(1)
        description = "When this creature dies, if there's a Lesson card in your graveyard, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "John Di Giovanni"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16be5cc4-b15a-4c5e-8a6e-05c87b519127.jpg?1764121542"
    }
}
