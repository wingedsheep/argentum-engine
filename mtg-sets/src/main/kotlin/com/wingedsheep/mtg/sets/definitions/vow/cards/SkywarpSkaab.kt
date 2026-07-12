package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Skywarp Skaab
 * {3}{U}{U}
 * Creature — Zombie Drake
 * 2/5
 *
 * Flying
 * When this creature enters, you may exile two creature cards from your graveyard. If you do,
 * draw a card.
 *
 * The ETB is the "exile exactly two" pipeline gated by [IfYouDoEffect] (see Aegis Sculptor):
 * gather creature cards from your graveyard, choose exactly two, move them to exile, and only
 * draw when both were actually exiled ([SuccessCriterion.CollectionNonEmpty] with `min = 2`).
 * With fewer than two creature cards the player can't complete the exile, so no card is drawn.
 */
val SkywarpSkaab = card("Skywarp Skaab") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Drake"
    power = 2
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, you may exile two creature cards from your graveyard. If you " +
        "do, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            IfYouDoEffect(
                action = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                zone = Zone.GRAVEYARD,
                                filter = GameObjectFilter.Creature
                            ),
                            storeAs = "graveyardCreatures"
                        ),
                        SelectFromCollectionEffect(
                            from = "graveyardCreatures",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                            storeSelected = "toExile",
                            selectedLabel = "Exile"
                        ),
                        MoveCollectionEffect(
                            from = "toExile",
                            destination = CardDestination.ToZone(Zone.EXILE)
                        )
                    )
                ),
                ifYouDo = Effects.DrawCards(1),
                successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 2)
            )
        )
        description = "When this creature enters, you may exile two creature cards from your " +
            "graveyard. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Steven Belledin"
        flavorText = "\"If I wanted all the parts to match, I wouldn't have become a stitcher.\"\n" +
            "—Barton, stitcher"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73168804-3c22-4fcb-907a-2f08999c0cea.jpg?1782703137"
    }
}
