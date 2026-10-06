package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Power Surge
 * {R}{R}
 * Enchantment
 * At the beginning of each player's upkeep, this enchantment deals X damage to that player, where
 * X is the number of untapped lands they controlled at the beginning of this turn.
 *
 * X is the untap-step snapshot, not a live count: by the upkeep the player's lands have already
 * untapped. Recorded every turn, so the card knows the number even if it entered after the turn
 * began (Scryfall ruling).
 */
val PowerSurge = card("Power Surge") {
    manaCost = "{R}{R}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, this enchantment deals X damage to that player, " +
        "where X is the number of untapped lands they controlled at the beginning of this turn."
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            DynamicAmounts.untappedLandsAtTurnStart(Player.TriggeringPlayer),
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62858604-ca5a-4f69-a045-a7515ebfabf2.jpg?1783948682"
    }
}
