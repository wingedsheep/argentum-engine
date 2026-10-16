package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Errant, Street Artist (SNC #41)
 * {U}  Legendary Creature — Human Rogue  0/3
 *
 * Flash
 * Defender, haste
 * {1}{U}, {T}: Copy target spell you control that wasn't cast. You may choose new targets for the copy.
 *
 * "Wasn't cast" is `StatePredicate.WasCast` negated (via `TargetFilter.notCast()`): a copy put onto
 * the stack isn't cast (CR 707.10), so it carries no cast origin. The copy is
 * [Effects.CopyTargetSpell], which carries the "you may choose new targets" prompt itself and
 * copies permanent spells too (a casualty copy, say).
 */
val ErrantStreetArtist = card("Errant, Street Artist") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Rogue"
    power = 0
    toughness = 3
    oracleText = "Flash\nDefender, haste\n{1}{U}, {T}: Copy target spell you control that wasn't cast. " +
        "You may choose new targets for the copy."

    keywords(Keyword.FLASH, Keyword.DEFENDER, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val spell = target(TargetFilter.SpellOnStack.youControl().notCast())
        effect = Effects.CopyTargetSpell(target = spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Justine Cruz"
        flavorText = "Few know her face, but everyone knows her work."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61d23407-9fe0-4cbf-b4b5-bc1e132c36e4.jpg?1783923146"
    }
}
