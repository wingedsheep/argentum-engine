package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vizier of Tumbling Sands
 * {2}{U}
 * Creature — Human Cleric
 * 1/3
 * {T}: Untap another target permanent.
 * Cycling {1}{U} ({1}{U}, Discard this card: Draw a card.)
 * When you cycle this card, untap target permanent.
 */
val VizierOfTumblingSands = card("Vizier of Tumbling Sands") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "{T}: Untap another target permanent.\n" +
        "Cycling {1}{U} ({1}{U}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, untap target permanent."

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.Permanent.copy(excludeSelf = true))
        effect = Effects.Untap(permanent)
        description = "Untap another target permanent."
    }

    keywordAbility(KeywordAbility.cycling("{1}{U}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Untap(permanent)
        description = "When you cycle this card, untap target permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Josu Hernaiz"
        flavorText = "\"Time is like the Luxa River—though it flows in one direction, it can be diverted.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce4ff0f5-abee-4f3e-89ae-1b7ee771ec68.jpg"
    }
}
