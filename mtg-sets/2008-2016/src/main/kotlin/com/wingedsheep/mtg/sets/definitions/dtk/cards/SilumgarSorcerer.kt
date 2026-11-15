package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silumgar Sorcerer
 * {1}{U}{U}
 * Creature — Human Wizard Sorcerer
 * 2/1
 * Flash
 * Flying
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, counter target creature spell.
 *
 * Flashed in with a creature spell on the stack, the Sorcerer's exploit trigger resolves above that
 * spell; the counter rides the exploit reflexive, so the spell is chosen after the sacrifice.
 */
val SilumgarSorcerer = card("Silumgar Sorcerer") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard Sorcerer"
    power = 2
    toughness = 1
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Flying\n" +
        "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, counter target creature spell."

    keywords(Keyword.FLASH, Keyword.FLYING)

    exploit {
        target(TargetFilter.CreatureSpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Jeff Simpson"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a914304-e774-49fd-9ac7-09e36989b1d5.jpg"
    }
}
