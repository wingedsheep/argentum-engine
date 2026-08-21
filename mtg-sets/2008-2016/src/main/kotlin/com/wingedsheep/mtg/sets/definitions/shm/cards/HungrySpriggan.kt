package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hungry Spriggan
 * {2}{G}
 * Creature — Goblin Warrior
 * 1 / 1
 *
 * Trample
 * Whenever this creature attacks, it gets +3/+3 until end of turn.
 *
 * - The pump is a *triggered* ability, not a static: it goes on the stack when attackers are
 *   declared and can be responded to, and it only fires when this creature itself attacks
 *   ([Triggers.Attacks] is the SELF-bound attack trigger).
 * - [EffectTarget.Self] rather than a target — "it" is the source, so nothing is targeted and the
 *   bonus still applies if the Spriggan is later removed from combat.
 * - `Effects.ModifyStats` defaults to `Duration.EndOfTurn`, which is exactly "until end of turn".
 */
val HungrySpriggan = card("Hungry Spriggan") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Trample\n" +
        "Whenever this creature attacks, it gets +3/+3 until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Drew Tucker"
        flavorText = "If a spriggan's eyes are larger than its stomach, it has ways to remedy the situation."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8be81b15-eb12-452b-8fdd-bde64807f422.jpg?1783942742"
    }
}
