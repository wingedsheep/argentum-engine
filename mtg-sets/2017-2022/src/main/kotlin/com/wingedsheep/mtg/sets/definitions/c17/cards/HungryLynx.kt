package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hungry Lynx — Commander 2017 #31
 * {1}{G} · Creature — Cat · 2/2
 *
 * Cats you control have protection from Rats.
 * At the beginning of your end step, target opponent creates a 1/1 black Rat creature token with
 * deathtouch.
 * Whenever a Rat dies, put a +1/+1 counter on each Cat you control.
 *
 * Protection from Rats is the projected `PROTECTION_FROM_SUBTYPE_RAT` keyword (the Feline Sovereign
 * shape), granted to every Cat you control including Hungry Lynx itself. The dies trigger watches
 * every Rat, not only the opponents' tokens, and the counters go on the Cats still on the
 * battlefield as it resolves (a Lynx dying alongside the Rat still triggers via look-back, but gets
 * no counter).
 */
private val catsYouControl = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.CAT).youControl())

private const val PROTECTION_FROM_RATS = "PROTECTION_FROM_SUBTYPE_RAT"

val HungryLynx = card("Hungry Lynx") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2
    oracleText = "Cats you control have protection from Rats. (They can't be blocked, targeted, or dealt damage by Rats.)\n" +
        "At the beginning of your end step, target opponent creates a 1/1 black Rat creature token with deathtouch.\n" +
        "Whenever a Rat dies, put a +1/+1 counter on each Cat you control."

    staticAbility {
        ability = GrantKeyword(PROTECTION_FROM_RATS, catsYouControl)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rat"),
            keywords = setOf(Keyword.DEATHTOUCH),
            controller = opponent,
            imageUri = "https://cards.scryfall.io/normal/front/c/1/c1788d4c-31b8-4f73-8275-5470316e7ad4.jpg?1783935828",
        )
        description = "At the beginning of your end step, target opponent creates a 1/1 black Rat creature " +
            "token with deathtouch."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.RAT)).dies()
        effect = Effects.ForEachInGroup(
            catsYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "Whenever a Rat dies, put a +1/+1 counter on each Cat you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Shreya Shetty"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0ad6f84-8ede-4411-9979-cefee255fa70.jpg?1783935941"
        ruling("2017-08-25", "The opponent who creates the Rat token is the owner of that token.")
        ruling(
            "2017-08-25",
            "Tokens that are sacrificed or destroyed are put into their owner's graveyard before ceasing to " +
                "exist. If the token was a Rat, Hungry Lynx's last ability will trigger."
        )
        ruling(
            "2017-08-25",
            "Hungry Lynx's last ability puts a +1/+1 counter on each Cat you control, including itself."
        )
        ruling(
            "2017-08-25",
            "If Hungry Lynx dies at the same time as a Rat, its ability triggers, but Hungry Lynx won't be on " +
                "the battlefield as that ability resolves. It can't be saved by the +1/+1 counter that would " +
                "have been put on it. The same is true of any other Cats you control that die at the same " +
                "time. Your remaining Cats will each get a +1/+1 counter."
        )
    }
}
