package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silence — Magic 2010 #31 (canonical / earliest real printing, 2009)
 * {W} · Instant
 *
 * Your opponents can't cast spells this turn.
 *
 * `CantCastSpells` over each opponent, the same restriction Flamescroll Celebrant applies.
 * Untargeted, and only casting is stopped — abilities and land drops are untouched.
 */
val Silence = card("Silence") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Your opponents can't cast spells this turn."

    spell {
        effect = Effects.CantCastSpells(EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Wayne Reynolds"
        flavorText = "\"All the wizardry in the world has to pass through one small and easily sealed " +
            "door.\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1559d660-8a9d-422b-95d3-710a046583dd.jpg?1783942397"
        ruling(
            "2021-03-19",
            "Silence won't affect spells that your opponents cast before you cast Silence, including any " +
                "spells that are still on the stack. Silence also won't stop your opponents from casting " +
                "spells after you cast Silence but before Silence resolves."
        )
        ruling(
            "2021-03-19",
            "The only thing Silence stops is casting spells. Your opponents can still activate abilities, " +
                "including abilities of cards in their hands (like cycling). Their triggered abilities work " +
                "as normal, they can still play lands, and so on."
        )
        ruling("2021-03-19", "A player who can't cast a spell can't suspend a card.")
    }
}
