package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rootha, Mercurial Artist — Strixhaven: School of Mages #227 (canonical printing)
 * {1}{U}{R} · Legendary Creature — Orc Shaman · 1/4
 *
 * {2}, Return Rootha to its owner's hand: Copy target instant or sorcery spell you control. You
 * may choose new targets for the copy.
 *
 * Kitsa, Otterball Elite's copy ability with Maze's End's self-bounce cost: returning Rootha is
 * part of the cost ([Costs.ReturnSelfToHand]), so the copy resolves with Rootha already in hand.
 * The target is required, so the ability can't be activated just to bounce Rootha (ruling).
 */
val RoothaMercurialArtist = card("Rootha, Mercurial Artist") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Orc Shaman"
    power = 1
    toughness = 4
    oracleText = "{2}, Return Rootha to its owner's hand: Copy target instant or sorcery spell you " +
        "control. You may choose new targets for the copy."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ReturnSelfToHand)
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(spell)
        holdPriority = true
        description = "{2}, Return Rootha to its owner's hand: Copy target instant or sorcery spell " +
            "you control. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Chris Rahn"
        flavorText = "\"Art is never finished. Merely abandoned.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acb8f24d-8d1d-48aa-a534-238e969a474c.jpg?1783927296"
        ruling("2021-04-16", "Rootha, Mercurial Artist's ability must have a target to be activated. You can't activate it without a target in order to return Rootha to your hand.")
    }
}
