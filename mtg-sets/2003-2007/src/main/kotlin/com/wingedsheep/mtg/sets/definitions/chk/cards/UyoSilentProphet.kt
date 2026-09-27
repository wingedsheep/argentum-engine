package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Uyo, Silent Prophet
 * {4}{U}{U}
 * Legendary Creature — Moonfolk Wizard
 * 4/4
 * Flying
 * {2}, Return two lands you control to their owner's hand: Copy target instant or sorcery spell.
 * You may choose new targets for the copy.
 *
 * The two-land Moonfolk cost is [Costs.ReturnToHand] with `count = 2` (Soratami Seer's shape);
 * the copy is [Effects.CopyTargetSpell], which carries the "you may choose new targets" prompt
 * itself (Mischievous Quanar's shape).
 */
val UyoSilentProphet = card("Uyo, Silent Prophet") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Moonfolk Wizard"
    power = 4
    toughness = 4
    oracleText = "Flying\n{2}, Return two lands you control to their owner's hand: Copy target " +
        "instant or sorcery spell. You may choose new targets for the copy."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ReturnToHand(Filters.Land, count = 2))
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CopyTargetSpell(target = spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "John Bolton"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbfb3c53-1e68-48ce-8008-93bc49e188dd.jpg?1783944318"
        ruling(
            "2004-12-01",
            "If you copy an Arcane spell that has cards spliced onto it, you'll copy the spliced text " +
                "as well as the original spell text."
        )
    }
}
