package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jaded Analyst — Murders at Karlov Manor #62
 * {1}{U} · Creature — Human Detective · 3/2
 *
 * Defender
 * Whenever you draw your second card each turn, this creature loses defender and gains vigilance
 * until end of turn.
 *
 * A 3/2 for two that has to be unlocked: the second draw each turn turns it into an attacker that
 * doesn't even tap. Both halves are [Duration.EndOfTurn] — the printed defender comes back at
 * cleanup, so the Analyst blocks again on the opponent's turn and needs unlocking afresh each turn.
 *
 * [Triggers.NthCardDrawn]`(2)` (CR 121.2) reads the per-turn draw counter rather than counting
 * draw *events*, so a single "draw two cards" spell crosses the threshold once and fires the
 * trigger once, and a card put into hand without the word "draw" (CR 121.5) doesn't advance it at
 * all. The turn's first draw — including the draw step's — counts toward the two, so on your own
 * turn one extra draw is enough.
 *
 * Losing defender is [Effects.RemoveKeyword], not a stat or type change: the Analyst stays a
 * Human Detective (relevant for the set's Detective payoffs) and simply sheds the attack
 * restriction. Vigilance is granted in the same resolution; the two are separate keyword
 * modifications in Layer 6 and don't depend on each other.
 */
val JadedAnalyst = card("Jaded Analyst") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Detective"
    power = 3
    toughness = 2
    oracleText = "Defender\n" +
        "Whenever you draw your second card each turn, this creature loses defender and gains " +
        "vigilance until end of turn."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.Composite(
            listOf(
                Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.EndOfTurn),
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self, Duration.EndOfTurn)
            )
        )
        description = "Whenever you draw your second card each turn, this creature loses defender " +
            "and gains vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Borja Pindado"
        flavorText = "\"Oh, good, another case with too many victims and not enough clues.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2807dcfb-d99c-483b-835f-2606eae4bd30.jpg?1783912907"
    }
}
