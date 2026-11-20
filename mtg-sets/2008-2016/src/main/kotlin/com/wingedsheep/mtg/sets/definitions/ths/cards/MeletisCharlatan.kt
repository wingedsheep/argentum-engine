package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Meletis Charlatan
 * {2}{U}
 * Creature — Human Wizard
 * 2/3
 *
 * {2}{U}, {T}: The controller of target instant or sorcery spell copies it. That player may choose
 * new targets for the copy.
 *
 * The copier is the targeted spell's controller (`copier = Player.ControllerOf`), so the copy is
 * theirs and so is the "choose new targets" decision — doubling an opponent's spell for them.
 */
val MeletisCharlatan = card("Meletis Charlatan") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "{2}{U}, {T}: The controller of target instant or sorcery spell copies it. " +
        "That player may choose new targets for the copy."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(spell, copier = Player.ControllerOf("target spell"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Jason A. Engle"
        flavorText = "\"Every object has an echo in the Æther beyond the world. Every idea has a " +
            "shadow that can be brought to light.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf22da0c-5bf5-4c81-9422-671ce17c8ffa.jpg?1783939794"
        ruling(
            "2013-09-15",
            "Meletis Charlatan's ability can target any instant or sorcery spell, not just one with " +
                "targets and not just one that you control."
        )
        ruling(
            "2013-09-15",
            "When the ability resolves, it creates a copy of the spell. The controller of the original " +
                "spell controls the copy. That copy is created on the stack, so it's not \"cast.\" " +
                "Abilities that trigger when a player casts a spell won't trigger. The copy will then " +
                "resolve like a normal spell, after players get a chance to cast spells and activate abilities."
        )
        ruling(
            "2013-09-15",
            "The copy will have the same targets as the spell it's copying unless its controller chooses " +
                "new ones. That player may change any number of the targets, including all of them or none " +
                "of them. If, for one of the targets, that player can't choose a new legal target, then it " +
                "remains unchanged (even if the current target is illegal)."
        )
        ruling(
            "2013-09-15",
            "If the spell being copied is modal (that is, it says \"Choose one —\" or the like), the copy " +
                "will have the same mode. Its controller can't choose a different one."
        )
        ruling(
            "2013-09-15",
            "If the spell being copied has an X whose value was determined as it was cast (like Curse of " +
                "the Swine has), the copy has the same value of X."
        )
    }
}
