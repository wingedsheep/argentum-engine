package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val KnightOfDawnsLight = card("Knight of Dawn's Light") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "If you would gain life, you gain that much life plus 1 instead.\n" +
        "{1}{W}: This creature gets +1/+1 until end of turn."

    keywords(Keyword.FIRST_STRIKE)
    replacementEffect(
        ModifyLifeGain(
            multiplier = 1,
            modifier = 1,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
        )
    )
    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Billy Christian"
        flavorText = "\"Phyrexians destroyed my village, but they cannot break my hope.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7c7a54e-4df6-4745-8bae-aeefda934d82.jpg?1783921364"
    }
}
