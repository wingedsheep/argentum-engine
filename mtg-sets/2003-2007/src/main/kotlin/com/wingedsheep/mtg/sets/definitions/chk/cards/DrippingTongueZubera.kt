package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dripping-Tongue Zubera
 * {1}{G}
 * Creature — Zubera Spirit
 * 1/2
 * When this creature dies, create a 1/1 colorless Spirit creature token for each Zubera that died
 * this turn.
 *
 * CHK printed no tokens; the Spirit art is the same Eternal Masters token Honden of Life's Web uses.
 */
val DrippingTongueZubera = card("Dripping-Tongue Zubera") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Zubera Spirit"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, create a 1/1 colorless Spirit creature token for each Zubera that died this turn."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.creaturesWithSubtypeDiedThisTurn(Subtype("Zubera")),
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Spirit"),
            imageUri = "https://cards.scryfall.io/normal/front/0/8/082c3bad-3fea-4c3f-8263-4b16139bb32a.jpg?1783937532"
        )
        description = "When this creature dies, create a 1/1 colorless Spirit creature token for each Zubera that died this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Tsutomu Kawade"
        flavorText = "When the Honden of Life's Web was destroyed, its attendants swarmed Kamigawa to ensure mortal defeat."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e752b762-38b7-463c-aa09-37fecfe71a53.jpg?1783944291"
    }
}
