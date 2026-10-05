package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mystic Skyfish
 * {2}{U}
 * Creature — Fish
 * 3/1
 *
 * Whenever you draw your second card each turn, this creature gains flying until end of turn.
 *
 * `Triggers.you.drawsNth(2)` counts draws (CR 121.2) for the turn, whether or not this creature
 * was on the battlefield for the first one, and fires at most once per turn.
 */
val MysticSkyfish = card("Mystic Skyfish") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    power = 3
    toughness = 1
    oracleText = "Whenever you draw your second card each turn, this creature gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        description = "Whenever you draw your second card each turn, this creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "326"
        artist = "Alayna Danner"
        flavorText = "The problem wasn't that fish had learned how to fly. It was that sharks had adapted to follow them."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0002ab72-834b-4c81-82b1-0d2760ea96b0.jpg?1783930623"

        ruling(
            "2020-06-23",
            "The triggered ability can trigger only once each turn. It doesn't matter if Mystic Skyfish " +
                "was on the battlefield when the first card was drawn. If it's not on the battlefield when " +
                "the second card is drawn, the ability can't trigger at all that turn. It won't trigger when " +
                "the third or fourth card is drawn."
        )
        ruling(
            "2020-06-23",
            "If a spell or ability causes you to put a card into your hand without specifically using the " +
                "word \"draw,\" it's not a card drawn."
        )
    }
}
