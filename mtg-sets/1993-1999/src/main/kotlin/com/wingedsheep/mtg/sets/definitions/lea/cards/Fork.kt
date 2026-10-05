package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fork
 * {R}{R}
 * Instant
 * Copy target instant or sorcery spell, except that the copy is red. You may choose new targets
 * for the copy.
 */
val Fork = card("Fork") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Copy target instant or sorcery spell, except that the copy is red. You may choose new targets for the copy."
    spell {
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(spell, exceptions = CopyExceptions(overrideColors = setOf(Color.RED)))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6b43916-fe2d-417a-a550-d7c795023297.jpg?1783948686"
        ruling("2004-12-01", "If you Fork a Spliced spell, the spliced text is added during the announcement of the original spell, and therefore is fully copied by Fork.")
        ruling("2004-10-04", "Fork does not let you make non-targeting choices about the spell.")
        ruling("2004-10-04", "For spells that can have a variable number of targets, the controller of the copy must use the same number of targets the original spell did.")
        ruling("2004-10-04", "Forking a spell with an X in the cost requires you to use the same X value.")
        ruling("2004-10-04", "The copy that is placed on the stack is not considered to have been \"cast\".")
    }
}
