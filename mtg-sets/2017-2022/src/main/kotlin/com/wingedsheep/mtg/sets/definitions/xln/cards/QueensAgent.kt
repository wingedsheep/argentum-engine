package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Queen's Agent
 * {5}{B}
 * Creature — Vampire Scout
 * 3/3
 * Lifelink
 * When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)
 */
val QueensAgent = card("Queen's Agent") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Scout"
    power = 3
    toughness = 3
    oracleText = "Lifelink\nWhen this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3562f65a-bd69-47e4-9d9d-ab5c2c6ceb35.jpg?1783935758"
    }
}
