package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elderfang Ritualist
 * {2}{B}
 * Creature — Elf Cleric
 * 3/1
 *
 * When this creature dies, return another target Elf card from your graveyard to your hand.
 */
val ElderfangRitualist = card("Elderfang Ritualist") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Cleric"
    power = 3
    toughness = 1
    oracleText = "When this creature dies, return another target Elf card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val elfCard = target(
            TargetFilter(
                GameObjectFilter.Any.withSubtype("Elf").ownedByYou(),
                zone = Zone.GRAVEYARD,
                excludeSelf = true
            ),
        )
        effect = Effects.ReturnToHand(elfCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "385"
        artist = "Wisnu Tan"
        flavorText = "Her fingers stiffened as the shrouded corpse stirred. Her final, ragged breath was one of triumph. " +
            "\"Return, my chosen, and grant our people victory!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57348f93-8764-4b1a-b02e-4e7881337fa2.jpg?1783928118"
    }
}
