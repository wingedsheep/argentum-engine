package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Most Decrepit Old Bird // Speak Secrets
 * {U}
 * Creature — Bird
 * 1/1
 *
 * Flying
 * Threshold — This creature gets +1/+1 as long as there are seven or more cards in your graveyard.
 *
 * Adventure: Speak Secrets — {1}{U}, Sorcery — Adventure
 * Mill four cards, then put an instant or sorcery card from among them into your hand.
 *
 * Threshold is a [ConditionalStaticAbility] over `GroupFilter.source()` (Billowing Shriekmass), so the
 * bonus turns on and off as the graveyard count crosses seven rather than being locked in on entry.
 *
 * The Adventure mills first (the four cards really reach the graveyard) and then pulls one instant or
 * sorcery from among *those four*. `ChooseExactly(1)` is the mandatory-if-able shape the oracle text
 * calls for — it is not "you may" — and the executor auto-resolves to nothing when none of the milled
 * cards is an instant or sorcery.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val MostDecrepitOldBird = card("Most Decrepit Old Bird") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Threshold — This creature gets +1/+1 as long as there are seven or more cards in your graveyard."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, GroupFilter.source()),
            condition = Conditions.CardsInGraveyardAtLeast(7)
        )
    }

    adventure("Speak Secrets") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Mill four cards, then put an instant or sorcery card from among them into your hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(
                listOf(
                    // Mill four cards.
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4), Player.You, isMill = true),
                        storeAs = "milled"
                    ),
                    MoveCollectionEffect(
                        from = "milled",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD)
                    ),
                    // Then put an instant or sorcery card from among them into your hand.
                    SelectFromCollectionEffect(
                        from = "milled",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        filter = GameObjectFilter.InstantOrSorcery,
                        storeSelected = "toHand",
                        showAllCards = true,
                        prompt = "Put an instant or sorcery card from among the milled cards into your hand",
                        selectedLabel = "Put in hand",
                        remainderLabel = "Leave in graveyard"
                    ),
                    MoveCollectionEffect(
                        from = "toHand",
                        destination = CardDestination.ToZone(Zone.HAND)
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d838feb-89f2-4cdb-a5ab-ec880f28d873.jpg?1785236659"
    }
}
