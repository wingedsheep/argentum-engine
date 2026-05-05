package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rhys, the Evermore
 * {1}{W}
 * Legendary Creature — Elf Warrior
 * 2/2
 *
 * Flash
 * When Rhys enters, another target creature you control gains persist until end of turn.
 *   (When it dies, if it had no -1/-1 counters on it, return it to the battlefield under
 *   its owner's control with a -1/-1 counter on it.)
 * {W}, {T}: Remove any number of counters from target creature you control.
 *   Activate only as a sorcery.
 */
val RhysTheEvermore = card("Rhys, the Evermore") {
    manaCost = "{1}{W}"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When Rhys enters, another target creature you control gains persist until end of turn. " +
        "(When it dies, if it had no -1/-1 counters on it, return it to the battlefield under its " +
        "owner's control with a -1/-1 counter on it.)\n" +
        "{W}, {T}: Remove any number of counters from target creature you control. Activate only as a sorcery."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "another target creature you control",
            TargetCreature(filter = TargetFilter.OtherCreatureYouControl)
        )
        effect = Effects.GrantKeyword(Keyword.PERSIST, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.RemoveAnyNumberOfCounters(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7072412-4aa2-40ef-a267-bd717551a42b.jpg?1767659090"
        ruling("2025-11-17", "If a creature has +1/+1 counters and -1/-1 counters on it, state-based actions remove the same number of each so that it has only one kind of those counters on it. A creature's persist ability can bring it back again if its -1/-1 counters are removed this way.")
        ruling("2025-11-17", "If the target creature dies but that card leaves the graveyard before the persist ability resolves, it won't be returned to the battlefield.")
        ruling("2025-11-17", "If a creature with persist that has +1/+1 counters on it receives enough -1/-1 counters to cause it to be destroyed by lethal damage or put into its owner's graveyard for having 0 or less toughness, persist won't trigger and the card won't return to the battlefield. That's because persist checks the creature as it last existed on the battlefield, and it still had -1/-1 counters on it at that point.")
    }
}
