package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val TAKE_REMOVED = "give_take_removed"

/**
 * Give // Take
 * {2}{G} Sorcery // {2}{U} Sorcery
 * Give: Put three +1/+1 counters on target creature.
 * Take: Remove all +1/+1 counters from target creature you control. Draw that many cards.
 * Fuse
 *
 * "That many" is the number of counters Take removes, so it is captured with
 * [Effects.StoreNumber] before the removal (reading the count afterwards would give 0). Fused onto
 * the same creature, Give resolves first, so its three counters are among those removed and
 * drawn for (the 2013-04-15 ruling).
 */
val GiveTake = card("Give // Take") {
    layout = CardLayout.SPLIT
    colorIdentity = "GU"
    keywords(Keyword.FUSE)

    face("Give") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery"
        oracleText = "Put three +1/+1 counters on target creature.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, creature)
        }
    }

    face("Take") {
        manaCost = "{2}{U}"
        typeLine = "Sorcery"
        oracleText = "Remove all +1/+1 counters from target creature you control. Draw that many cards.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.StoreNumber(
                TAKE_REMOVED,
                DynamicAmounts.countersOn(creature, CounterType.PLUS_ONE_PLUS_ONE)
            ) then
                Effects.RemoveAllCountersOfType(CounterType.PLUS_ONE_PLUS_ONE, creature) then
                Effects.DrawCards(DynamicAmounts.storedNumber(TAKE_REMOVED))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9af07d28-45a2-45d6-b1cb-0858c609a881.jpg?1783940016"
        ruling("2013-04-15", "If you cast Give // Take as a fused split spell, you can target the same creature for both parts. If you do so, you will first put three +1/+1 counters on that creature, then remove all +1/+1 counters from it, then draw that many cards.")
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
        ruling("2013-04-15", "You can target any creature you control with Take; the target does not need to have any +1/+1 counters on it.")
    }
}
