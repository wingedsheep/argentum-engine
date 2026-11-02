package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sunder Shaman
 * {R}{R}{G}{G}
 * Creature — Giant Shaman
 * 5/5
 *
 * This creature can't be blocked by more than one creature.
 * Whenever this creature deals combat damage to a player, destroy target artifact or
 * enchantment that player controls.
 */
val SunderShaman = card("Sunder Shaman") {
    manaCost = "{R}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Giant Shaman"
    oracleText = "This creature can't be blocked by more than one creature.\n" +
        "Whenever this creature deals combat damage to a player, destroy target artifact or enchantment that player controls."
    power = 5
    toughness = 5

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    // "That player controls" is the player just damaged, not "any opponent".
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment.controlledByTriggeringPlayer()))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Jason Rainville"
        flavorText = "\"You take our wilds, we take your city.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bb6a8bb-9eec-4f9b-a5a8-604631e4b823.jpg?1783933634"
    }
}
