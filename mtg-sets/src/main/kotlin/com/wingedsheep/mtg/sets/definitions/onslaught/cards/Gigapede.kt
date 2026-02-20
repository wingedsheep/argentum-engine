package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect

/**
 * Gigapede
 * {3}{G}{G}
 * Creature — Insect
 * 6/1
 * Shroud
 * At the beginning of your upkeep, if Gigapede is in your graveyard,
 * you may discard a card. If you do, return Gigapede to its owner's hand.
 */
val Gigapede = card("Gigapede") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Insect"
    power = 6
    toughness = 1
    oracleText = "Shroud\nAt the beginning of your upkeep, if Gigapede is in your graveyard, you may discard a card. If you do, return Gigapede to its owner's hand."

    keywords(Keyword.SHROUD)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerZone = Zone.GRAVEYARD
        effect = ReflexiveTriggerEffect(
            action = EffectPatterns.discardCards(1),
            optional = true,
            reflexiveEffect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Anthony S. Waters"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/0/a/0a96a608-9237-41c1-824c-89d5fad939ad.jpg?1562897409"
    }
}
