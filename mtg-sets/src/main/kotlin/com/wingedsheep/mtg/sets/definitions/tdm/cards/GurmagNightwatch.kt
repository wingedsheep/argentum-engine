package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Gurmag Nightwatch — Tarkir: Dragonstorm #190
 * {2/B}{2/G}{2/U} · Creature — Human Ranger · 3/3
 *
 * When this creature enters, look at the top three cards of your library. You may put
 * one of those cards back on top of your library. Put the rest into your graveyard.
 *
 * Composed from the look-at-top / select / bin pipeline:
 *   1. gather the top three cards into a private collection ("looked"),
 *   2. let the controller choose up to one to keep on top of the library,
 *   3. the kept card returns to the top, the remainder is put into the graveyard.
 *
 * The mana cost is monocolored hybrid ("twobrid"): each symbol can be paid with 2 generic
 * or one mana of the listed color, so the card is castable in any of B/G/U decks.
 */
val GurmagNightwatch = card("Gurmag Nightwatch") {
    manaCost = "{2/B}{2/G}{2/U}"
    colorIdentity = "BGU"
    typeLine = "Creature — Human Ranger"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, look at the top three cards of your library. " +
        "You may put one of those cards back on top of your library. Put the rest into your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "keptOnTop",
                storeRemainder = "toGraveyard",
                selectedLabel = "Put back on top of your library",
                remainderLabel = "Put into your graveyard"
            ),
            MoveCollectionEffect(
                from = "keptOnTop",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
            ),
            MoveCollectionEffect(
                from = "toGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        ))
        description = "When this creature enters, look at the top three cards of your library. " +
            "You may put one of those cards back on top of your library. Put the rest into your graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Nereida"
        flavorText = "\"The fishers were probably exaggerating the size of the crocodile they spotted, " +
            "but maybe they weren't. Let's check it out.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de731430-6bbf-4782-953e-b69c46353959.jpg?1743204741"
    }
}
