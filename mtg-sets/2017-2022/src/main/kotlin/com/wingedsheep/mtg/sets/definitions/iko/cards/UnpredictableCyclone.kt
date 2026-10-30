package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unpredictable Cyclone — Ikoria: Lair of Behemoths #139
 * {3}{R}{R} · Enchantment
 *
 * If a cycling ability of another nonland card would cause you to draw a card, instead exile cards
 * from the top of your library until you exile a card that shares a card type with the cycled card.
 * You may cast that card without paying its mana cost. Then put the exiled cards that weren't cast
 * this way on the bottom of your library in a random order.
 * Cycling {2}
 *
 * A [ReplaceDrawWith] scoped by `DrawEvent.cyclingOf`: only the draw a cycling ability of a nonland
 * card causes is replaced, and the cycled card is the replacement's [EffectTarget.TriggeringEntity].
 * "Another" needs no filter — the Cyclone is on the battlefield, so it can't be the card cycled.
 */
val UnpredictableCyclone = card("Unpredictable Cyclone") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "If a cycling ability of another nonland card would cause you to draw a card, " +
        "instead exile cards from the top of your library until you exile a card that shares a card " +
        "type with the cycled card. You may cast that card without paying its mana cost. Then put " +
        "the exiled cards that weren't cast this way on the bottom of your library in a random order.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.Pipeline {
                val (found, exiled) = gatherUntilMatch(
                    GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.TriggeringEntity)
                )
                exile(exiled)
                ifNotEmpty(found) {
                    run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
                }
                val notCast = filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
                toLibraryBottom(notCast, order = CardOrder.Random)
            },
            appliesTo = EventPattern.DrawEvent(cyclingOf = GameObjectFilter.Nonland),
        )
    )

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a0462f6-a027-43c0-8e01-c0591d9a45d9.jpg?1783931041"
        ruling("2020-04-17", "If the cycled card has multiple card types, you stop when you exile a card that shares any of those types. You don't exile card to find one that shares all of its types, and you can't choose to keep going to find a different one of those types.")
        ruling("2020-04-17", "If you exile your entire library without exiling a card that shares a card type, you put the library back in a random order and continue. You won't draw a card.")
        ruling("2020-04-17", "If you wish to cast the last exiled card, you do so as part of the resolution of the cycling ability. You can't wait to cast it later. Timing permissions based on a card's type are ignored.")
    }
}
