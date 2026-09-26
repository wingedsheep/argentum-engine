package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dosan the Falling Leaf
 * {1}{G}{G}
 * Legendary Creature — Human Monk
 * 2/2
 * Players can cast spells only during their own turns.
 *
 * Every player — Dosan's controller included — can't cast spells during a turn that isn't theirs.
 * `conditionFromCaster` reads [Conditions.IsNotYourTurn] from each caster's seat, so it holds in multiplayer
 * too, where "not the controller's turn" would be the wrong question. Abilities are untouched.
 */
val DosanTheFallingLeaf = card("Dosan the Falling Leaf") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Monk"
    power = 2
    toughness = 2
    oracleText = "Players can cast spells only during their own turns."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.Each,
            condition = Conditions.IsNotYourTurn,
            conditionFromCaster = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Mark Zug"
        flavorText = "\"Each night as Master Dosan prays to the kami, the hate he receives in return withers his " +
            "body a little more. Though the kami are slowly killing him, still he continues his prayers.\"\n" +
            "—Meditation journal of a young budoka"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffb190db-48fc-4c39-ae9f-5e304eabb4f4.jpg?1783944291"

        ruling("2004-12-01", "Dosan's ability only stops players from casting spells. It doesn't stop activated or triggered abilities.")
    }
}
