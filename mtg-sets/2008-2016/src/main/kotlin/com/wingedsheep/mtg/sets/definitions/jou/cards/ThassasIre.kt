package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thassa's Ire
 * {U}
 * Enchantment
 *
 * {3}{U}: You may tap or untap target creature.
 */
val ThassasIre = card("Thassa's Ire") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "{3}{U}: You may tap or untap target creature."

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(creature), "Tap that creature"),
                    Mode.noTarget(Effects.Untap(creature), "Untap that creature"),
                ),
                chooseCount = 1,
                countsAsModalSpell = false,
            )
        )
        description = "{3}{U}: You may tap or untap target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Chris Rahn"
        flavorText = "The sailor had never seen a god before. Now she had gazed upon Thassa. Shortly she would see Athreos, and then she would meet Erebos."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cc6245f-c60f-4674-8c6d-24fe3b78e00e.jpg?1783939441"
    }
}
