package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skyrider Patrol
 * {2}{G}{U}
 * Creature — Elf Scout
 * 2/3
 * Flying
 * At the beginning of combat on your turn, you may pay {G}{U}. When you do, put a +1/+1 counter
 *   on another target creature you control, and that creature gains flying until end of turn.
 *
 * The combat trigger is the Passenger Ferry reflexive shape: the optional {G}{U} payment is the
 * action, and the reflexive ability — which targets another creature you control, chosen as it
 * goes on the stack — only fires if the payment is made.
 */
val SkyriderPatrol = card("Skyrider Patrol") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "At the beginning of combat on your turn, you may pay {G}{U}. When you do, put a +1/+1 " +
        "counter on another target creature you control, and that creature gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ReflexiveTrigger(
            // "you may pay {G}{U}"
            action = Effects.PayMana("{G}{U}"),
            optional = true) {
            // "When you do, put a +1/+1 counter on another target creature you control,
            //  and that creature gains flying until end of turn."
            val creature = target(TargetFilter.CreatureYouControl.other())
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                Effects.GrantKeyword(Keyword.FLYING, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Mitchell Malloy"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/650461af-081b-4cc9-a140-865a666f1443.jpg?1783934518"
    }
}
