package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Display of Power
 * {1}{R}{R}
 * Instant
 *
 * This spell can't be copied.
 * Copy any number of target instant and/or sorcery spells. You may choose new targets
 * for the copies.
 *
 * Uses [com.wingedsheep.sdk.scripting.effects.CopyEachTargetSpellEffect] (CR 707.10): one
 * copy is made per targeted spell, with optional retargeting per copy. The "can't be
 * copied" clause maps to the card-level `cantBeCopied` flag (CR 707.10 — no copy results
 * if a copy effect names this spell).
 */
val DisplayOfPower = card("Display of Power") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell can't be copied.\n" +
        "Copy any number of target instant and/or sorcery spells. You may choose new " +
        "targets for the copies."

    cantBeCopied = true

    spell {
        target("any number of target instant and/or sorcery spells", Targets.AnyNumberOfInstantOrSorcerySpells)
        effect = Effects.CopyEachTargetSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        flavorText = "\"The Nazgûl closed round at night, and I was besieged. Such light and flame cannot have been seen on Weathertop since the war-beacons of old.\"\n—Gandalf"
        artist = "Shahab Alizadeh"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/295b8595-5b4e-4fc8-8249-486d36e15f67.jpg?1686968845"
    }
}
