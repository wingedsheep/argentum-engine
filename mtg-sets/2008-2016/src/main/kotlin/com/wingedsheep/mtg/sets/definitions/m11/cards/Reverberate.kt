package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reverberate
 * {R}{R}
 * Instant
 * Copy target instant or sorcery spell. You may choose new targets for the copy.
 */
val Reverberate = card("Reverberate") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Copy target instant or sorcery spell. You may choose new targets for the copy."
    spell {
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(spell)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "jD"
        flavorText = "\"I'm not indecisive. I just don't like to limit my options.\"\n—Maxti, pyromancer"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd435013-0ab9-42f4-985c-66ea2b3760e9.jpg?1783941802"
        ruling("2010-08-15", "Reverberate can target (and copy) any instant or sorcery spell, not just one with targets. It doesn't matter who controls it.")
        ruling("2010-08-15", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2010-08-15", "If the spell Reverberate copies is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. You can't choose a different one.")
        ruling("2010-08-15", "If the spell Reverberate copies has an X whose value was determined as it was cast (like Earthquake does), the copy has the same value of X.")
        ruling("2010-08-15", "You can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
    }
}
