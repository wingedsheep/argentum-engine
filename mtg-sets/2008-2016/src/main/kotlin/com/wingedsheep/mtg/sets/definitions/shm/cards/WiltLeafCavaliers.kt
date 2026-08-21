package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wilt-Leaf Cavaliers
 * {G/W}{G/W}{G/W}
 * Creature — Elf Knight
 * 3 / 4
 *
 * Vigilance
 *
 * - Keyword-only creature: [Keyword.VIGILANCE] is engine-live, so no scripted ability is needed.
 */
val WiltLeafCavaliers = card("Wilt-Leaf Cavaliers") {
    manaCost = "{G/W}{G/W}{G/W}"
    typeLine = "Creature — Elf Knight"
    power = 3
    toughness = 4
    oracleText = "Vigilance"

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Steve Prescott"
        flavorText = "Every elf in Shadowmoor is charged from birth with a terrible duty: to strike back against the ugliness and darkness, even though they are all around and seemingly without end."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8ffe320-c88a-4eb7-a091-e128e6c1f37c.jpg?1783942714"
    }
}
