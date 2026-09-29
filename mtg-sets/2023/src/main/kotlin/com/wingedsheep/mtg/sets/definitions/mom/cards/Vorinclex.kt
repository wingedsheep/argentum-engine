package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vorinclex // The Grand Evolution — March of the Machine #213
 * {3}{G}{G} · Legendary Creature — Phyrexian Praetor 6/6 // Enchantment — Saga
 *
 * Chapter I mills ten and picks up to two creature cards *from among the milled cards* — the
 * pipeline's milled collection, not the whole graveyard. Chapter II is a divided-counter trigger:
 * zero to seven targets (each target needs at least one counter, CR 601.2d), the split announced
 * as the chapter ability goes on the stack. Chapter III grants the fight ability to the creatures
 * you control as it resolves (the set is locked then — Vorinclex itself is not back yet), then the
 * Saga exiles itself and returns front face up.
 */
private val TheGrandEvolution = card("The Grand Evolution") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Mill ten cards. Put up to two creature cards from among the milled cards onto the battlefield.\n" +
        "II — Distribute seven +1/+1 counters among any number of target creatures you control.\n" +
        "III — Until end of turn, creatures you control gain \"{1}: This creature fights target " +
        "creature you don't control.\" Exile this Saga, then return it to the battlefield (front face up)."

    // I — Mill ten cards. Put up to two creature cards from among the milled cards onto the battlefield.
    sagaChapter(1) {
        effect = Effects.Pipeline {
            val milled = mill(10)
            val creatures = chooseUpTo(
                2,
                from = milled,
                filter = GameObjectFilter.Creature,
                showAllCards = true,
                prompt = "Put up to two creature cards from among the milled cards onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave in graveyard"
            )
            move(creatures, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    // II — Distribute seven +1/+1 counters among any number of target creatures you control.
    sagaChapter(2) {
        targets(TargetFilter.CreatureYouControl, count = 7, minCount = 0)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 7)
    }

    // III — creatures you control gain the fight ability this turn; then flip back to Vorinclex.
    sagaChapter(3) {
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{1}")
                val victim = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.Fight(EffectTarget.Self, victim)
                description = "This creature fights target creature you don't control."
            },
            filter = GroupFilter.AllCreaturesYouControl,
            duration = Duration.EndOfTurn
        ) then Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/back/e/2/e24b5289-d3b5-4b4d-bb37-69bf2c3b48bc.jpg?1783916965"
        ruling(
            "2023-04-14",
            "You choose how many targets the chapter II ability of The Grand Evolution has and how the " +
                "counters are distributed as you put the ability onto the stack. Each target must receive " +
                "at least one counter."
        )
        ruling(
            "2023-04-14",
            "If some of the creatures are illegal targets as the chapter II ability tries to resolve, the " +
                "original distribution of counters still applies and the counters that would have been put " +
                "on the illegal targets are lost. They won't be put instead on a legal target."
        )
        ruling(
            "2023-04-14",
            "The set of creatures that gain the activated fight ability is determined as the chapter III " +
                "ability resolves. Creatures you begin to control later in the turn and noncreature " +
                "permanents that become creatures later in the turn won't gain that ability. Notably, " +
                "Vorinclex won't be back on the battlefield yet, so it won't gain the ability either."
        )
    }
}

private val VorinclexFront = card("Vorinclex") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    oracleText = "Reach, trample\n" +
        "When Vorinclex enters, search your library for up to two Forest cards, reveal them, put them " +
        "into your hand, then shuffle.\n" +
        "{6}{G}{G}: Exile Vorinclex, then return it to the battlefield transformed under its owner's " +
        "control. Activate only as a sorcery."
    power = 6
    toughness = 6
    keywords(Keyword.REACH, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype("Forest"),
            count = 2,
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{6}{G}{G}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e24b5289-d3b5-4b4d-bb37-69bf2c3b48bc.jpg?1783916965"
        ruling(
            "2023-04-14",
            "The Forest cards you find can be any land cards with the Forest land type, not just ones " +
                "named Forest."
        )
    }
}

val Vorinclex: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = VorinclexFront,
    backFace = TheGrandEvolution,
)
