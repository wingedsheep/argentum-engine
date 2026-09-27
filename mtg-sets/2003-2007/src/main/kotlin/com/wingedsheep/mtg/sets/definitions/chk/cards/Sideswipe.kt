package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sideswipe
 * {1}{R}
 * Instant
 * You may change any targets of target Arcane spell.
 *
 * "Any targets" is the all-slots retarget, so this is `ChangeTriggeringObjectTargets` pointed at the
 * targeted spell (the Wild Ricochet shape without the copy) rather than `ChangeTarget`, which swaps a
 * single target and no-ops on a spell with more than one. The chooser may change all, some or none
 * of them, slot by slot. The target is any Arcane spell, whoever controls it.
 */
val Sideswipe = card("Sideswipe") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You may change any targets of target Arcane spell."

    spell {
        val spell = target(TargetFilter(GameObjectFilter.Any.withSubtype("Arcane"), zone = Zone.STACK))
        effect = Effects.ChangeTriggeringObjectTargets(spell = spell)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Ron Spears"
        flavorText = "Hisoka's wizards struggled for years to master the art of redirection that came so " +
            "naturally to the shamans of Ganzan Pass."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bd611b7-7bd3-4a76-bd69-34a7235965ae.jpg?1783944296"
    }
}
