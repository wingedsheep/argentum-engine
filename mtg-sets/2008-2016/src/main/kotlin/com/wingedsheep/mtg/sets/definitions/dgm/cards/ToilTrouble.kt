package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity

/**
 * Toil // Trouble
 * {2}{B} Sorcery // {2}{R} Sorcery
 * Toil: Target player draws two cards and loses 2 life.
 * Trouble: Trouble deals damage to target player equal to the number of cards in that player's hand.
 * Fuse
 *
 * Trouble reads the targeted player's hand size when it resolves, so in a fused cast at the same
 * player the two cards Toil drew are counted (the 2013-04-15 ruling).
 */
val ToilTrouble = card("Toil // Trouble") {
    layout = CardLayout.SPLIT
    colorIdentity = "BR"
    keywords(Keyword.FUSE)

    face("Toil") {
        manaCost = "{2}{B}"
        typeLine = "Sorcery"
        oracleText = "Target player draws two cards and loses 2 life.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val player = target(Targets.Player)
            effect = Effects.DrawCards(2, player) then Effects.LoseLife(2, player)
        }
    }

    face("Trouble") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        oracleText = "Trouble deals damage to target player equal to the number of cards in that player's hand.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val player = target(Targets.Player)
            effect = Effects.DealDamage(
                DynamicAmounts.zone(player.asPlayer, Zone.HAND).count(),
                player,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15bb3454-e3bb-4af9-9e93-461e210c26b7.jpg?1783940014"
        ruling("2013-04-15", "If you cast Toil // Trouble as a fused split card and choose to target the same player twice, the two drawn cards are counted when determining how much damage is dealt. The player can't do anything between the effects of Toil and Trouble to have fewer cards in their hand.")
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
