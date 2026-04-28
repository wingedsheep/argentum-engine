package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kulrath Zealot
 * {5}{R}
 * Creature — Elemental Warrior
 * 6/5
 *
 * When this creature enters, exile the top card of your library. Until the end of your next turn,
 * you may play that card.
 * Basic landcycling {1}{R} ({1}{R}, Discard this card: Search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.)
 */
val KulrathZealot = card("Kulrath Zealot") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Elemental Warrior"
    power = 6
    toughness = 5
    oracleText = "When this creature enters, exile the top card of your library. Until the end of your next turn, you may play that card.\nBasic landcycling {1}{R}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "exiledCard"
            ),
            MoveCollectionEffect(
                from = "exiledCard",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.UntilEndOfNextTurn)
        ))
    }

    keywordAbility(KeywordAbility.BasicLandcycling(ManaCost.parse("{1}{R}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Karl Kopinski"
        flavorText = "The Path of Flame brought him power at the terrible cost of his own identity."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3502685d-4e57-4c5c-94c6-ae69048cdfbf.jpg?1767871934"
    }
}
