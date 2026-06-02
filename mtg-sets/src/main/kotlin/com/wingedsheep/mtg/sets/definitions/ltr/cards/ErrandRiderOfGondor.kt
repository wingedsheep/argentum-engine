package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Errand-Rider of Gondor
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 *
 * When this creature enters, draw a card. Then if you don't control a legendary
 * creature, put a card from your hand on the bottom of your library.
 */
val ErrandRiderOfGondor = card("Errand-Rider of Gondor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, draw a card. Then if you don't control a legendary creature, put a card from your hand on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1),
                ConditionalEffect(
                    condition = Conditions.Not(
                        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.legendary())
                    ),
                    effect = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.FromZone(Zone.HAND, Player.You),
                                storeAs = "handCards"
                            ),
                            SelectFromCollectionEffect(
                                from = "handCards",
                                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                                storeSelected = "chosen",
                                prompt = "Put a card from your hand on the bottom of your library"
                            ),
                            MoveCollectionEffect(
                                from = "chosen",
                                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
                            )
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "YW Tang"
        flavorText = "\"Gondor is in great need. Lord Denethor asks for all your strength and all your speed, lest Gondor fall at last.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3f990e7-54a3-4893-8510-645b2065447b.jpg?1686967733"
    }
}
