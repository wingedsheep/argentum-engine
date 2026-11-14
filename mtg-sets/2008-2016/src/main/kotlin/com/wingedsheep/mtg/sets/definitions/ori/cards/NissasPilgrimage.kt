package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nissa's Pilgrimage
 * {2}{G}
 * Sorcery
 *
 * Search your library for up to two basic Forest cards, reveal those cards, and put one onto the
 * battlefield tapped and the rest into your hand. Then shuffle.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, search
 * your library for up to three basic Forest cards instead of two.
 *
 * Cultivate's pipeline (search up to N, then split one onto the battlefield tapped and the rest to
 * hand) over basic Forests. Spell mastery only raises the search cap, so it folds into the first
 * selection as a [DynamicAmounts.conditional], checked at resolution — the spell is still on the
 * stack then and never counts itself.
 */
val NissasPilgrimage = card("Nissa's Pilgrimage") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic Forest cards, reveal those cards, and put one onto " +
        "the battlefield tapped and the rest into your hand. Then shuffle.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, search your " +
        "library for up to three basic Forest cards instead of two."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST)),
                search = true
            )
            val found = chooseUpTo(
                DynamicAmounts.conditional(
                    condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                    ifTrue = 3,
                    ifFalse = 2
                ),
                from = searchable,
                prompt = "Search your library for basic Forest cards"
            )
            val (toBattlefield, toHandCards) = chooseExactlySplit(
                1,
                from = found,
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Into your hand",
                prompt = "Choose which Forest enters the battlefield tapped; the rest go to your hand."
            )
            move(
                toBattlefield,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                revealed = true
            )
            toHand(toHandCards, revealed = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/800cef89-e90c-4f84-a7a0-3cd203532169.jpg?1783938319"
        ruling("2015-06-22", "If you only find one basic Forest card, you'll put it onto the battlefield tapped. You won't be able to put it into your hand, even if you want to.")
        ruling("2015-06-22", "If the spell mastery ability applies, and you find three basic Forest cards, one of them will be put onto the battlefield tapped and two of them will be put into your hand.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
