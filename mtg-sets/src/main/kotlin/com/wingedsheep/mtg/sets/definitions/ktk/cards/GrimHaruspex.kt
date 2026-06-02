package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Grim Haruspex
 * {2}{B}
 * Creature — Human Wizard
 * 3/2
 * Morph {B}
 * Whenever another nontoken creature you control dies, draw a card.
 */
val GrimHaruspex = card("Grim Haruspex") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "Morph {B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhenever another nontoken creature you control dies, draw a card."

    morph = "{B}"

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().copy(
                    cardPredicates = GameObjectFilter.Creature.cardPredicates + CardPredicate.IsNontoken
                ),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Seb McKinnon"
        flavorText = "\"We all want to know what's going on in someone else's head. I simply open it up and look.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf1fe2bf-1640-4781-87b4-09a1f7d35831.jpg?1562793791"
    }
}
