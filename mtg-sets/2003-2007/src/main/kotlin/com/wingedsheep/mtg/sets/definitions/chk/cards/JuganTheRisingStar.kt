package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jugan, the Rising Star
 * {3}{G}{G}{G}
 * Legendary Creature — Dragon Spirit
 * 5/5
 * Flying
 * When Jugan dies, you may distribute five +1/+1 counters among any number of target creatures.
 *
 * "Any number of target creatures" is a zero-to-five target range — each chosen target gets at
 * least one counter (CR 601.2d), so five counters cap it at five — and choosing no targets is the
 * "may" declined. The split is announced as the trigger goes on the stack (CR 603.3d).
 */
val JuganTheRisingStar = card("Jugan, the Rising Star") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Dragon Spirit"
    oracleText = "Flying\nWhen Jugan dies, you may distribute five +1/+1 counters among any number " +
        "of target creatures."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        targets(TargetFilter.Creature, count = 5, minCount = 0)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 5)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Shishizaru"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c77814d-c594-45a8-845f-79f608b8cde7.jpg?1783944288"
    }
}
