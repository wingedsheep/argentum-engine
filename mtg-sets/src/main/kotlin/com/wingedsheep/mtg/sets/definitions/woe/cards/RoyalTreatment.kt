package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Royal Treatment
 * {G}
 * Instant
 *
 * Target creature you control gains hexproof until end of turn. Create a Royal Role token
 * attached to that creature.
 *
 * The +1/+1 and ward {1} come from the Royal Role token itself, so the spell only grants
 * hexproof and creates the Role. "If you control another Role on it, put that one into the
 * graveyard" isn't card text to model — it's the Role state-based action (CR 303.7a / 704.5y),
 * which keeps only the newest-timestamped Role a player controls on a permanent. The Role sticks
 * around after the turn ends; only the hexproof is until end of turn.
 */
val RoyalTreatment = card("Royal Treatment") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gains hexproof until end of turn. Create a Royal Role token " +
        "attached to that creature. (If you control another Role on it, put that one into the graveyard. " +
        "Enchanted creature gets +1/+1 and has ward {1}.)"

    spell {
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.HEXPROOF, t),
            Effects.CreateRoleToken("Royal Role", t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Julia Metzger"
        flavorText = "\"Stay your blades, peasants.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6516b8f-ecfb-401e-ba8e-bf561aa2be64.jpg?1783915078"
    }
}
