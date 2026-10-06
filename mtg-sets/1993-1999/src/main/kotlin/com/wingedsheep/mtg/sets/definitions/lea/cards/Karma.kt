package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Karma — Limited Edition Alpha #26
 * {2}{W}{W} · Enchantment
 *
 * At the beginning of each player's upkeep, this enchantment deals damage to that player equal to
 * the number of Swamps they control.
 *
 * Each-player upkeep trigger (Furnace Punisher's shape); [Player.TriggeringPlayer] is the player
 * whose upkeep it is, and the Swamp count is read off *their* battlefield (controller, projected)
 * when the ability resolves, per the 2004-10-04 ruling.
 */
val Karma = card("Karma") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, this enchantment deals damage to that " +
        "player equal to the number of Swamps they control."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(
                Player.TriggeringPlayer,
                GameObjectFilter.Land.withSubtype(Subtype.SWAMP),
            ).count(),
            EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of each player's upkeep, this enchantment deals damage to " +
            "that player equal to the number of Swamps they control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f30ad61-fcb7-4d55-ba86-94de1bf545e4.jpg?1783948713"
        ruling(
            "2004-10-04",
            "Amount of damage is determined when the ability resolves and not when it is placed on the stack."
        )
    }
}
