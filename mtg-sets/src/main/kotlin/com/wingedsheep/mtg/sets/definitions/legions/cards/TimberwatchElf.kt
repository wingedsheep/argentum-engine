package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Timberwatch Elf
 * {2}{G}
 * Creature — Elf
 * 1/2
 * {T}: Target creature gets +X/+X until end of turn, where X is the number of Elves on the battlefield.
 */
val TimberwatchElf = card("Timberwatch Elf") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 2
    oracleText = "{T}: Target creature gets +X/+X until end of turn, where X is the number of Elves on the battlefield."

    activatedAbility {
        cost = AbilityCost.Tap
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        val elfCount = DynamicAmounts.creaturesWithSubtype(Subtype("Elf"))
        effect = Effects.ModifyStats(
            power = elfCount,
            toughness = elfCount,
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Dave Dorman"
        flavorText = "\"Even through the Mirari's voice, the elves still hear the call of their kinship.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/045ae4ec-07f2-4098-a2d9-4bfcbd0273b2.jpg?1562895850"
    }
}
