package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatures

/**
 * Keeper of the Lens
 * {1}
 * Artifact Creature — Golem
 * 1/2
 * You may look at face-down creatures you don't control any time.
 *
 * Same printed static as Lens of Clarity's second half: the server's visibility layer shows the
 * identity of every face-down battlefield creature to this permanent's controller, but not face-down
 * spells on the stack (per ruling).
 */
val KeeperOfTheLens = card("Keeper of the Lens") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 1
    toughness = 2
    oracleText = "You may look at face-down creatures you don't control any time."

    staticAbility {
        ability = LookAtFaceDownCreatures
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "240"
        artist = "Ryan Barger"
        flavorText = "\"It sees all, but it reveals what it sees only to a chosen few.\"\n—Taigam, Ojutai master"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86d6c502-ae3e-4e67-8f1d-3323b4468f72.jpg?1783938568"
        ruling("2015-02-25", "Keeper of the Lens allows you to look at face-down creatures you don't control whenever you want to, even if you don't have priority. This action doesn't use the stack.")
        ruling("2015-02-25", "Keeper of the Lens doesn't stop your opponents from looking at face-down creatures they control.")
        ruling("2015-02-25", "Keeper of the Lens doesn't let you look at other players' face-down spells while they're on the stack.")
    }
}
