package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Alive // Well
 * {3}{G} Sorcery // {W} Sorcery
 * Alive: Create a 3/3 green Centaur creature token.
 * Well: You gain 2 life for each creature you control.
 * Fuse
 *
 * Fused, Alive resolves first, so its Centaur counts toward Well's life gain.
 */
val AliveWell = card("Alive // Well") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"
    keywords(Keyword.FUSE)

    face("Alive") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Create a 3/3 green Centaur creature token.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Centaur"),
            )
        }
    }

    face("Well") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "You gain 2 life for each creature you control.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Effects.GainLife(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count() * 2
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db84415e-048a-4cfc-9121-5ae17a412198.jpg?1783940017"
        ruling("2013-04-15", "If you cast Alive // Well as a fused split spell, the Centaur creature token will count toward the amount of life you gain.")
        ruling("2013-04-15", "Count the number of creatures you control when Well resolves to determine how much life you gain.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
    }
}
