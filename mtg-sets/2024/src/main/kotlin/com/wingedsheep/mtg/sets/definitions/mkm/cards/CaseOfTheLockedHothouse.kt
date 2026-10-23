package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.solvedStaticAbility
import com.wingedsheep.sdk.dsl.toSolve
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * Case of the Locked Hothouse — Murders at Karlov Manor #155
 * {3}{G} · Enchantment — Case · Rare
 *
 * You may play an additional land on each of your turns.
 * To solve — You control seven or more lands.
 * Solved — You may look at the top card of your library any time, and you may play lands and cast
 * creature and enchantment spells from the top of your library.
 *
 * The unsolved half is what solves it: an extra land drop each turn is how a four-mana enchantment
 * reaches seven lands, and per the printed ruling the extra drops stack — two Hothouses give three
 * lands a turn.
 *
 * The Solved line is two statics because the visibility and the play permission are separate facts
 * (the same split Assemble the Players uses): [LookAtTopOfLibrary] is the *private* peek — this is
 * not Future Sight's public reveal — and [PlayLandsAndCastFilteredFromTopOfLibrary] is the
 * permission, whose `spellFilter` is the creature-or-enchantment restriction while lands are always
 * included. Both are gated on solved, so the top card stays hidden until the Case is solved rather
 * than being visible but unplayable.
 */
val CaseOfTheLockedHothouse = card("Case of the Locked Hothouse") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Case"
    oracleText = "You may play an additional land on each of your turns.\n" +
        "To solve — You control seven or more lands. (If unsolved, solve at the beginning of your " +
        "end step.)\n" +
        "Solved — You may look at the top card of your library any time, and you may play lands " +
        "and cast creature and enchantment spells from the top of your library."

    staticAbility {
        ability = GrantAdditionalLandDrop(1)
    }

    toSolve(Conditions.YouControlAtLeast(7, GameObjectFilter.Land))

    solvedStaticAbility {
        ability = LookAtTopOfLibrary()
    }

    solvedStaticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(
            spellFilter = GameObjectFilter.Creature or GameObjectFilter.Enchantment
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Leanna Crossan"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0929a1bd-e35c-4ca5-8c8c-dd304cf4b830.jpg?1783912869"

        ruling(
            "2024-02-09",
            "The effect of Case of the Locked Hothouse that allows you to play an additional land " +
                "is cumulative with similar effects. For example, if you control two Case of the " +
                "Locked Hothouses, you'll be able to play three lands during each of your turns."
        )
    }
}
