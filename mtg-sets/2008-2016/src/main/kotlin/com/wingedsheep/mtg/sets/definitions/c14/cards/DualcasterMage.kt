package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dualcaster Mage
 * {1}{R}{R}
 * Creature — Human Wizard
 * 2/2
 * Flash
 * When this creature enters, copy target instant or sorcery spell. You may choose new targets for the copy.
 */
val DualcasterMage = card("Dualcaster Mage") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Flash\nWhen this creature enters, copy target instant or sorcery spell. You may choose new targets for the copy."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Matt Stewart"
        flavorText = "\"It has been my experience that disasters like to strike in pairs.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b80c8a0-0870-4836-bee1-f4a805d119d6.jpg?1783938867"
        ruling("2016-06-08", "Dualcaster Mage's triggered ability can target (and copy) any instant or sorcery spell, not just one with targets. It doesn't matter who controls it.")
        ruling("2016-06-08", "As Dualcaster Mage's triggered ability resolves, it creates a copy of a spell on the stack. The copy isn't cast, so abilities that trigger when a player casts a spell won't trigger.")
        ruling("2016-06-08", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2016-06-08", "If the spell is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. You can't choose a different one.")
        ruling("2016-06-08", "If the spell has an X whose value was determined as it was cast (like Earthquake does), the copy has the same value of X.")
    }
}
