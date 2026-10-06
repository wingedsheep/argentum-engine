package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Manabarbs — Limited Edition Alpha #163
 * {3}{R} · Enchantment
 *
 * Whenever a player taps a land for mana, this enchantment deals 1 damage to that player.
 *
 * `Triggers.anyPlayer.tapsLandForMana()` watches every player's land taps; the trigger context binds
 * the tapping player as [Player.TriggeringPlayer]. It deals damage rather than adding mana, so it is
 * not a mana ability — it uses the stack and can be responded to, one trigger per land tapped.
 */
val Manabarbs = card("Manabarbs") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a player taps a land for mana, this enchantment deals 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.anyPlayer.tapsLandForMana()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6121f72f-680f-4bb4-ae4d-37ee4ebed4d8.jpg?1783948683"
        ruling("2009-10-01", "This ability is not a mana ability. It goes on the stack and can be responded to.")
        ruling("2009-10-01", "The ability will trigger each time a land is tapped for mana. Each ability is separate.")
        ruling(
            "2009-10-01",
            "If any lands are tapped for mana while a player is casting a spell or activating an ability, " +
                "Manabarbs's ability will trigger that many times and wait. When the player finishes casting " +
                "that spell or activating that ability, it's put on the stack, then Manabarbs's triggered " +
                "abilities are put on the stack on top of it. The Manabarbs abilities will resolve first."
        )
    }
}
