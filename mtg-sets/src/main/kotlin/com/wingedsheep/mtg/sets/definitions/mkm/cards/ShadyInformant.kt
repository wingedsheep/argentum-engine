package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shady Informant — Murders at Karlov Manor #231
 * {3}{B}{R} · Creature — Ogre Rogue · 4/2
 *
 * When this creature dies, it deals 2 damage to any target.
 * Disguise {2}{B/R}{B/R}
 *
 * The dies trigger and the disguise cost don't interact beyond CR 708.2: a face-down Informant is
 * a vanilla 2/2 with ward {2} and has no dies trigger at all, so it must be face up when it dies
 * for the 2 damage to happen. Turning it face up is a special action, not a cast, and per
 * CR 702.168d is not entering the battlefield — nothing here keys on entering, so that's a no-op
 * for this card either way.
 *
 * The damage source is the Informant itself, read from last-known information (the permanent is
 * already in the graveyard when the ability resolves).
 */
val ShadyInformant = card("Shady Informant") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Rogue"
    power = 4
    toughness = 2
    oracleText = "When this creature dies, it deals 2 damage to any target.\n" +
        "Disguise {2}{B/R}{B/R} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)"

    disguise = "{2}{B/R}{B/R}"

    triggeredAbility {
        trigger = Triggers.Dies
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.DealDamage(2, anyTarget)
        description = "When this creature dies, it deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "231"
        artist = "Caio Monteiro"
        flavorText = "Rakdos snitches rarely survive to get stitches."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0de36e63-8190-415f-b65b-bae1e595845d.jpg?1783912840"
    }
}
