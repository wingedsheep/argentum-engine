package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boros Challenger
 * {R}{W}
 * Creature — Human Soldier
 * 2/3
 *
 * Mentor
 * {2}{R}{W}: This creature gets +1/+1 until end of turn.
 */
val BorosChallenger = card("Boros Challenger") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\n" +
        "{2}{R}{W}: This creature gets +1/+1 until end of turn."

    mentor()

    activatedAbility {
        cost = Costs.Mana("{2}{R}{W}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "{2}{R}{W}: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Gabor Szikszai"
        flavorText = "\"Send your champion. I could use a light workout.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/545f3a30-7984-4046-8a14-51bc9cbc3fe0.jpg?1783934141"
    }
}
