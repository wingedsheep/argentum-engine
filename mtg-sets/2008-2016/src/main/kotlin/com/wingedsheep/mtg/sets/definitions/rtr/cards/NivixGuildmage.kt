package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nivix Guildmage
 * {U}{R}
 * Creature — Human Wizard
 * 2/2
 * {1}{U}{R}: Draw a card, then discard a card.
 * {2}{U}{R}: Copy target instant or sorcery spell you control. You may choose new targets for the copy.
 *
 * Canonical printing: Return to Ravnica (earliest); reprinted in C13, C17 and GK1.
 */
val NivixGuildmage = card("Nivix Guildmage") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{1}{U}{R}: Draw a card, then discard a card.\n" +
        "{2}{U}{R}: Copy target instant or sorcery spell you control. You may choose new targets for the copy."

    activatedAbility {
        cost = Costs.Mana("{1}{U}{R}")
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{R}")
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Scott M. Fischer"
        flavorText = "\"The only action worth taking is one with an unknown outcome.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d3fde47-8d9e-4a84-b8a5-dcfe0c1d443c.jpg?1783940335"
        ruling(
            "2012-10-01",
            "Nivix Guildmage's second ability can target (and copy) any instant or sorcery spell you control, not just one with targets."
        )
        ruling(
            "2012-10-01",
            "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal)."
        )
    }
}
