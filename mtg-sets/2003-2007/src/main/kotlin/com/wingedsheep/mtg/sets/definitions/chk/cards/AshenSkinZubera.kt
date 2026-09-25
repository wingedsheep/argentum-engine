package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ashen-Skin Zubera
 * {1}{B}
 * Creature — Zubera Spirit
 * 1/2
 * When this creature dies, target opponent discards a card for each Zubera that died this turn.
 *
 * The count is game-wide and includes this Zubera itself.
 */
val AshenSkinZubera = card("Ashen-Skin Zubera") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zubera Spirit"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, target opponent discards a card for each Zubera that died this turn."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(Subtype("Zubera")), opponent)
        description = "When this creature dies, target opponent discards a card for each Zubera that died this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Wayne Reynolds"
        flavorText = "When the Honden of Night's Reach began to crumble, its attendants swarmed Kamigawa to haunt mortal dreams."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40cee88d-849d-4715-ad88-7cdb855a3088.jpg?1783944317"
    }
}
