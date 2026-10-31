package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lutri, the Spellchaser — Ikoria: Lair of Behemoths #227
 * {1}{U/R}{U/R} · Legendary Creature — Elemental Otter · 3/2
 *
 * Companion — singleton among nonland cards ([CompanionCondition.DistinctNames] over Nonland).
 * "If you cast it" is the any-zone [Conditions.WasCast] intervening-if; the copy targets any
 * instant or sorcery spell you control (with or without targets) and may be retargeted.
 */
val LutriTheSpellchaser = card("Lutri, the Spellchaser") {
    manaCost = "{1}{U/R}{U/R}"
    typeLine = "Legendary Creature — Elemental Otter"
    power = 3
    toughness = 2
    oracleText = "Companion — Each nonland card in your starting deck has a different name. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "Flash\n" +
        "When Lutri enters, if you cast it, copy target instant or sorcery spell you control. " +
        "You may choose new targets for the copy."

    keywordAbility(KeywordAbility.companion(CompanionCondition.DistinctNames(GameObjectFilter.Nonland)))

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
        description = "When Lutri enters, if you cast it, copy target instant or sorcery spell you control. " +
            "You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb1189c9-7842-466e-8238-1e02677d8494.jpg?1783931010"
        ruling("2020-04-17", "Lutri's ability can copy any instant or sorcery spell you control, not just one with targets.")
        ruling("2020-04-17", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger. It will resolve before the original spell does.")
        ruling("2020-04-17", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. A different mode can't be chosen.")
        ruling("2020-04-17", "If the spell that's copied has an X whose value was determined as it was cast, the copy will have the same value of X.")
    }
}
