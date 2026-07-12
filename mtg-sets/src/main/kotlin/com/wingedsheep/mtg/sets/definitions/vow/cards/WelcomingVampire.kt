package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect

/**
 * Welcoming Vampire
 * {2}{W}
 * Creature — Vampire
 * 2/3
 * Flying
 * Whenever one or more other creatures you control with power 2 or less enter, draw a card.
 * This ability triggers only once each turn.
 */
val WelcomingVampire = card("Welcoming Vampire") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\nWhenever one or more other creatures you control with power 2 or less enter, draw a card. This ability triggers only once each turn."
    power = 2
    toughness = 3
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.powerAtMost(2).youControl(),
            binding = TriggerBinding.OTHER
        )
        oncePerTurn = true
        effect = DrawCardsEffect(1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Lorenzo Mastroianni"
        flavorText = "From the moment they arrived, wedding guests were met with a pageant of wealth and excess."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8f69cea-823c-482b-a605-8138b3d950e6.jpg?1782703161"
    }
}
