package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Burn Trail
 * {3}{R}
 * Sorcery
 *
 * Burn Trail deals 3 damage to any target.
 * Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)
 *
 * - "any target" is the single [Targets.Any] requirement (creature, player, battle or planeswalker),
 *   not a creature-or-player union — the modern Oracle wording maps onto one `AnyTarget` slot.
 * - Conspire is declared as [KeywordAbility.Conspire] only; `CardBuilder` derives
 *   `Keyword.CONSPIRE` into `keywords`, so a separate `keywords(...)` line would be redundant.
 * - The damage source is left implicit (the spell itself), so no `damageSource` override is written.
 */
val BurnTrail = card("Burn Trail") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"
    oracleText = "Burn Trail deals 3 damage to any target.\n" +
        "Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)"

    keywordAbility(KeywordAbility.Conspire)

    spell {
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f01f9a0-f1d0-4241-a270-df4ed673d1fd.jpg?1783942750"
    }
}
