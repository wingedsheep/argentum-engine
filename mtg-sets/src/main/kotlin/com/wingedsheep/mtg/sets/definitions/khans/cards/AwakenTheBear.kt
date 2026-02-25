package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Awaken the Bear
 * {2}{G}
 * Instant
 * Target creature gets +3/+3 and gains trample until end of turn.
 */
val AwakenTheBear = card("Awaken the Bear") {
    manaCost = "{2}{G}"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 and gains trample until end of turn."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.ModifyStats(3, 3, t)
            .then(Effects.GrantKeyword(Keyword.TRAMPLE, t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Svetlin Velinov"
        flavorText = "When Temur warriors enter the battle trance known as \"awakening the bear,\" they lose all sense of enemy or friend, seeing only threats to the wilderness."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/803a6ac7-9327-4c2f-b023-93f5f65f83b8.jpg?1562789301"
    }
}
