package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Siphon Insight
 * {U}{B}
 * Instant
 * Look at the top two cards of target opponent's library. Exile one of them face down and put the
 * other on the bottom of that library. You may play the exiled card for as long as it remains
 * exiled, and you may spend mana as though it were mana of any color to cast that spell.
 * Flashback {1}{U}{B}
 *
 * Black Cat, Cunning Thief's look-split-exile-face-down shape over the targeted opponent's library
 * ([FaceDownMode.HIDDEN] — only the grantee may look at the exiled card, per the ruling), with a
 * permanent may-play grant. "As though it were mana of any color" is `withAnyManaColor` (Stolen
 * Strategy, Curse of Hospitality), not the broader any-*type*. "Play" includes lands, so the grant
 * isn't `nonLandOnly`.
 */
val SiphonInsight = card("Siphon Insight") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Look at the top two cards of target opponent's library. Exile one of them face " +
        "down and put the other on the bottom of that library. You may play the exiled card for " +
        "as long as it remains exiled, and you may spend mana as though it were mana of any color " +
        "to cast that spell.\n" +
        "Flashback {1}{U}{B} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val topTwo = gather(CardSource.TopOfLibrary(2, opponent.asPlayer))
            val (exiled, other) = chooseExactlySplit(
                1,
                from = topTwo,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of that library"
            )
            exile(exiled, opponent.asPlayer, faceDown = FaceDownMode.HIDDEN)
            toLibraryBottom(other, opponent.asPlayer, order = CardOrder.Preserve)
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.Permanent,
                withAnyManaColor = true,
            ))
        }
    }

    keywordAbility(KeywordAbility.flashback("{1}{U}{B}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d329e2d4-e483-4981-8a0b-6a9a717d50cd.jpg?1783925553"
        ruling("2021-09-24", "Only you get to look at the exiled card. Its owner doesn't get to know what it is while it is in exile unless you tell them.")
        ruling("2021-09-24", "You may play the card even if it is a land.")
        ruling("2021-09-24", "You must still follow all normal timing rules to play a card exiled with Siphon Insight.")
        ruling("2021-09-24", "You don't have to disclose which card you exiled and which card you put on the bottom of the library. For example, if your opponent knows what the top card of their library is and you cast Siphon Insight, you don't have to tell them where you put that card.")
    }
}
