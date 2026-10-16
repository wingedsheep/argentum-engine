package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Winding Constrictor
 * {B}{G}
 * Creature — Snake
 * 2/3
 *
 * If one or more counters would be put on an artifact or creature you control, that many plus one
 * of each of those kinds of counters are put on that permanent instead.
 * If you would get one or more counters, you get that many plus one of each of those kinds of
 * counters instead.
 *
 * Two static [ModifyCounterPlacement] replacements with `counterType = null` (any kind). The engine
 * applies the modifier once per kind being placed, which is the "plus one of each of those kinds"
 * reading. Both lines gate only on the recipient ("would be put on" / "would get"), not on who
 * places the counters, so `placedByYou` stays `false`. The permanent half reads the recipient's
 * *projected* types, so a permanent made an artifact or creature on arrival is covered; neither
 * half is `Recipient.Self`, so the Constrictor never modifies its own entry counters.
 */
private val ArtifactOrCreatureYouControl =
    (GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()

val WindingConstrictor = card("Winding Constrictor") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Snake"
    power = 2
    toughness = 3
    oracleText = "If one or more counters would be put on an artifact or creature you control, " +
        "that many plus one of each of those kinds of counters are put on that permanent instead.\n" +
        "If you would get one or more counters, you get that many plus one of each of those kinds " +
        "of counters instead."

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = null,
                recipient = Recipient.Object(ArtifactOrCreatureYouControl),
            ),
        )
    )

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = null,
                recipient = Recipient.You,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/107c8aa8-c8f8-4cbf-821b-bd2cb33354f0.jpg?1783936732"
        ruling(
            "2017-09-29",
            "If a nonartifact, noncreature permanent (such as a planeswalker) would enter the battlefield " +
                "with counters on it and become an artifact or a creature on the battlefield due to another " +
                "card's effect (such as that of Mycosynth Lattice), Winding Constrictor's effect will give " +
                "that permanent another of those counters."
        )
        ruling(
            "2017-02-09",
            "If an artifact or creature you control would enter the battlefield with a number of any kind " +
                "of counters on it, it enters with that many plus one instead."
        )
        ruling(
            "2017-02-09",
            "If an effect includes multiple instructions to put one or more counters on an artifact or " +
                "creature, such as Lifecrafter's Gift does, Winding Constrictor's effect applies to each of " +
                "those instructions."
        )
        ruling(
            "2017-02-09",
            "If you control two Winding Constrictors, the number of counters placed on the artifact or " +
                "creature is the original number plus two. Three Winding Constrictors adds three to the " +
                "original number, and so on."
        )
        ruling(
            "2017-02-09",
            "If you would get counters of multiple kinds at the same time, Winding Constrictor increases " +
                "the number of each of those kinds of counters by one. The same is true if counters of " +
                "multiple kinds would be placed on an artifact or creature you control."
        )
        ruling(
            "2017-02-09",
            "Winding Constrictor's effect can't apply to itself as it's entering the battlefield or to any " +
                "other permanent entering the battlefield at the same time as it."
        )
    }
}
