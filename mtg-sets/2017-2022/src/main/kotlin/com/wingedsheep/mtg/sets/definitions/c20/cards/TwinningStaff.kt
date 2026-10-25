package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Twinning Staff
 * {3}
 * Artifact
 *
 * If you would copy a spell one or more times, instead copy it that many times plus an additional
 * time. You may choose new targets for the additional copy.
 * {7}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy.
 *
 * The replacement is the spell-copy case of [ModifyKeywordActionAmount]: every spell-copy
 * instruction you carry out (storm, Dualcaster Mage, this card's own ability, …) makes one more
 * copy, and each Staff adds its own.
 */
val TwinningStaff = card("Twinning Staff") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "If you would copy a spell one or more times, instead copy it that many times plus an " +
        "additional time. You may choose new targets for the additional copy.\n" +
        "{7}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy."

    replacementEffect(ModifyKeywordActionAmount(appliesTo = EventPattern.SpellCopiedEvent(), modifier = 1))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{7}"), Costs.Tap)
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(spell)
        holdPriority = true
        description = "{7}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7971306-700e-4677-bf52-83e4f9ac04c1.jpg?1783931206"
    }
}
