package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ascent of the Worthy
 * {1}{W}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Choose a creature you control. Until your next turn, all damage that would be dealt to
 * creatures you control is dealt to that creature instead.
 * III — Return target creature card from your graveyard to the battlefield with a flying counter on
 * it. That creature is an Angel Warrior in addition to its other types.
 *
 * Chapters I and II pick the creature on resolution without targeting it, then set up a class-wide
 * redirection shield: [RedirectScope.CONTINUOUS] so it is never used up, a `Creature.youControl()`
 * `protectedFilter` read at damage time from the Saga controller's side (creatures that arrive
 * later are covered; if the chosen creature changes control, damage to your creatures still goes
 * to it), lasting [Duration.UntilYourNextTurn]. Damage already headed for the chosen creature
 * stays there, and a chosen creature that has left the battlefield redirects nothing.
 *
 * Chapter III's rider has no duration, so (like Liliana, Death's Majesty's −3) the Angel and
 * Warrior types are permanent Layer 4 grants on the returned creature.
 */
val AscentOfTheWorthy = card("Ascent of the Worthy") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Choose a creature you control. Until your next turn, all damage that would be dealt to " +
        "creatures you control is dealt to that creature instead.\n" +
        "III — Return target creature card from your graveyard to the battlefield with a flying counter " +
        "on it. That creature is an Angel Warrior in addition to its other types."

    val chooseSoak = Effects.Pipeline {
        val chosen = selectTarget(
            TargetObject(filter = TargetFilter.Creature.youControl()),
            nonTargeting = true,
            prompt = "Choose a creature you control to take the damage dealt to your creatures"
        )
        run(Effects.RedirectNextDamage(
            protectedTargets = emptyList(),
            redirectTo = chosen.asTarget,
            scope = RedirectScope.CONTINUOUS,
            protectedFilter = GameObjectFilter.Creature.youControl(),
            duration = Duration.UntilYourNextTurn,
        ))
    }

    sagaChapter(1) {
        effect = chooseSoak
    }

    sagaChapter(2) {
        effect = chooseSoak
    }

    sagaChapter(3) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(
            creatureCard,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.FLYING,
        ) then
            Effects.AddCreatureType("Angel", creatureCard, Duration.Permanent) then
            Effects.AddCreatureType("Warrior", creatureCard, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "James Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bf01666-0fbf-4b15-a33d-964165bbfafb.jpg?1783928202"
    }
}
