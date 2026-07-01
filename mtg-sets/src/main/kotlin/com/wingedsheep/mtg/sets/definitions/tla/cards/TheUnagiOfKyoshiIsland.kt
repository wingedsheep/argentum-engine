package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Unagi of Kyoshi Island
 * {3}{U}{U}
 * Legendary Creature — Serpent
 * 5/5
 * Flash
 * Ward—Waterbend {4}. (Whenever this creature becomes the target of a spell or ability an opponent
 * controls, counter it unless that player pays {4}. They can tap their artifacts and creatures to
 * help. Each one pays for {1}.)
 * Whenever an opponent draws their second card each turn, you draw two cards.
 *
 * Ward—Waterbend is the existing Ward keyword whose mana payment routes through the shared
 * waterbend tap-to-help machinery — [KeywordAbility.wardWaterbend] produces
 * `WardCost.Mana("{4}", waterbend = true)`, and the ward payment decision lets the paying player
 * tap their untapped artifacts/creatures (each paying {1}) before paying the remainder with mana.
 * The draw payoff reuses the [Triggers.NthCardDrawn] facade scoped to [Player.EachOpponent]
 * ("an opponent draws their second card each turn").
 */
val TheUnagiOfKyoshiIsland = card("The Unagi of Kyoshi Island") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Serpent"
    oracleText = "Flash\n" +
        "Ward—Waterbend {4}. (Whenever this creature becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {4}. They can tap their artifacts " +
        "and creatures to help. Each one pays for {1}.)\n" +
        "Whenever an opponent draws their second card each turn, you draw two cards."
    power = 5
    toughness = 5

    keywords(Keyword.FLASH)
    keywordAbility(KeywordAbility.wardWaterbend("{4}"))

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2, Player.EachOpponent)
        effect = Effects.DrawCards(2)
        description = "Whenever an opponent draws their second card each turn, you draw two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Miho Midorikawa"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ecd8b38-9ee5-41a5-9b93-21c33fc1a6ff.jpg?1764120514"
    }
}
