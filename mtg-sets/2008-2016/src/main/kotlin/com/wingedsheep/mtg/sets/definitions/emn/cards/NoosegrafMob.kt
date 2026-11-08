package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Noosegraf Mob — Eldritch Moon #98 (canonical printing)
 * {4}{B}{B} · Creature — Zombie · 0/0
 *
 * This creature enters with five +1/+1 counters on it.
 * Whenever a player casts a spell, remove a +1/+1 counter from this creature. If you do, create a
 * 2/2 black Zombie creature token.
 *
 * The trigger watches every seat (`Triggers.anyPlayer.casts()`). The removal is mandatory and
 * exactly one counter; "if you do" gates the token on a counter actually coming off
 * (`SuccessCriterion.CountersRemoved`), so a Mob that has left the battlefield or run out of
 * +1/+1 counters by resolution makes no Zombie (ruling 2016-07-13).
 */
val NoosegrafMob = card("Noosegraf Mob") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 0
    toughness = 0
    oracleText = "This creature enters with five +1/+1 counters on it.\n" +
        "Whenever a player casts a spell, remove a +1/+1 counter from this creature. " +
        "If you do, create a 2/2 black Zombie creature token."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 5,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        effect = Effects.IfYouDo(
            action = Effects.RemoveCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                imageUri = "https://cards.scryfall.io/normal/front/1/5/15f1f43a-cbdb-4d1b-a254-dadcb2469784.jpg?1783937418"
            ),
            successCriterion = SuccessCriterion.CountersRemoved,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de44166d-3db6-4ece-985d-b8977c25e29f.jpg?1783937478"
        ruling(
            "2016-07-13",
            "If Noosegraf Mob is no longer on the battlefield or no longer has +1/+1 counters on it as its triggered ability resolves, you can't remove a counter from it and won't get a Zombie token."
        )
        ruling(
            "2016-07-13",
            "You can't remove more than one +1/+1 counter from Noosegraf Mob at once to get more Zombie tokens, and you can't choose not to remove a counter."
        )
        ruling(
            "2016-07-13",
            "Noosegraf Mob's triggered ability resolves before the spell that caused it to trigger."
        )
    }
}
