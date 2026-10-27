package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kardur's Vicious Return — Kaldheim #217
 * {2}{B}{R} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — You may sacrifice a creature. When you do, this Saga deals 3 damage to any target.
 * II — Each player discards a card.
 * III — Return target creature card from your graveyard to the battlefield. Put a +1/+1 counter on
 *       it. It gains haste until your next turn.
 *
 * Chapter I goes on the stack untargeted; the sacrifice is chosen on resolution and the damage is a
 * reflexive trigger whose target is picked only then (Shrapnel Slinger's shape). Chapter II uses
 * [Effects.EachPlayerDiscards], which owns the simultaneous set-aside-then-discard the ruling calls for.
 */
val KardursViciousReturn = card("Kardur's Vicious Return") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — You may sacrifice a creature. When you do, this Saga deals 3 damage to any target.\n" +
        "II — Each player discards a card.\n" +
        "III — Return target creature card from your graveyard to the battlefield. Put a +1/+1 counter on it. " +
        "It gains haste until your next turn."

    sagaChapter(1) {
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val toSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.Creature.youControl()),
                    nonTargeting = true,
                    prompt = "Choose a creature to sacrifice"
                )
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice a creature. When you do, this Saga deals 3 damage to any target."
        ) {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(3, any)
        }
    }

    sagaChapter(2) {
        effect = Effects.EachPlayerDiscards(1)
    }

    sagaChapter(3) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creatureCard) then
            Effects.GrantKeyword(Keyword.HASTE, creatureCard, Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/233e4774-e701-42d9-aa25-e7e06c14e43d.jpg?1783928196"
        ruling(
            "2021-02-05",
            "The chapter I ability goes on the stack without a target. While that ability is resolving, you may " +
                "sacrifice a creature. When you do, the reflexive triggered ability triggers and you pick a target " +
                "to be dealt damage. This is different than effects that say \"If you do\" in that players may take " +
                "actions after you've sacrificed the creature but before damage is dealt."
        )
        ruling(
            "2021-02-05",
            "As the chapter II ability resolves, first the player whose turn it is chooses a card in hand and sets " +
                "it aside without revealing it, then each other player in turn order does the same. Then all the " +
                "chosen cards are discarded at the same time."
        )
    }
}
