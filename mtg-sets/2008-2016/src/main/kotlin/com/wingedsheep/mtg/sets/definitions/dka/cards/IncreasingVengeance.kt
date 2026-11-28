package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Increasing Vengeance — Dark Ascension #95 (canonical printing)
 * {R}{R} · Instant
 *
 * Copy target instant or sorcery spell you control. If this spell was cast from a graveyard, copy
 * that spell twice instead. You may choose new targets for the copies.
 * Flashback {3}{R}{R}
 *
 * The cycle's Increasing Confusion / Ambition shape: a resolution-time cast-from-graveyard check picks
 * between one copy and two, each copy retargetable on its own (CR 707.10c).
 */
val IncreasingVengeance = card("Increasing Vengeance") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Copy target instant or sorcery spell you control. If this spell was cast from a " +
        "graveyard, copy that spell twice instead. You may choose new targets for the copies.\n" +
        "Flashback {3}{R}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.If(
            condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
            then = Effects.CopyTargetSpell(target = spell, copies = DynamicAmounts.fixed(2)),
            otherwise = Effects.CopyTargetSpell(target = spell),
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{R}{R}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Anthony Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d13afe4a-4a3d-42ae-ac0a-b789364c7e7e.jpg?1783940816"
    }
}
