package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spiritual Sanctuary
 * {2}{W}{W}
 * Enchantment
 * At the beginning of each player's upkeep, if that player controls a Plains, they gain 1 life.
 *
 * "That player" is the player whose upkeep it is — bound as the triggering player by the step
 * trigger; the intervening-if reads their battlefield on trigger and again on resolution.
 */
val SpiritualSanctuary = card("Spiritual Sanctuary") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, if that player controls a Plains, they gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Exists(
            Player.TriggeringPlayer,
            Zone.BATTLEFIELD,
            GameObjectFilter.Land.withSubtype("Plains"),
        )
        effect = Effects.GainLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/654dd1e0-a91d-44ee-af20-c025bf360c3f.jpg?1783948080"
    }
}
