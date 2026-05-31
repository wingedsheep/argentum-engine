package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Flamehold Grappler
 * {U}{R}{W}
 * Creature — Human Monk
 * 3/3
 *
 * First strike
 * When this creature enters, copy the next spell you cast this turn when you cast it.
 * You may choose new targets for the copy. (A copy of a permanent spell becomes a token.)
 *
 * The "copy the next spell" clause copies *any* spell — not just instants and sorceries —
 * so the pending copy uses [GameObjectFilter.Any]. The shared Storm copy infrastructure
 * already handles choosing new targets for the copy and turning a copied permanent spell
 * into a token.
 */
val FlameholdGrappler = card("Flamehold Grappler") {
    manaCost = "{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Creature — Human Monk"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "When this creature enters, copy the next spell you cast this turn when you cast it. " +
        "You may choose new targets for the copy. (A copy of a permanent spell becomes a token.)"

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CopyNextSpellCast(copies = 1, spellFilter = GameObjectFilter.Any)
        description = "copy the next spell you cast this turn when you cast it. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Wayne Wu"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc8443a6-282f-4218-9dc8-144b5570d891.jpg?1743204722"
    }
}
