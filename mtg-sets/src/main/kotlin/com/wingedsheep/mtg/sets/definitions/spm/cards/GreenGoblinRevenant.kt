package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Green Goblin, Revenant — Marvel's Spider-Man #130
 * {3}{B}{R} · Legendary Creature — Goblin Human Villain · 3/3
 *
 * Flying, deathtouch
 * Whenever Green Goblin attacks, discard a card. Then draw a card for each card you've
 * discarded this turn.
 *
 * The discard resolves before the draw, so the just-discarded card is counted — the count is
 * read at the time the draw resolves via [DynamicAmounts.cardsDiscardedThisTurn].
 */
val GreenGoblinRevenant = card("Green Goblin, Revenant") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Goblin Human Villain"
    power = 3
    toughness = 3
    oracleText = "Flying, deathtouch\n" +
        "Whenever Green Goblin attacks, discard a card. Then draw a card for each card you've " +
        "discarded this turn."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            Effects.Discard(1),
            Effects.DrawCards(DynamicAmounts.cardsDiscardedThisTurn()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Chris Rahn"
        flavorText = "\"The Green Goblin lives again!\"\n—Green Goblin, Harry Osborn"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/218ef931-46f5-4a4d-9f26-898a1ff8f70f.jpg?1783905318"
    }
}
