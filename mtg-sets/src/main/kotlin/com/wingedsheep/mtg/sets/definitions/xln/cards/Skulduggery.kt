package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Skulduggery
 * {B}
 * Instant
 *
 * Until end of turn, target creature you control gets +1/+1 and target creature an
 * opponent controls gets -1/-1.
 *
 * Two distinct targets: one creature you control (buffed) and one creature an opponent
 * controls (debuffed). Each is chosen and validated independently.
 */
val Skulduggery = card("Skulduggery") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control gets +1/+1 and target creature an opponent controls gets -1/-1."

    spell {
        val yours = target("yours", Targets.CreatureYouControl)
        val theirs = target("theirs", Targets.CreatureOpponentControls)
        effect = Effects.Composite(
            listOf(
                Effects.ModifyStats(1, 1, yours),
                Effects.ModifyStats(-1, -1, theirs)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Deruchenko Alexander"
        flavorText = "\"They're so much more willing to parley once they're hanging from a boom by the ankle!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba30343b-1637-490f-810e-d614219789e3.jpg?1575629239"
    }
}
