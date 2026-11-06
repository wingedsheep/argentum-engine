package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trespasser's Curse — Amonkhet #112 (canonical printing)
 * {1}{B} · Enchantment — Aura Curse
 *
 * Enchant player
 * Whenever a creature enchanted player controls enters, that player loses 1 life and you gain 1 life.
 *
 * The trigger's controller scope is the Curse's enchanted player, not its controller:
 * `controlledByEnchantedPlayer()` resolves the player off the trigger's source Aura.
 */
val TrespassersCurse = card("Trespasser's Curse") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "Whenever a creature enchanted player controls enters, that player loses 1 life and you gain 1 life."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.controlledByEnchantedPlayer()).enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EnchantedPlayer)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "David Palumbo"
        flavorText = "Even in the relative safety of the city, there are places best left undisturbed."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6ef4c59-ab84-4d84-b795-015b21ae3fe0.jpg?1783936496"
    }
}
