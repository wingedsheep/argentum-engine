package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Radagast of Rhosgobel
 * {2}{G}{G}
 * Legendary Creature — Avatar Wizard
 * 2/5
 * The first creature spell you cast each turn costs {2} less to cast and can be cast as though it
 * had flash.
 *
 *  - One printed sentence, two engine-level statics: cost reduction (CR 601.2f) and a timing
 *    permission (CR 702.8) are read at different points of the cast, and neither can express the
 *    other. Both carry the *same* "first creature spell you cast each turn" gate so they can never
 *    disagree about which spell is the discounted one.
 *  - The gate counts off the caster's spells-cast-this-turn record, which the cost side already used
 *    ([CostGating.NthOfTypePerTurn]); [GrantFlashToSpellType.nthOfTypePerTurn] is the timing twin
 *    added for this card. The spell being cast is not yet in that record, so both gates are open
 *    exactly while zero creature spells have been cast this turn — and a creature spell that was
 *    countered still closes the window, because the wording keys on casting, not resolving.
 *  - Only generic mana is reduced (CR 601.2f), so a `{G}{G}` creature spell gets no discount.
 *  - The discount and the flash are not tied together: casting a creature spell at instant speed on
 *    an opponent's turn is itself the turn's first creature spell, and gets both.
 */
val RadagastOfRhosgobel = card("Radagast of Rhosgobel") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 2
    toughness = 5
    oracleText = "The first creature spell you cast each turn costs {2} less to cast and can be " +
        "cast as though it had flash."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.NthOfTypePerTurn(1),
        )
    }

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Creature,
            controllerOnly = true,
            nthOfTypePerTurn = 1,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Anna Podedworna"
        flavorText = "\"Perhaps you have heard of my good cousin Radagast who lives near the " +
            "southern borders of Mirkwood?\" Gandalf asked.\n" +
            "\"Yes,\" said Beorn, \"not a bad fellow as Wizards go, I believe.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5741bbad-a6e4-45e0-b827-73f48c9975bf.jpg?1785496313"
    }
}
