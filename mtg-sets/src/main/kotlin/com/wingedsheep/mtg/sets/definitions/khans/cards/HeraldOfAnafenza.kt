package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Herald of Anafenza
 * {W}
 * Creature — Human Soldier
 * 1/2
 * Outlast {2}{W} ({2}{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 * Whenever you activate Herald of Anafenza's outlast ability, create a 1/1 white Warrior creature token.
 *
 * Ruling (2014-09-20): The Warrior token is created before the +1/+1 counter is put on Herald of Anafenza.
 * Modeled as a single activated ability with composite effect: create token first, then add counter.
 */
val HeraldOfAnafenza = card("Herald of Anafenza") {
    manaCost = "{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Outlast {2}{W} ({2}{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)\nWhenever you activate Herald of Anafenza's outlast ability, create a 1/1 white Warrior creature token."

    // Outlast {2}{W} — combined with the triggered token creation.
    // Per the ruling, the token is created before the +1/+1 counter is placed.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/f/4/f46bcc76-181c-4e06-aa04-590a3e651dc7.jpg?1562640133"
        ).then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8bf6ed4-48f3-419f-bafd-d1ee4d798482.jpg?1562795324"
    }
}
