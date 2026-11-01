package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarkhan the Masterless — War of the Spark #143
 * {3}{R}{R} · Legendary Planeswalker — Sarkhan · Loyalty 5
 *
 * - Static trigger: Circle of Flame's ANY-binding "attacks you or a planeswalker you control"
 *   trigger, then a per-Dragon iteration where each Dragon is itself the damage source
 *   (`damageSource = EffectTarget.IterationEntity`) for 1 damage to the triggering attacker.
 * - +1: every planeswalker you control (locked in at resolution) becomes a 4/4 red Dragon
 *   creature with flying until end of turn. "Becomes a … creature" without "in addition to its
 *   other types" replaces the planeswalker type (2019-05-03 ruling), so PLANESWALKER is removed
 *   just as Sarkhan, the Dragonspeaker does; loyalty counters and loyalty abilities remain.
 * - −3: a 4/4 red flying Dragon token.
 */
val SarkhanTheMasterless = card("Sarkhan the Masterless") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Sarkhan"
    startingLoyalty = 5
    oracleText = "Whenever a creature attacks you or a planeswalker you control, each Dragon you control " +
        "deals 1 damage to that creature.\n" +
        "+1: Until end of turn, each planeswalker you control becomes a 4/4 red Dragon creature and gains flying.\n" +
        "−3: Create a 4/4 red Dragon creature token with flying."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Dragon").youControl()),
            effect = Effects.DealDamage(
                amount = 1,
                target = EffectTarget.TriggeringEntity,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    loyaltyAbility(+1) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.PlaneswalkersYouControl,
            effect = Effects.BecomeCreature(
                target = EffectTarget.IterationEntity,
                power = 4,
                toughness = 4,
                keywords = setOf(Keyword.FLYING),
                creatureTypes = setOf("Dragon"),
                removeTypes = setOf("PLANESWALKER"),
                colors = setOf("RED"),
                duration = Duration.EndOfTurn,
            ),
        )
    }

    loyaltyAbility(-3) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a195b254-bc3c-4259-9219-5a9e085d7947.jpg?1783933349"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb96971-5bf1-4ae3-98f4-7cd7a614bf97.jpg?1783933422"
        ruling(
            "2019-05-03",
            "A planeswalker that becomes a creature due to Sarkhan's first loyalty ability can't attack " +
                "unless you've controlled it continuously since your turn began."
        )
        ruling(
            "2019-05-03",
            "Once Sarkhan's first loyalty ability has resolved, each planeswalker you control (including " +
                "Sarkhan) is no longer a planeswalker for the rest of the turn. They don't lose any loyalty " +
                "counters or abilities, and you can still activate their loyalty abilities if you haven't done " +
                "so yet this turn. They don't lose loyalty if they're dealt damage while they're not planeswalkers."
        )
    }
}
