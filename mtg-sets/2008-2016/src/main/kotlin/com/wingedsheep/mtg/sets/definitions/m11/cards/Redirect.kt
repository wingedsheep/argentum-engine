package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Redirect
 * {U}{U}
 * Instant
 *
 * You may choose new targets for target spell.
 *
 * Retargets *every* target of the spell (any number, including none), so this is
 * `ChangeTriggeringObjectTargets` pointed at the targeted spell — the Insidious Will second-mode
 * shape — not the single-target `ChangeTarget`. Any spell is a legal target; one with no targets
 * simply has nothing to change.
 */
val Redirect = card("Redirect") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may choose new targets for target spell."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.ChangeTriggeringObjectTargets(spell = spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Izzy"
        flavorText = "\"Your strategy is marvelous. I've just made a few minor adjustments.\"\n—Veris, watcher of Indi Keep"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60bae44b-c6f2-40bf-a427-aee5cfbdfea9.jpg?1783941822"
        ruling("2010-08-15", "You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2010-08-15", "If the targeted spell is modal (that is, it says \"Choose one —\" or the like), you can't choose a different mode.")
        ruling("2010-08-15", "If you cast Redirect targeting a spell that targets a spell on the stack (like Cancel does, for example), you can't change that spell's target to itself. You can, however, change that spell's target to Redirect. If you do, that spell won't resolve when it tries to resolve because Redirect will have left the stack by then.")
        ruling("2010-08-15", "Redirect can target any spell, not just an instant or sorcery spell. For example, you could use it to change the target of an Aura spell. However, if the targeted spell has no targets (for example, if it's an instant or sorcery spell that doesn't specifically use the word \"target,\" or if it's a creature spell), Redirect won't have any effect on it.")
    }
}
