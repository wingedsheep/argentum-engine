package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Banneret
 * {R}
 * Creature — Goblin Soldier
 * 1/1
 *
 * Mentor
 * {1}{R}: This creature gets +2/+0 until end of turn.
 */
val GoblinBanneret = card("Goblin Banneret") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin Soldier"
    power = 1
    toughness = 1
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\n" +
        "{1}{R}: This creature gets +2/+0 until end of turn."

    mentor()

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Viktor Titov"
        flavorText = "The Boros banner stands tall even if its bearer doesn't."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90b3b56c-5236-4319-b17e-5f71143a7906.jpg?1783934162"
    }
}
