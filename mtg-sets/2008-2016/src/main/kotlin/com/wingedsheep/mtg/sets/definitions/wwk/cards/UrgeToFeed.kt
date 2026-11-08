package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urge to Feed — Worldwake #70 (canonical printing)
 * {B}{B}
 * Instant
 * Target creature gets -3/-3 until end of turn. You may tap any number of untapped Vampire creatures
 * you control. If you do, put a +1/+1 counter on each of those Vampires.
 *
 * Harmony of Nature's shape: the untapped Vampires are gathered and chosen as the spell resolves
 * (2010-03-01 ruling), "any number" admits zero (declining the "may" — no counters), and the
 * counters go on exactly the creatures tapped this way. The -3/-3 applies first and state-based
 * actions wait for the whole spell, so a targeted 3-toughness Vampire of your own can be tapped and
 * countered back to 1 toughness. An illegal target fizzles the whole spell, so no Vampires are tapped.
 */
val UrgeToFeed = card("Urge to Feed") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-3 until end of turn. You may tap any number of untapped " +
        "Vampire creatures you control. If you do, put a +1/+1 counter on each of those Vampires."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            run(Effects.ModifyStats(-3, -3, t))
            val candidates = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = candidates,
                prompt = "You may tap any number of untapped Vampires you control to put a +1/+1 counter on each",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.AddCountersToCollection(tapped, CounterType.PLUS_ONE_PLUS_ONE))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b388d4d9-62b6-4174-897e-f933a4badcf9.jpg?1783942054"
        ruling(
            "2010-03-01",
            "You don't choose which untapped Vampire creatures you control to tap (if any) until Urge to Feed resolves."
        )
        ruling(
            "2010-03-01",
            "If you cast Urge to Feed targeting an untapped Vampire creature you control with toughness 3, its " +
                "toughness will be reduced to 0 by the -3/-3 effect. However, state-based actions aren't checked " +
                "until the spell has finished resolving. You may then tap that Vampire and put a +1/+1 counter on " +
                "it, raising its toughness to 1. It will remain on the battlefield."
        )
        ruling(
            "2010-03-01",
            "If the targeted creature is an illegal target by the time Urge to Feed resolves, the entire spell " +
                "doesn't resolve. You can't tap any Vampires."
        )
    }
}
