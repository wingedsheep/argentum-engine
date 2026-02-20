package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Ebon Dragon
 * {5}{B}{B}
 * Creature — Dragon
 * 5/4
 * Flying
 * When Ebon Dragon enters the battlefield, you may have target opponent discard a card.
 */
val EbonDragon = card("Ebon Dragon") {
    manaCost = "{5}{B}{B}"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 4
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        target = TargetOpponent()
        effect = Effects.Discard(1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f10cf69-d3dc-43a4-9595-0f7d245c5efa.jpg"
    }
}
