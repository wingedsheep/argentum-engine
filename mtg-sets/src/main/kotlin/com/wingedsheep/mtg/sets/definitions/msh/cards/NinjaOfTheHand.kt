package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ninja of the Hand — Marvel Super Heroes #108 (common)
 * {2}{B} · Creature — Human Ninja Villain · 2/2
 *
 * Deathtouch
 * Power-up — {4}{B}: Each opponent discards a card. Put a +1/+1 counter on this creature.
 * (Activate each power-up ability only once. Reduce the cost by its mana cost if it entered
 * this turn.)
 *
 * Printed order matters and is preserved: the discard resolves before the counter. Each half is a
 * plain facade — [Effects.EachOpponentDiscards] is symmetric across every opponent (so it scales
 * in multiplayer without any per-card work) and is not targeted, so a hexproof opponent still
 * discards. `{4}{B}` − `{2}{B}` = `{2}`.
 */
val NinjaOfTheHand = card("Ninja of the Hand") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Ninja Villain"
    oracleText = "Deathtouch\n" +
        "Power-up — {4}{B}: Each opponent discards a card. Put a +1/+1 counter on this creature. " +
        "(Activate each power-up ability only once. Reduce the cost by its mana cost if it " +
        "entered this turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{4}{B}")
        effect = Effects.Composite(
            Effects.EachOpponentDiscards(1),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "InHyuk Lee"
        flavorText = "\"They are ruthless killers. I was one of them.\"\n—Elektra Natchios"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b8116d8-2cc6-449b-a8b4-8a5166553497.jpg?1783902939"
    }
}
