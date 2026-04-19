package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Protective Response
 * {2}{W}
 * Instant
 *
 * Convoke (Your creatures can help cast this spell. Each creature you tap while
 * casting this spell pays for {1} or one mana of that creature's color.)
 * Destroy target attacking or blocking creature.
 */
val ProtectiveResponse = card("Protective Response") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Destroy target attacking or blocking creature."

    keywords(Keyword.CONVOKE)

    spell {
        val creature = target("creature", TargetCreature(filter = TargetFilter.AttackingOrBlockingCreature))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Gustavo Pelissari"
        flavorText = "With no connection to the thoughtweft, kithkin nomads rely on guts, instinct, and each other."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/113975e1-7712-4760-96ad-405f8b4e41e3.jpg?1767956959"
    }
}
