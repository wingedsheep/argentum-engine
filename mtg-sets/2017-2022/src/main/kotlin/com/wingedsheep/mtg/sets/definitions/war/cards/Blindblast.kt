package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blindblast — War of the Spark #114 (canonical printing)
 * {2}{R}
 * Instant
 * Blindblast deals 1 damage to target creature. That creature can't block this turn.
 * Draw a card.
 *
 * The can't-block applies even if the damage is prevented. With its only target illegal on
 * resolution the spell doesn't resolve, so no card is drawn (rulings).
 */
val Blindblast = card("Blindblast") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Blindblast deals 1 damage to target creature. That creature can't block this turn.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(1, creature) then
            Effects.CantBlock(target = creature) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Peril is in the eye of the beholder. At least, if your aim is good.\"\n—Jaya Ballard"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d49637b-2255-4f16-8f24-140145966bfa.jpg?1783933434"
        ruling(
            "2019-05-03",
            "Casting Blindblast after a creature has blocked won't remove the blocking creature from combat " +
                "unless the damage Blindblast deals causes that creature to die. It won't cause the creature it " +
                "blocked to become unblocked even if the blocking creature does die."
        )
        ruling(
            "2019-05-03",
            "If the target creature is an illegal target by the time Blindblast tries to resolve, the spell " +
                "doesn't resolve. You won't draw a card."
        )
    }
}
