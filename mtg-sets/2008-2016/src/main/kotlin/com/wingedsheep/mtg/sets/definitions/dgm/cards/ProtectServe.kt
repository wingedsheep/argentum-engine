package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Protect // Serve
 * {2}{W} Instant // {1}{U} Instant
 * Protect: Target creature gets +2/+4 until end of turn.
 * Serve: Target creature gets -6/-0 until end of turn.
 * Fuse
 */
val ProtectServe = card("Protect // Serve") {
    layout = CardLayout.SPLIT
    colorIdentity = "WU"
    keywords(Keyword.FUSE)

    face("Protect") {
        manaCost = "{2}{W}"
        typeLine = "Instant"
        oracleText = "Target creature gets +2/+4 until end of turn.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 4, creature)
        }
    }

    face("Serve") {
        manaCost = "{1}{U}"
        typeLine = "Instant"
        oracleText = "Target creature gets -6/-0 until end of turn.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(-6, 0, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b8acd7d-f3e2-4358-91ab-40901b68d64c.jpg?1783940016"
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
