package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kodama of the South Tree
 * {2}{G}{G}
 * Legendary Creature — Spirit
 * 4/4
 * Whenever you cast a Spirit or Arcane spell, each other creature you control gets +1/+1 and
 * gains trample until end of turn.
 *
 * Kami of the Hunt's Spirit-or-Arcane cast trigger feeding a one-group pump-and-grant; "each
 * other" excludes the Kodama itself.
 */
val KodamaOfTheSouthTree = card("Kodama of the South Tree") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spirit"
    oracleText = "Whenever you cast a Spirit or Arcane spell, each other creature you control gets " +
        "+1/+1 and gains trample until end of turn."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnySubtype("Spirit", "Arcane"))
        effect = Patterns.Group.pumpAndGrantToAll(
            1, 1, Keyword.TRAMPLE, GroupFilter.OtherCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Ron Spears"
        flavorText = "\"The monks of the South Tree had always reveled beneath their kodama's friendly gaze. During the Kami War, this gaze became fierce and full of hate.\"\n—\"Poem of the Five Trees\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/120c05f1-5ec3-4a0f-8628-6919e393104a.jpg?1783944287"
    }
}
