package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Burning Earth — Magic 2014 #130
 * {3}{R} · Enchantment
 *
 * Whenever a player taps a nonbasic land for mana, this enchantment deals 1 damage to that player.
 *
 * Manabarbs narrowed to nonbasic lands: `Triggers.anyPlayer.tapsLandForMana(GameObjectFilter.NonbasicLand)`
 * watches every player's nonbasic-land taps and binds the tapping player as [Player.TriggeringPlayer].
 * Not a mana ability — it uses the stack, one trigger per nonbasic land tapped.
 */
val BurningEarth = card("Burning Earth") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a player taps a nonbasic land for mana, this enchantment deals 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.anyPlayer.tapsLandForMana(GameObjectFilter.NonbasicLand)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "rk post"
        flavorText = "\"Your world is formed from the same power that wraps my burning hand around your throat.\"\n—Shrazik, lavamancer"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1df3a7c9-5c8d-438c-a5ad-3c9754c6ea5d.jpg?1783939917"
        ruling("2013-07-01", "The ability will trigger each time a nonbasic land is tapped for mana. Each of these abilities goes on the stack and resolves separately.")
        ruling(
            "2013-07-01",
            "If any nonbasic lands are tapped for mana while a player is casting a spell or activating an ability, " +
                "Burning Earth's ability will trigger that many times and wait. When the player finishes casting that " +
                "spell or activating that ability, Burning Earth's triggered abilities are put on the stack on top of it. " +
                "Burning Earth's abilities will resolve first."
        )
        ruling(
            "2013-07-01",
            "On the other hand, a player can tap nonbasic lands for mana, put the Burning Earth triggered abilities on " +
                "the stack, and then respond to those abilities by casting an instant spell or activating an ability " +
                "using that mana. In that case, the instant spell or activated ability will resolve first."
        )
    }
}
