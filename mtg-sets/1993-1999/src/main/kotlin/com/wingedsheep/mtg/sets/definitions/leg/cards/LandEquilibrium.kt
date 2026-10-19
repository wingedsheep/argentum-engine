package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PutOntoBattlefieldThen
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Land Equilibrium
 * {2}{U}{U}
 * Enchantment
 * If an opponent who controls at least as many lands as you do would put a land onto the
 * battlefield, that player instead puts that land onto the battlefield then sacrifices a land of
 * their choice.
 *
 * "That player" is the one putting the land there ([Player.TriggeringPlayer]), whoever it enters
 * under; the land count is compared before the land arrives, and the player may sacrifice the land
 * that just entered — before they can tap it for mana.
 */
val LandEquilibrium = card("Land Equilibrium") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "If an opponent who controls at least as many lands as you do would put a land onto the " +
        "battlefield, that player instead puts that land onto the battlefield then sacrifices a land of their choice."

    replacementEffect(
        PutOntoBattlefieldThen(
            putter = Player.EachOpponent,
            condition = Conditions.CompareAmounts(
                DynamicAmounts.count(Player.TriggeringPlayer, Zone.BATTLEFIELD, GameObjectFilter.Land),
                ComparisonOperator.GTE,
                DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land)
            ),
            effect = Effects.Sacrifice(
                GameObjectFilter.Land,
                count = 1,
                target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
            ),
            appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.Land, to = Zone.BATTLEFIELD)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c406b25-03f8-4aaa-9ea7-48bf754166b7.jpg?1783948074"
    }
}
