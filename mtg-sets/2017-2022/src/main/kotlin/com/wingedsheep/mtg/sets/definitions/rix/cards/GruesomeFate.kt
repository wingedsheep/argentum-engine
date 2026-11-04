package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gruesome Fate
 * {2}{B}
 * Sorcery
 * Each opponent loses 1 life for each creature you control.
 *
 * The count is taken once, on resolution, and each opponent loses that much.
 */
val GruesomeFate = card("Gruesome Fate") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"
    oracleText = "Each opponent loses 1 life for each creature you control."

    spell {
        effect = Effects.LoseLife(
            DynamicAmounts.creaturesYouControl(),
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Even Amundsen"
        flavorText = "\"You were keen enough to sink your fangs into us, vampire! And now you're afraid of a few little nibbles?\"\n—Udvil \"Ratty\" Ratlock"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ea07116-afe7-4d88-a530-54424e791b74.jpg?1783935311"
    }
}
