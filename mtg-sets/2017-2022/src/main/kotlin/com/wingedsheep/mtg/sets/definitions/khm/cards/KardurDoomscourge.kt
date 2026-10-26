package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kardur, Doomscourge — Kaldheim #216 (canonical printing)
 * {2}{B}{R} Legendary Creature — Demon Berserker 4/3
 *
 * The enters trigger is a durational group requirement, not goad: the creatures aren't goaded, and
 * it reaches creatures that come under an opponent's control later (it's rule-modifying, CR
 * 611.2c). "You" is Kardur's controller when the trigger resolves. "Attacking" in the dies trigger
 * is read off the zone change's last-known information.
 */
val KardurDoomscourge = card("Kardur, Doomscourge") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon Berserker"
    power = 4
    toughness = 3
    oracleText = "When Kardur enters, until your next turn, creatures your opponents control attack each " +
        "combat if able and attack a player other than you if able.\n" +
        "Whenever an attacking creature dies, each opponent loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MustAttackGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            MustAttackDefender.PlayerOtherThanYou,
            Duration.UntilYourNextTurn
        )
        description = "When Kardur enters, until your next turn, creatures your opponents control attack " +
            "each combat if able and attack a player other than you if able."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attacking()).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever an attacking creature dies, each opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9685e2a0-5573-41bc-a914-f40c3011459b.jpg?1783928196"
        ruling("2021-02-05", "Kardur's first ability affects all creatures your opponents control, including any that enter the battlefield after the ability resolves.")
        ruling("2021-02-05", "If a creature an opponent controls doesn't qualify for any of the above exceptions and can attack, it must attack a player other than you if able. If the creature can't attack any of those players, or if there are no such players, it must attack you or an opposing planeswalker.")
        ruling("2021-02-05", "Kardur's last ability will trigger no matter who controlled the attacking creature and no matter who it was attacking.")
    }
}
