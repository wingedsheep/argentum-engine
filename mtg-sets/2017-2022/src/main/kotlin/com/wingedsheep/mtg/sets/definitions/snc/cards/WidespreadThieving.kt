package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Widespread Thieving
 * {2}{R}
 * Enchantment
 *
 * Hideaway 5 (When this enchantment enters, look at the top five cards of your library, exile
 * one face down, then put the rest on the bottom in a random order.)
 * Whenever you cast a multicolored spell, create a Treasure token. Then you may pay
 * {W}{U}{B}{R}{G}. If you do, you may play the exiled card without paying its mana cost.
 *
 * Hideaway is the same composition as Fight Rigging / Wiretapping / Cemetery Tampering. The
 * Treasure is created before the optional {W}{U}{B}{R}{G} payment is offered, so (per the
 * ruling) it can help pay. "If you do, you may play…" is a second, independent choice: paying
 * doesn't force the free play.
 */
val WidespreadThieving = card("Widespread Thieving") {
    manaCost = "{2}{R}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment"
    oracleText = "Hideaway 5 (When this enchantment enters, look at the top five cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "Whenever you cast a multicolored spell, create a Treasure token. Then you may pay " +
        "{W}{U}{B}{R}{G}. If you do, you may play the exiled card without paying its mana cost."

    keywordAbility(KeywordAbility.hideaway(5))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val widespreadThievingTop = gather(
                CardSource.TopOfLibrary(
                    count = 5,
                    player = Player.You
                )
            )
            val (widespreadThievingPicked, widespreadThievingRest) = chooseExactlySplit(
                1,
                from = widespreadThievingTop,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of library"
            )
            exile(widespreadThievingPicked, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
            toLibraryBottom(widespreadThievingRest, order = CardOrder.Random)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Multicolored)
        effect = Effects.CreateTreasure(1) then
            Effects.MayPay(
                cost = ManaCost.parse("{W}{U}{B}{R}{G}"),
                then = Effects.May(
                    Effects.Pipeline {
                        val widespreadThievingLinked = gather(CardSource.FromLinkedExile())
                        run(Effects.PlayFromCollectionWithoutPayingCost(widespreadThievingLinked))
                    },
                    descriptionOverride = "Play the exiled card without paying its mana cost"
                )
            )
        description = "Whenever you cast a multicolored spell, create a Treasure token. Then you " +
            "may pay {W}{U}{B}{R}{G}. If you do, you may play the exiled card without paying its " +
            "mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd22723d-855f-4b31-827f-a4e388ebbc07.jpg?1783923111"
        ruling(
            "2022-04-29",
            "You may use the Treasure token created by the first part of Widespread Thieving's " +
                "last ability to pay part of the {W}{U}{B}{R}{G} cost in the second part of that " +
                "ability."
        )
        ruling(
            "2022-04-29",
            "\"Hideaway N\" means \"When this permanent enters the battlefield, look at the top N " +
                "cards of your library. Exile one of them face down and put the rest on the bottom " +
                "of your library in a random order. The exiled card gains 'The player who controls " +
                "the permanent that exiled this card may look at this card in the exile zone.'\""
        )
        ruling(
            "2022-04-29",
            "Any player who has controlled a permanent with a hideaway ability since a card was " +
                "exiled with it may look at that card."
        )
        ruling(
            "2022-04-29",
            "Hideaway now causes you to put the rest of the cards on the bottom of your library " +
                "in a random order instead of any order."
        )
    }
}
