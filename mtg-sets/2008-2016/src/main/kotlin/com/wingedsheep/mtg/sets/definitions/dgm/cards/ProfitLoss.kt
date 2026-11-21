package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Profit // Loss
 * {1}{W} Instant // {2}{B} Instant
 * Profit: Creatures you control get +1/+1 until end of turn.
 * Loss: Creatures your opponents control get -1/-1 until end of turn.
 * Fuse
 */
val ProfitLoss = card("Profit // Loss") {
    layout = CardLayout.SPLIT
    colorIdentity = "WB"
    keywords(Keyword.FUSE)

    face("Profit") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        oracleText = "Creatures you control get +1/+1 until end of turn.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Patterns.Group.modifyStatsForAll(1, 1, Filters.Group.creaturesYouControl, Duration.EndOfTurn)
        }
    }

    face("Loss") {
        manaCost = "{2}{B}"
        typeLine = "Instant"
        oracleText = "Creatures your opponents control get -1/-1 until end of turn.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Patterns.Group.modifyStatsForAll(-1, -1, Filters.Group.creaturesOpponentsControl, Duration.EndOfTurn)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eb3ce46-ddd2-43b3-9e45-019ae91df686.jpg?1783940015"
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "You can choose the same object as the target of each half of a fused split spell, if appropriate.")
        ruling("2013-04-15", "When resolving a fused split spell with multiple targets, treat it as you would any spell with multiple targets. If all targets are illegal when the spell tries to resolve, the spell doesn't resolve and none of its effects happen. If at least one target is still legal at that time, the spell resolves, but an illegal target can't perform any actions or have any actions performed on it.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "Some split cards with fuse have two halves that are both multicolored. That card is multicolored no matter which half is cast, or if both halves are cast. It's also multicolored while not on the stack.")
        ruling("2013-04-15", "If a player names a card, the player may name either half of a split card, but not both. A split card has the chosen name if one of its two names matches the chosen name.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
    }
}
