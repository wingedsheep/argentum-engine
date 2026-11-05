package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wasp of the Bitter End
 * {1}{B}
 * Creature — Insect Horror
 * 2/1
 * Flying
 * Whenever you cast a Bolas planeswalker spell, you may sacrifice this creature. If you do,
 * destroy target creature.
 *
 * The target is chosen as the trigger is put on the stack; the sacrifice is offered on
 * resolution, and the destroy is gated on the sacrifice actually happening (a Wasp that already
 * left the battlefield can't be sacrificed, so nothing is destroyed).
 */
val WaspOfTheBitterEnd = card("Wasp of the Bitter End") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect Horror"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhenever you cast a Bolas planeswalker spell, you may sacrifice this " +
        "creature. If you do, destroy target creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Planeswalker.withSubtype("Bolas"))
        val victim = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.IfYouDo(
                action = SacrificeSelfEffect,
                then = Effects.Destroy(victim),
                successCriterion = SuccessCriterion.PermanentsSacrificed,
            ),
            sourceRequiredZone = Zone.BATTLEFIELD,
            descriptionOverride = "sacrifice this creature. If you do, destroy target creature",
        )
        description = "Whenever you cast a Bolas planeswalker spell, you may sacrifice this " +
            "creature. If you do, destroy target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Tomasz Jedruszek"
        flavorText = "As the Second Sun glided into its final position in the sky, a chorus of " +
            "wings sounded to herald the return of Nicol Bolas."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46d348eb-7db0-4209-8960-c6adc473417d.jpg?1783935984"
        ruling(
            "2017-07-14",
            "You choose a target for Wasp of the Bitter End's triggered ability right after " +
                "casting a Bolas planeswalker spell, but you don't choose whether to sacrifice Wasp " +
                "of the Bitter End or not until that ability resolves. If the creature becomes an " +
                "illegal target, the entire ability doesn't resolve and you can't sacrifice Wasp of " +
                "the Bitter End."
        )
    }
}
