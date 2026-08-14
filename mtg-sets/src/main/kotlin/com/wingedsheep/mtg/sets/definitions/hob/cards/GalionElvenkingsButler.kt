package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Galion, Elvenking's Butler — The Hobbit #125
 * {2}{G}{G} · Legendary Creature — Elf Advisor · Uncommon
 * 4/4
 *
 * Whenever Galion attacks, choose up to one other target creature you control. Its base power and
 * toughness become equal to Galion's power and toughness until end of turn.
 *
 * Modeling notes:
 *  - "up to one **other** target creature you control" — `optional = true` plus
 *    `TargetFilter.OtherCreatureYouControl`, so attacking alone is legal and the trigger simply does
 *    nothing.
 *  - This is a layer 7b *set* evaluated once on resolution ([Effects.SetBasePowerAndToughness] with
 *    dynamic amounts), not a continuously-recomputed CDA: the chosen creature keeps the numbers
 *    Galion had when the trigger resolved even if Galion is later pumped or dies. Because it sets the
 *    *base* stats, a +1/+1 counter or an Aura still applies on top in layer 7c/7d.
 */
val GalionElvenkingsButler = card("Galion, Elvenking's Butler") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Advisor"
    power = 4
    toughness = 4
    oracleText = "Whenever Galion attacks, choose up to one other target creature you control. " +
        "Its base power and toughness become equal to Galion's power and toughness until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        val other = target(
            "up to one other target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreatureYouControl)
        )
        effect = Effects.SetBasePowerAndToughness(
            power = DynamicAmounts.sourcePower(),
            toughness = DynamicAmounts.sourceToughness(),
            target = other,
            duration = Duration.EndOfTurn
        )
        description = "Whenever Galion attacks, choose up to one other target creature you control. " +
            "Its base power and toughness become equal to Galion's power and toughness until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Jarel Threat"
        flavorText = "\"Here's the old villain with his head on a jug! He's been having a little " +
            "feast all to himself!\"\n—Elven guard"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985bd676-58c4-42c7-a570-1b413e9aa94c.jpg?1785152142"
    }
}
