package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deadbridge Shaman
 * {2}{B}
 * Creature — Elf Shaman
 * 3/1
 * When this creature dies, target opponent discards a card.
 */
val DeadbridgeShaman = card("Deadbridge Shaman") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Shaman"
    power = 3
    toughness = 1
    oracleText = "When this creature dies, target opponent discards a card."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(1, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Nils Hamm"
        flavorText = "Ending her life won't end the infestation."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b62a4c2-e66d-4fc3-8703-a90b84f567bf.jpg?1783938343"
    }
}
