package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reconnaissance
 * {W}
 * Enchantment
 * {0}: Remove target attacking creature you control from combat and untap it. (If you activate
 * during end of combat, the creature will untap after it deals combat damage.)
 *
 * `RemoveFromCombat` then `Untap` on one target. Removing an attacker from combat doesn't undo
 * anything that triggered on its attack declaration (2004-10-04 ruling).
 */
val Reconnaissance = card("Reconnaissance") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{0}: Remove target attacking creature you control from combat and untap it. " +
        "(If you activate during end of combat, the creature will untap after it deals combat damage.)"

    activatedAbility {
        cost = Costs.Mana("{0}")
        val attacker = target(TargetFilter(GameObjectFilter.Creature.attacking().youControl()))
        effect = Effects.RemoveFromCombat(attacker) then Effects.Untap(attacker)
        description = "Remove target attacking creature you control from combat and untap it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Val Mayerik"
        flavorText = "War favors the informed."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a16012d8-703c-4385-8769-13e3caba3fc6.jpg?1783946529"
        ruling("2004-10-04", "Does not undo any effects which triggered on declaration of attackers or blockers.")
    }
}
