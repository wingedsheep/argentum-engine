package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rescuer Sphinx
 * {2}{U}{U}
 * Creature — Sphinx
 * 3/2
 * Flying
 * As this creature enters, you may return a nonland permanent you control to its owner's hand.
 * If you do, this creature enters with a +1/+1 counter on it.
 *
 * An [EntersWithChoice] replacement: the permanent is picked while the Sphinx is entering, from
 * the nonland permanents you already control (the entering Sphinx is never a candidate, nor is
 * anything entering alongside it), and declining is allowed. The `then` rider runs as part of
 * the same entry — before state-based actions, trigger checks and priority — so nobody can act
 * between the pick and the Sphinx standing there with its counter, and a returned permanent's
 * "whenever a creature enters" ability never sees the Sphinx arrive. "If you do" is tracked: the
 * counter is placed only if the chosen permanent actually reached its owner's hand.
 */
val RescuerSphinx = card("Rescuer Sphinx") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 3
    toughness = 2
    oracleText = "Flying\nAs this creature enters, you may return a nonland permanent you control to its " +
        "owner's hand. If you do, this creature enters with a +1/+1 counter on it."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithChoice(
            ChoiceType.CREATURE_ON_BATTLEFIELD,
            permanentFilter = GameObjectFilter.NonlandPermanent.youControl(),
            mayChooseNothing = true,
            then = Effects.Pipeline(
                descriptionOverride = "Return it to its owner's hand. If you do, this creature enters with a +1/+1 counter on it",
            ) {
                val chosen = gather(CardSource.Entity(EffectTarget.ChosenCreature))
                val returned = moveTracked(chosen, CardDestination.ToZone(Zone.HAND))
                ifNotEmpty(returned) {
                    run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
                }
            },
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Jesper Ejsing"
        flavorText = "\"Now, little fighter, show me you deserved this second chance.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00ba9fa2-bfd2-4dc7-b96d-d22a8886a6ae.jpg?1783933459"
        ruling(
            "2019-05-03",
            "You choose whether to return a nonland permanent and which one to return for Rescuer Sphinx's " +
                "ability while it's entering the battlefield. No player may take actions between the time you " +
                "choose which permanent to return and the time Rescuer Sphinx is on the battlefield with a " +
                "+1/+1 counter."
        )
        ruling(
            "2019-05-03",
            "If the returned permanent had an ability that would otherwise have triggered when Rescuer Sphinx " +
                "enters the battlefield (such as that of Kronch Wrangler), that ability won't trigger."
        )
        ruling(
            "2019-05-03",
            "If Rescuer Sphinx somehow enters the battlefield at the same time as another permanent, you can't " +
                "return that permanent. You may choose only permanents that are already on the battlefield."
        )
    }
}
