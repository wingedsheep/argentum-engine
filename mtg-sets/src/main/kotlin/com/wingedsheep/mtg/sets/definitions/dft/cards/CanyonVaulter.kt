package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Canyon Vaulter — Aetherdrift #8. */
val CanyonVaulter = card("Canyon Vaulter") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Pilot"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature saddles a Mount or crews a Vehicle during your main " +
        "phase, that Mount or Vehicle gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.or(Triggers.Saddles, Triggers.Crews)
        triggerCondition = Conditions.IsYourMainPhase
        effect = Effects.GrantKeyword(
            Keyword.FLYING,
            target = EffectTarget.TriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "David Astruga"
        flavorText = "Some seek thrills in watching the show. Some seek thrills in being the show."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc0b15da-a45c-42f5-aafc-20ad9e38bf24.jpg?1783907921"
    }
}
