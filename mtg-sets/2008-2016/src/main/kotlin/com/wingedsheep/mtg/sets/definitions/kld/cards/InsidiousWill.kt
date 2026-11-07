package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Insidious Will
 * {2}{U}{U}
 * Instant
 *
 * Choose one —
 * • Counter target spell.
 * • You may choose new targets for target spell.
 * • Copy target instant or sorcery spell. You may choose new targets for the copy.
 *
 * The second mode retargets *every* target of the spell, not just one, so it is
 * `ChangeTriggeringObjectTargets` pointed at the spell this mode targeted (the Wild Ricochet /
 * Invert Polarity shape), not the single-target `ChangeTarget`. Any spell is a legal target; one
 * with no targets simply has nothing to change. The third mode's "choose new targets for the copy"
 * is carried by `CopyTargetSpell` itself.
 */
val InsidiousWill = card("Insidious Will") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target spell.\n" +
        "• You may choose new targets for target spell.\n" +
        "• Copy target instant or sorcery spell. You may choose new targets for the copy."

    spell {
        modal {
            mode("Counter target spell") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("You may choose new targets for target spell") {
                val spell = target(TargetFilter.SpellOnStack)
                effect = Effects.ChangeTriggeringObjectTargets(spell = spell)
            }
            mode("Copy target instant or sorcery spell. You may choose new targets for the copy") {
                val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
                effect = Effects.CopyTargetSpell(spell)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Anthony Palumbo"
        flavorText = "\"We meet again, pyromancer.\"\n—Baral, Chief of Compliance"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8eafb2bb-58bf-4c6b-ae8f-91bcea12c7d2.jpg?1783937218"
        ruling("2016-09-20", "If you choose new targets for the target spell or for the copy, the new targets must be legal.")
        ruling("2016-09-20", "If you copy a spell, you control the copy.")
        ruling("2016-09-20", "The third mode can copy any instant or sorcery spell, not just one with targets.")
        ruling("2016-09-20", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2016-09-20", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2016-09-20", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. A different mode cannot be chosen.")
        ruling("2016-09-20", "If the spell that's copied has an X whose value was determined as it was cast (like Eliminate the Competition does), the copy will have the same value of X.")
        ruling("2016-09-20", "If the spell has damage divided as it was cast (like Chandra's Pyrohelix), the division can't be changed (although the targets receiving that damage still can).")
        ruling("2016-09-20", "The controller of a copy can't choose to pay any alternative or additional costs for the copy. However, effects based on any alternative or additional costs that were paid for the original spell are copied as though those same costs were paid for the copy.")
    }
}
