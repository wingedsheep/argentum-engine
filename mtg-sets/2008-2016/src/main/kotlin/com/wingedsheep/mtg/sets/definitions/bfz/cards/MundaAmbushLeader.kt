package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Munda, Ambush Leader
 * {2}{R}{W}
 * Legendary Creature — Kor Ally
 * 3/4
 * Haste
 * Rally — Whenever Munda or another Ally you control enters, you may look at the top four cards of
 * your library. If you do, reveal any number of Ally cards from among them, then put those cards on
 * top of your library in any order and the rest on the bottom in any order.
 *
 * Rally is an ability word: an ANY-bound enters trigger over Allies you control, so Munda's own
 * arrival fires it too. The "you may look … If you do" is the trigger's optional gate; declining
 * leaves the library untouched. All four looked-at cards are shown, only Ally cards are selectable;
 * the chosen Allies are revealed to the opponent, then each pile is ordered by its controller.
 */
val MundaAmbushLeader = card("Munda, Ambush Leader") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Kor Ally"
    power = 3
    toughness = 4
    oracleText = "Haste\nRally — Whenever Munda or another Ally you control enters, you may look at the top " +
        "four cards of your library. If you do, reveal any number of Ally cards from among them, then put " +
        "those cards on top of your library in any order and the rest on the bottom in any order."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.ALLY).youControl()).enters()
        optional = true
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (allies, rest) = chooseAnyNumberSplit(
                from = looked,
                filter = GameObjectFilter.Any.withSubtype(Subtype.ALLY),
                prompt = "Reveal any number of Ally cards to put on top of your library",
                selectedLabel = "Reveal and put on top",
                remainderLabel = "Put on bottom",
                showAllCards = true,
            )
            reveal(allies, revealToSelf = false)
            toLibraryTop(allies)
            toLibraryBottom(rest)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8ee09fe-46d7-492b-a2f3-3fb0c3f83f07.jpg?1783938179"
        ruling(
            "2015-08-25",
            "If a creature with a rally ability enters the battlefield under your control at the same time as " +
                "other Allies, that ability will trigger once for each of those creatures and once for the " +
                "creature with the ability itself."
        )
    }
}
