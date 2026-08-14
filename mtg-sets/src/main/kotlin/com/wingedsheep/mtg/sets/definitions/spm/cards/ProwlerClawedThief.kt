package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Prowler, Clawed Thief
 * {1}{U}{B}
 * Legendary Creature — Human Rogue Villain, 2/3
 * Menace
 * Whenever another Villain you control enters, Prowler connives.
 */
val ProwlerClawedThief = card("Prowler, Clawed Thief") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Rogue Villain"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nWhenever another Villain you control enters, Prowler connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype("Villain").youControl(),
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.Connive()
        description = "Whenever another Villain you control enters, Prowler connives."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Anthony Devine"
        flavorText = "\"It's nothing personal, kid. It's just money.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd31953a-7259-44e3-a94f-013bda68006d.jpg?1757377763"
    }
}
