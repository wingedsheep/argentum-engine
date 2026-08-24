package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dwarven Lieutenant
 * {R}{R}
 * Creature — Dwarf Soldier
 * 1/2
 * {1}{R}: Target Dwarf creature gets +1/+0 until end of turn.
 *
 * The red counterpart of [IcatianLieutenant].
 */
val DwarvenLieutenant = card("Dwarven Lieutenant") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Soldier"
    oracleText = "{1}{R}: Target Dwarf creature gets +1/+0 until end of turn."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        val t = target(
            "target Dwarf creature",
            TargetCreature(filter = TargetFilter.Creature.withSubtype(Subtype.DWARF))
        )
        effect = Effects.ModifyStats(1, 0, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Jeff A. Menges"
        flavorText = "\"Dwarven officers were tireless in battle, moving up and down the lines to rally their troops and boost morale.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea9a38b1-4676-425a-b40d-4fb478966024.jpg?1783947895"
    }
}
