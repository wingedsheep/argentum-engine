package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val ArmedDangerous = card("Armed // Dangerous") {
    layout = CardLayout.SPLIT
    colorIdentity = "RG"
    keywords(Keyword.FUSE)

    face("Armed") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        oracleText = "Target creature gets +1/+1 and gains double strike until end of turn.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
        }
    }

    face("Dangerous") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "All creatures able to block target creature this turn do so.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.MustBeBlocked(creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff7f4fc2-6f76-44e7-a30b-7166a0d10d2a.jpg?1783940018"
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "You can choose the same object as the target of each half of a fused split spell, if appropriate.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "When resolving a fused split spell with multiple targets, treat it as you would any spell with multiple targets. If all targets are illegal when the spell tries to resolve, the spell doesn't resolve and none of its effects happen. If at least one target is still legal at that time, the spell resolves, but an illegal target can't perform any actions or have any actions performed on it.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
        ruling("2013-04-15", "Dangerous doesn't give any creatures the ability to block the target creature. It just forces those creatures that are already able to block the creature to do so.")
        ruling("2013-04-15", "As blockers are declared, any creature that's tapped or affected by a spell or ability that says it can't block doesn't block. If there's a cost associated with having the creature block, no player is forced to pay that cost, so it doesn't block if that cost isn't paid.")
    }
}
