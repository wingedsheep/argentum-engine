package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bold Biochemist — Marvel Super Heroes #48 (common)
 * {1}{U} · Creature — Human Scientist · 1/3
 *
 * Power-up — {5}{U}: Put a +1/+1 counter on this creature and draw two cards. (Activate each
 * power-up ability only once. Reduce the cost by its mana cost if it entered this turn.)
 *
 * The first power-up in the cycle that does something other than grow: the counter and the draw
 * are one [Effects.Composite], resolving in printed order. `{5}{U}` − `{1}{U}` = `{4}`, so the
 * turn it lands this is a two-mana 1/3 plus a four-mana Divination that leaves a 2/4 behind.
 */
val BoldBiochemist = card("Bold Biochemist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Scientist"
    oracleText = "Power-up — {5}{U}: Put a +1/+1 counter on this creature and draw two cards. " +
        "(Activate each power-up ability only once. Reduce the cost by its mana cost if it " +
        "entered this turn.)"
    power = 1
    toughness = 3

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{U}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.DrawCards(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Marco Turini"
        flavorText = "\"Dr. Pym always told me I was meant for big things. Turns out, he was right.\"\n—Dr. Bill Foster"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10644127-3da0-484e-9afc-de26d9c34390.jpg?1783902964"
    }
}
