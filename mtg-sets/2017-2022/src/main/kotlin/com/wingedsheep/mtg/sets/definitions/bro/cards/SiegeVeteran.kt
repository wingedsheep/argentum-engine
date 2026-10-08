package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Siege Veteran
 * {2}{W}
 * Creature — Human Soldier
 * 2/2
 * At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.
 * Whenever another nontoken Soldier you control dies, create a 1/1 colorless Soldier artifact creature token.
 *
 * "Soldier" is a bare tribal noun — a *permanent* filter with the subtype, not `Creature`.
 */
val SiegeVeteran = card("Siege Veteran") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.\n" +
        "Whenever another nontoken Soldier you control dies, create a 1/1 colorless Soldier artifact creature token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Soldier").youControl().nontoken()).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Darren Tan"
        flavorText = "\"Steady, recruit. We fight in the light of justice. We cast no shadow.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f57e2a32-6e52-4f6e-96ec-55f5b7ba77e0.jpg"
    }
}
