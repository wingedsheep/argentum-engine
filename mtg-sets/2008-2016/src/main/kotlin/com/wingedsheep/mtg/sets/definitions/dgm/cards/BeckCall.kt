package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry

/**
 * Beck // Call
 * {G}{U} Sorcery // {4}{W}{U} Sorcery
 * Beck: Whenever a creature enters this turn, you may draw a card.
 * Call: Create four 1/1 white Bird creature tokens with flying.
 * Fuse
 *
 * Beck watches every creature, whoever controls it. Fused, Beck resolves first, so its
 * delayed trigger sees Call's four Birds enter.
 */
val BeckCall = card("Beck // Call") {
    layout = CardLayout.SPLIT
    colorIdentity = "GWU"
    keywords(Keyword.FUSE)

    face("Beck") {
        manaCost = "{G}{U}"
        typeLine = "Sorcery"
        oracleText = "Whenever a creature enters this turn, you may draw a card.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.a(GameObjectFilter.Creature).enters(),
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.May(Effects.DrawCards(1)),
            )
        }
    }

    face("Call") {
        manaCost = "{4}{W}{U}"
        typeLine = "Sorcery"
        oracleText = "Create four 1/1 white Bird creature tokens with flying.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Bird"),
                keywords = setOf(Keyword.FLYING),
                count = 4,
                imageUri = "https://cards.scryfall.io/normal/front/4/5/456832f6-60b5-46d7-b37e-4fb44e60e792.jpg?1783902809",
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a01d6540-9eaf-4e08-a62d-682551ee78e9.jpg?1783940017"
        ruling("2013-04-15", "Beck triggers when any creature enters the battlefield, no matter who controls it.")
        ruling("2013-04-15", "If you cast Beck // Call as a fused split spell, the triggered ability it creates will be in effect when the Bird creature tokens enter the battlefield. You'll draw up to four cards.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "Some split cards with fuse have two halves that are both multicolored. That card is multicolored no matter which half is cast, or if both halves are cast. It's also multicolored while not on the stack.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
    }
}
