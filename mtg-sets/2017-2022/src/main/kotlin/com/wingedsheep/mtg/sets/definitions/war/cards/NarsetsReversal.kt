package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Narset's Reversal — War of the Spark #62 (canonical printing)
 * {U}{U} · Instant
 *
 * Copy target instant or sorcery spell, then return it to its owner's hand. You may choose new
 * targets for the copy.
 *
 * Order is load-bearing: the copy is cloned off the original while it is still on the stack (and
 * [Effects.CopyTargetSpell] carries the "choose new targets" prompt itself), and only then is the
 * original bounced. The bounce is not a counter, so it works on a spell that can't be countered.
 */
val NarsetsReversal = card("Narset's Reversal") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Copy target instant or sorcery spell, then return it to its owner's hand. " +
        "You may choose new targets for the copy."

    spell {
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(spell) then Effects.ReturnSpellOrPermanentToOwnersHand(spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Viktor Titov"
        flavorText = "\"In the disarray of battle, an enemy's attack may be your most convenient weapon.\"\n—Narset"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63754036-d51e-47bb-925b-564d9dc922ff.jpg?1783933459"
        ruling("2019-05-03", "Narset's Reversal can copy any instant or sorcery spell, not just one with targets.")
        ruling("2019-05-03", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2019-05-03", "If you copy a spell, you control the copy. Narset's Reversal and the copy resolve before the original spell would have resolved.")
        ruling("2019-05-03", "If a spell is returned to its owner's hand, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works against a spell that can't be countered.")
        ruling("2019-05-03", "If a copy of a spell is returned to its owner's hand, it's moved there, then it will cease to exist as a state-based action.")
    }
}
