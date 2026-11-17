package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shockmaw Dragon
 * {4}{R}{R}
 * Creature — Dragon
 * 4/4
 * Flying
 * Whenever this creature deals combat damage to a player, it deals 1 damage to each creature
 * that player controls.
 *
 * The damaged player is the [Player.TriggeringPlayer] of the combat-damage trigger; the sweep
 * is the group-damage pattern over the creatures that player controls (cf. Balefire Dragon).
 */
val ShockmawDragon = card("Shockmaw Dragon") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever this creature deals combat damage to a player, it deals 1 damage " +
        "to each creature that player controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Group.dealDamageToAll(
            1,
            GroupFilter(
                GameObjectFilter.Creature.targetPlayerControls(
                    EffectTarget.PlayerRef(Player.TriggeringPlayer)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Alejandro Mirabal"
        flavorText = "Dragons of the Kolaghan brood are always on the move. They are the driving force " +
            "behind the Mardu's nomadic way of life."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4810919-b379-40db-a68b-a769dd3ee32d.jpg?1783938684"
    }
}
