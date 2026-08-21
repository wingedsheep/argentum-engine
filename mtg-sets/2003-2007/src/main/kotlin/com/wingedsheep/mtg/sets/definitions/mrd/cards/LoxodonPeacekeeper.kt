package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RankTieBreak

/**
 * Loxodon Peacekeeper — Mirrodin #13 (canonical printing)
 * {1}{W} · Creature — Elephant Soldier · 4/4
 *
 * At the beginning of your upkeep, the player with the lowest life total gains control of this
 * creature. If two or more players are tied for lowest life total, you choose one of them, and
 * that player gains control of this creature.
 *
 * A 4/4 for two that keeps defecting to whoever is losing. Mechanically it is Ghazbán Ogre read
 * backwards, which is exactly how it is scripted: the same
 * [com.wingedsheep.sdk.scripting.effects.GainControlByRankEffect], with the direction flipped to
 * LEAST and the tie rule changed.
 *
 * Both of those had to become real axes for this card. The effect previously hardcoded "most" and
 * "a tie means nothing happens" — the second of which is not a rare edge case here but the
 * *opening position* of every game: two players on 20 are tied for lowest, so on the first upkeep
 * the controller chooses, and choosing themselves is how they keep it. An implementation that
 * quietly did nothing on a tie would look right in a race and be wrong on turn two.
 *
 * Note there is no intervening-if clause on this trigger, unlike Ghazbán Ogre's: the ability
 * always triggers and always resolves, and the tie-break is part of the resolution rather than a
 * condition on it.
 */
val LoxodonPeacekeeper = card("Loxodon Peacekeeper") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Soldier"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your upkeep, the player with the lowest life total gains " +
        "control of this creature. If two or more players are tied for lowest life total, you " +
        "choose one of them, and that player gains control of this creature."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.GainControlByLowestLife(tieBreak = RankTieBreak.CONTROLLER_CHOOSES)
        description = "At the beginning of your upkeep, the player with the lowest life total " +
            "gains control of this creature. If two or more players are tied for lowest life " +
            "total, you choose one of them, and that player gains control of this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c9e7029-bea3-4a33-bd05-774e802616d4.jpg?1783944561"
    }
}
