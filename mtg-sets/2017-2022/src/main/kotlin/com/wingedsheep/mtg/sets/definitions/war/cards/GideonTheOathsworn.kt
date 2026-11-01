package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gideon, the Oathsworn
 * {4}{W}{W}
 * Legendary Planeswalker — Gideon
 * Loyalty 4
 *
 * Whenever you attack with two or more non-Gideon creatures, put a +1/+1 counter on each of those
 * creatures.
 * +2: Until end of turn, Gideon becomes a 5/5 white Soldier creature that's still a planeswalker.
 *     Prevent all damage that would be dealt to him this turn.
 * −9: Exile Gideon and each creature your opponents control.
 *
 * The static trigger is a `YouAttackEvent` counting only declared attackers that aren't Gideons
 * (ruling: a Gideon creature-planeswalker neither counts toward the two nor gets a counter). The
 * firing captures exactly those attackers; "each of those creatures" is that captured collection,
 * narrowed to the ones still on the battlefield as it resolves.
 *
 * The +2 adds the creature type and the Soldier subtype without removing planeswalker, so Gideon
 * keeps his planeswalker subtype and loyalty (the Gideon, Champion of Justice shape).
 *
 * The −9 gathers Gideon together with every creature an opponent controls and exiles them in one
 * move. With exactly 9 loyalty Gideon is put into the graveyard as a state-based action before the
 * ability resolves, so only the opposing creatures are exiled (ruling).
 */
val GideonTheOathsworn = card("Gideon, the Oathsworn") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 4
    oracleText = "Whenever you attack with two or more non-Gideon creatures, put a +1/+1 counter on " +
        "each of those creatures.\n" +
        "+2: Until end of turn, Gideon becomes a 5/5 white Soldier creature that's still a " +
        "planeswalker. Prevent all damage that would be dealt to him this turn. (He can't attack if " +
        "he was cast this turn.)\n" +
        "−9: Exile Gideon and each creature your opponents control."

    // Whenever you attack with two or more non-Gideon creatures, put a +1/+1 counter on each of
    // those creatures.
    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Creature.notSubtype(Subtype("Gideon")),
            minAttackers = 2
        )
        effect = Effects.Pipeline {
            val stillHere = filter(triggerCaptured, GameObjectFilter.Any.onBattlefield())
            run(Effects.AddCountersToCollection(stillHere, CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
    }

    // +2: Until end of turn, Gideon becomes a 5/5 white Soldier creature that's still a
    //     planeswalker. Prevent all damage that would be dealt to him this turn.
    loyaltyAbility(+2) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 5,
            creatureTypes = setOf("Soldier"),
            colors = setOf("WHITE"),
            duration = Duration.EndOfTurn
        ) then Effects.PreventDamage(target = EffectTarget.Self)
    }

    // −9: Exile Gideon and each creature your opponents control.
    loyaltyAbility(-9) {
        effect = Effects.Pipeline {
            val exiled = gather(
                GameObjectFilter.Creature.opponentControls() or GameObjectFilter.Any.sourceItself()
            )
            exile(exiled)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "265"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43e7331d-98b8-420f-a346-3f3521c3b258.jpg?1783933361"
        inBooster = false
        ruling("2019-05-03", "A non-Gideon creature is a creature that isn't also a Gideon planeswalker. For example, Gideon's Company is a non-Gideon creature.")
        ruling("2019-05-03", "Any attacking Gideon creatures won't get a +1/+1 counter, but as long as two or more attacking creatures aren't Gideons, those attacking creatures each get a +1/+1 counter.")
        ruling("2019-05-03", "Gideon's first loyalty ability doesn't count as a creature entering the battlefield. Gideon was already on the battlefield; he only changed his types.")
        ruling("2019-05-03", "If Gideon becomes a creature the same turn he enters the battlefield, you can't attack with him or use any of his {T} abilities (if he gains any).")
        ruling("2019-05-03", "Gideon's first loyalty ability causes him to become a creature with the creature type Soldier. He remains a planeswalker with the planeswalker type Gideon. (He also retains any other card types or subtypes he may have had.) Each subtype is correlated to the proper card type: planeswalker is only a type (not a creature type), and Soldier is just a creature type (not a planeswalker type).")
        ruling("2019-05-03", "If damage that can't be prevented is dealt to Gideon after his first loyalty ability has resolved, that damage will have all applicable results: specifically, the damage is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters to be removed from him (since he's a planeswalker). Even though he's also a creature, if Gideon has no loyalty counters on him, he's put into his owner's graveyard.")
        ruling("2019-05-03", "If Gideon has 9 loyalty as you activate his last ability, he'll be put into your graveyard and won't be exiled.")
    }
}
