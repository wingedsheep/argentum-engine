package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Barkshell Blessing
 * {G/W}
 * Instant
 *
 * Target creature gets +2/+2 until end of turn.
 * Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)
 *
 * - "until end of turn" is [Effects.ModifyStats]'s default duration, so no duration is written.
 * - Conspire is declared as [KeywordAbility.Conspire] only; `CardBuilder` derives
 *   `Keyword.CONSPIRE` into `keywords`, so a separate `keywords(...)` line would be redundant.
 * - {G/W} is a hybrid symbol: the card is both green and white, so either a green or a white pair
 *   of untapped creatures can pay the conspire cost.
 */
val BarkshellBlessing = card("Barkshell Blessing") {
    manaCost = "{G/W}"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn.\n" +
        "Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose a new target for the copy.)"

    keywordAbility(KeywordAbility.Conspire)

    spell {
        val creature = target("target", Targets.Creature)
        effect = Effects.ModifyStats(2, 2, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd273ef2-4aed-4c7e-8c97-fe8b1af9ce69.jpg?1783942718"
    }
}
