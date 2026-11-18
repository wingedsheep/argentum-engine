package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Siege Dragon — Magic 2015 #162 (canonical printing)
 * {5}{R}{R} · Creature — Dragon · 5/5
 *
 * Flying
 * When this creature enters, destroy all Walls your opponents control.
 * Whenever this creature attacks, if defending player controls no Walls, it deals 2 damage to each
 * creature without flying that player controls.
 *
 * "Defending player" is the player this Dragon attacks (the controller of the planeswalker it
 * attacks), read per attacker through `Player.DefendingPlayer` — both in the intervening "if"
 * (checked when the trigger fires and again on resolution) and in the damage filter, so in
 * multiplayer neither another opponent's Walls nor their creatures are involved.
 */
val SiegeDragon = card("Siege Dragon") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, destroy all Walls your opponents control.\n" +
        "Whenever this creature attacks, if defending player controls no Walls, it deals 2 damage " +
        "to each creature without flying that player controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DestroyAll(GameObjectFilter.Permanent.withSubtype(Subtype.WALL).opponentControls())
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(
                Player.DefendingPlayer,
                GameObjectFilter.Permanent.withSubtype(Subtype.WALL)
            ).count(),
            ComparisonOperator.EQ,
            0
        )
        effect = Patterns.Group.dealDamageToAll(
            2,
            GroupFilter(
                GameObjectFilter.Creature
                    .withoutKeyword(Keyword.FLYING)
                    .targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12dc18f7-f4d4-421e-869e-c2e7598a9c77.jpg?1783939169"
    }
}
