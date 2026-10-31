package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oko, the Trickster — Throne of Eldraine #309 (canonical printing; Brawl deck)
 * {4}{G}{U} · Legendary Planeswalker — Oko · Loyalty 4
 *
 * +1: Put two +1/+1 counters on up to one target creature you control.
 * 0: Until end of turn, Oko becomes a copy of target creature you control. Prevent all damage that
 * would be dealt to him this turn.
 * −7: Until end of turn, each creature you control has base power and toughness 10/10 and gains
 * trample.
 *
 * The 0 is Mirage Mirror's single-permanent copy shape (`affected = Self`, [Duration.EndOfTurn]):
 * only copiable values change, so Oko keeps his loyalty counters but stops being a planeswalker
 * while he's a copy, and reverts at cleanup. The prevention half is Gideon, Champion of Justice's
 * `PreventDamage(target = Self)` shield, keyed to the same permanent whatever it currently copies.
 * The −7 iterates the creatures you control at resolution ([Effects.ForEachInGroup]), so creatures
 * you gain later in the turn aren't affected (ruling).
 */
val OkoTheTrickster = card("Oko, the Trickster") {
    manaCost = "{4}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Oko"
    startingLoyalty = 4
    oracleText = "+1: Put two +1/+1 counters on up to one target creature you control.\n" +
        "0: Until end of turn, Oko becomes a copy of target creature you control. Prevent all " +
        "damage that would be dealt to him this turn.\n" +
        "−7: Until end of turn, each creature you control has base power and toughness 10/10 " +
        "and gains trample."

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.CreatureYouControl, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }

    loyaltyAbility(0) {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = creature,
            affected = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        ) then Effects.PreventDamage(target = EffectTarget.Self)
    }

    loyaltyAbility(-7) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.SetBasePowerAndToughness(10, 10, EffectTarget.IterationEntity, Duration.EndOfTurn) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity, Duration.EndOfTurn)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "309"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83e61813-c4c8-4e80-8808-ac5107966ee3.jpg?1783932554"
        ruling("2019-10-04", "If Oko becomes a creature the same turn he enters the battlefield, you can't attack with him or use any of his {T} abilities (if he gains any).")
        ruling("2019-10-04", "While Oko is a creature, he isn't a planeswalker. Damage dealt to him won't cause him to lose loyalty.")
        ruling("2019-10-04", "If the target creature is an illegal target by the time Oko's second ability tries to resolve, the ability won't resolve. Damage that would be dealt to Oko won't be prevented.")
        ruling("2019-10-04", "Oko copies the printed values of the target creature. It won't copy counters on that creature or effects that have changed its power, toughness, types, color, or so on. Notably, it won't copy effects that made the target creature become a creature if it isn't normally a creature.")
        ruling("2019-10-04", "If the target creature is copying something else, then Oko becomes a copy of whatever that creature copied.")
        ruling("2019-10-04", "Oko's last ability affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't be 10/10.")
        ruling("2019-10-04", "Any effects that modify a creature's power and/or toughness without setting them to a specific number or value will apply after its base power and toughness are set, regardless of the order in which those effects were created. The same is true of counters that modify its power and toughness.")
    }
}
