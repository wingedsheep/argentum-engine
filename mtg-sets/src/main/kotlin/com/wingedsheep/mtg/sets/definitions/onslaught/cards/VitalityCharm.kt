package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.RegenerateEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Vitality Charm
 * {G}
 * Instant
 * Choose one —
 * • Create a 1/1 green Insect creature token.
 * • Target creature gets +1/+1 and gains trample until end of turn.
 * • Regenerate target Beast.
 */
val VitalityCharm = card("Vitality Charm") {
    manaCost = "{G}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Create a 1/1 green Insect creature token.\n• Target creature gets +1/+1 and gains trample until end of turn.\n• Regenerate target Beast."

    spell {
        modal(chooseCount = 1) {
            mode("Create a 1/1 green Insect creature token") {
                effect = CreateTokenEffect(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Insect"),
                    imageUri = "https://cards.scryfall.io/normal/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793"
                )
            }
            mode("Target creature gets +1/+1 and gains trample until end of turn") {
                target = TargetCreature()
                effect = Effects.ModifyStats(1, 1, EffectTarget.ContextTarget(0))
                    .then(Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0)))
            }
            mode("Regenerate target Beast") {
                target = TargetCreature(filter = TargetFilter.Creature.withSubtype("Beast"))
                effect = RegenerateEffect(EffectTarget.ContextTarget(0))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "296"
        artist = "David Martin"
        flavorText = "\"We are nothing without the spirits of the wild.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1abae21-ed8f-4e21-b227-f721b840c11f.jpg?1562940084"
    }
}
