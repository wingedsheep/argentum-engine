package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mishra, Excavation Prodigy — The Brothers' War #140
 * {2}{R} · Legendary Creature — Human Artificer · 2/1
 *
 * Haste
 * {1}, {T}, Discard a card: Draw a card.
 * Whenever you discard one or more artifact cards, add {R}{R}. This ability triggers only once each turn.
 *
 * "One or more artifact cards" is a batch (CR 603.2c): `discards(Artifact, batch = true)` fires once
 * per discard event that holds at least one artifact card, however many it held, and non-artifact
 * cards in the same event neither add nor block. "Triggers only once each turn" is the `oncePerTurn`
 * firing cap. The trigger is not a mana ability (it doesn't trigger from activating one, CR 605.1b),
 * so it uses the stack; per the ruling it fires on any discard, not just Mishra's own looter.
 */
val MishraExcavationProdigy = card("Mishra, Excavation Prodigy") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "Haste\n" +
        "{1}, {T}, Discard a card: Draw a card.\n" +
        "Whenever you discard one or more artifact cards, add {R}{R}. This ability triggers only " +
        "once each turn."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.DrawCards(1)
        description = "{1}, {T}, Discard a card: Draw a card."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Artifact, batch = true)
        oncePerTurn = true
        effect = Effects.AddMana(Color.RED, 2)
        description = "Whenever you discard one or more artifact cards, add {R}{R}. This ability " +
            "triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Donato Giancola"
        flavorText = "While Urza read books, Mishra learned to read the desert itself."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5d02416-8578-40a5-bf31-d1ef9b8764f1.jpg?1783920068"
        ruling(
            "2022-10-14",
            "The last ability will trigger if you discard artifact cards for any reason, not just " +
                "due to its activated ability."
        )
    }
}
