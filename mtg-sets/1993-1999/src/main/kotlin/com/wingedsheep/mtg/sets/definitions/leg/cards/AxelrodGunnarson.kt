package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Axelrod Gunnarson — Legends #219
 * {4}{B}{B}{R}{R} · Legendary Creature — Giant · 5/5
 *
 * Trample
 * Whenever a creature dealt damage by Axelrod Gunnarson this turn dies, you gain 1 life and
 * Axelrod Gunnarson deals 1 damage to target player or planeswalker.
 *
 * `Triggers.self.damagedCreatureDies()` is the Sengir Vampire trigger: it looks back over the
 * turn's damage record, so a combat trade still triggers and the ping is dealt using last-known
 * information of Axelrod.
 */
val AxelrodGunnarson = card("Axelrod Gunnarson") {
    manaCost = "{4}{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Giant"
    oracleText = "Trample\n" +
        "Whenever a creature dealt damage by Axelrod Gunnarson this turn dies, you gain 1 life and " +
        "Axelrod Gunnarson deals 1 damage to target player or planeswalker."
    power = 5
    toughness = 5
    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.GainLife(1) then Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Scott Kirschner"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acce83cf-965b-4e45-8efb-63f814df7a35.jpg?1783948041"
        ruling(
            "2009-10-01",
            "Each time a creature is put into a graveyard from play, check whether Axelrod Gunnarson had " +
                "dealt any damage to it at any time during that turn. (This includes combat damage.) If so, " +
                "Axelrod Gunnarson's second ability will trigger. It doesn't matter who controlled the " +
                "creature or whose graveyard it was put into."
        )
        ruling(
            "2009-10-01",
            "If Axelrod Gunnarson and a creature it dealt damage to are both put into a graveyard at the " +
                "same time, Axelrod Gunnarson's second ability will trigger."
        )
        ruling(
            "2009-10-01",
            "The player you target with Axelrod Gunnarson's ability doesn't have to be the player that " +
                "controlled the creature that was put into a graveyard."
        )
    }
}
