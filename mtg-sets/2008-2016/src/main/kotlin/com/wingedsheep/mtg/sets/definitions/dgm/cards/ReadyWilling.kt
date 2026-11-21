package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ready // Willing
 * {1}{G}{W} Instant // {1}{W}{B} Instant
 * Ready: Creatures you control gain indestructible until end of turn. Untap each creature you control.
 * Willing: Creatures you control gain deathtouch and lifelink until end of turn.
 * Fuse
 *
 * Both halves are group effects: the affected creatures are gathered when each instruction resolves,
 * so a creature that comes under your control afterwards gains nothing (the 2013 rulings).
 */
val ReadyWilling = card("Ready // Willing") {
    layout = CardLayout.SPLIT
    colorIdentity = "WBG"
    keywords(Keyword.FUSE)

    face("Ready") {
        manaCost = "{1}{G}{W}"
        typeLine = "Instant"
        oracleText = "Creatures you control gain indestructible until end of turn. Untap each creature you control.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Patterns.Group.grantKeywordToAll(
                keyword = Keyword.INDESTRUCTIBLE,
                filter = GroupFilter.AllCreaturesYouControl
            ) then Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.Untap(EffectTarget.IterationEntity)
            )
        }
    }

    face("Willing") {
        manaCost = "{1}{W}{B}"
        typeLine = "Instant"
        oracleText = "Creatures you control gain deathtouch and lifelink until end of turn.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Patterns.Group.grantKeywordToAll(
                keyword = Keyword.DEATHTOUCH,
                filter = GroupFilter.AllCreaturesYouControl
            ) then Patterns.Group.grantKeywordToAll(
                keyword = Keyword.LIFELINK,
                filter = GroupFilter.AllCreaturesYouControl
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22081f95-dc8e-41ed-b609-b6a22ee5428b.jpg?1783940014"
        ruling("2013-07-01", "A creature that comes under your control after Ready resolves will not be granted by this effect.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "You can choose the same object as the target of each half of a fused split spell, if appropriate.")
        ruling("2013-04-15", "When resolving a fused split spell with multiple targets, treat it as you would any spell with multiple targets. If all targets are illegal when the spell tries to resolve, the spell doesn't resolve and none of its effects happen. If at least one target is still legal at that time, the spell resolves, but an illegal target can't perform any actions or have any actions performed on it.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "Some split cards with fuse have two halves that are both multicolored. That card is multicolored no matter which half is cast, or if both halves are cast. It's also multicolored while not on the stack.")
        ruling("2013-04-15", "If a player names a card, the player may name either half of a split card, but not both. A split card has the chosen name if one of its two names matches the chosen name.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
        ruling("2013-04-15", "Multiple instances of deathtouch or lifelink are redundant.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "A creature that comes under your control after Willing resolves won't have deathtouch or lifelink, because adding abilities does change a creature's characteristics.")
    }
}
