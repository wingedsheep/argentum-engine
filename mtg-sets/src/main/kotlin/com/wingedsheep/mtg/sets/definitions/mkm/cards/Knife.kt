package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Knife — Murders at Karlov Manor #134
 * {R} · Artifact — Clue Equipment
 *
 * During your turn, equipped creature gets +1/+0 and has first strike.
 * {2}, Sacrifice this Equipment: Draw a card.
 * Equip {2}
 *
 * One of the set's "murder weapon" Equipment — an Equipment that is also a Clue, so it carries the
 * standard Clue sacrifice-to-draw *and* the Clue subtype, which the set's "sacrifice a Clue" payoffs
 * read straight off the type line (Scryfall's ruling: "If an effect refers to a Clue, it means any
 * Clue artifact, not just a Clue artifact token").
 *
 * "During your turn" gates both halves, so they are two [ConditionalStaticAbility] blocks sharing
 * [Conditions.IsYourTurn] rather than one — a static ability is one continuous modification, and
 * +1/+0 (layer 7c) and first strike (layer 6) apply in different layers anyway. "Your" is the
 * Equipment controller's turn, not the equipped creature's controller's, which matters only when
 * the creature has been stolen out from under the Equipment.
 */
val Knife = card("Knife") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Clue Equipment"
    oracleText = "During your turn, equipped creature gets +1/+0 and has first strike.\n" +
        "{2}, Sacrifice this Equipment: Draw a card.\n" +
        "Equip {2}"

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = ModifyStats(+1, +0, Filters.EquippedCreature)
    }

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Tony Foti"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6883788-e1ee-4ddd-add2-24d6bc367717.jpg?1783912882"

        ruling(
            "2016-04-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token."
        )
        ruling(
            "2016-04-08",
            "You can't sacrifice a Clue to pay multiple costs."
        )
    }
}
