package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedIfDefenderControls
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Neurok Spy — Mirrodin #44
 * {2}{U} · Creature — Human Rogue · 2/2
 *
 * This creature can't be blocked as long as defending player controls an artifact.
 *
 * "Artifactwalk" — the landwalk shape with an artifact instead of a land type, and on Mirrodin that
 * makes a 2/2 for three essentially unblockable. Modelled with
 * [CantBeBlockedIfDefenderControls] over the unscoped [GameObjectFilter.Artifact]: the evasion rule
 * counts artifacts on the *defending player's* battlefield, so the filter needs no controller
 * predicate, and in a multiplayer pod attacking an artifact-less player leaves the Spy blockable
 * even while another opponent is drowning in Myr.
 *
 * Deliberately not `Conditions.OpponentControls(Artifact)` wrapped around `CantBeBlocked`: that
 * reads "any opponent", which is the same thing only in a two-player game.
 *
 * The check is made as blockers are declared and uses projected state, so a permanent that merely
 * *became* an artifact (Ensoul Artifact, March of the Machines) turns the evasion on, and an
 * artifact that leaves after blockers are declared doesn't retroactively undo a legal block.
 */
val NeurokSpy = card("Neurok Spy") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "This creature can't be blocked as long as defending player controls an artifact."

    staticAbility {
        ability = CantBeBlockedIfDefenderControls(GameObjectFilter.Artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Daren Bader"
        flavorText = "From the murk of Mephidross to the heart of Kuldotha, the vedalken send " +
            "their servants forth to gather knowledge from every inch of Mirrodin."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47b86a02-d40a-4615-8402-bd5700cb5101.jpg?1783944553"
    }
}
