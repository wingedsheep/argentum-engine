package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trained Cheetah
 * {2}{G}
 * Creature — Cat
 * 2/2
 * Whenever this creature becomes blocked, it gets +1/+1 until end of turn.
 */
val TrainedCheetah = card("Trained Cheetah") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature becomes blocked, it gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Fang Yue"
        flavorText = "\"[King Mulu's cheetahs] came riding on the winds, charging, with fangs bared and claws flexed.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab242eab-5cab-41a0-bcf8-93a6919e4558.jpg"
    }
}
