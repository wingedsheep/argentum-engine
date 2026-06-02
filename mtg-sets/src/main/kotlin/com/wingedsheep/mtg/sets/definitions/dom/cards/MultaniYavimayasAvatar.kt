package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Multani, Yavimaya's Avatar
 * {4}{G}{G}
 * Legendary Creature — Elemental Avatar
 * 0/0
 * Reach, trample
 * Multani, Yavimaya's Avatar gets +1/+1 for each land you control and each land card
 * in your graveyard.
 * {1}{G}, Return two lands you control to their owner's hand: Return Multani from
 * your graveyard to your hand.
 */
val MultaniYavimayasAvatar = card("Multani, Yavimaya's Avatar") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elemental Avatar"
    power = 0
    toughness = 0
    oracleText = "Reach, trample\nMultani, Yavimaya's Avatar gets +1/+1 for each land you control and each land card in your graveyard.\n{1}{G}, Return two lands you control to their owner's hand: Return Multani from your graveyard to your hand."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    dynamicStats(
        DynamicAmount.Add(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Land)
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.ReturnToHand(GameObjectFilter.Land, count = 2)
        )
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5233ad7b-2903-4736-b13a-5cd4a275eb61.jpg?1562735650"
        ruling("2018-04-27", "Multani's ability that modifies its power and toughness applies only while it's on the battlefield. In all other zones, it's a 0/0 creature card.")
        ruling("2018-04-27", "To activate Multani's last ability, you must return lands you control from the battlefield to their owner's hand. Land cards in your graveyard can't be returned this way.")
    }
}
