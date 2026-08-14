package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Huntsman's Redemption
 * {2}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 3/3 green Beast creature token.
 * II — You may sacrifice a creature. If you do, search your library for a creature or basic land
 *   card, reveal it, put it into your hand, then shuffle.
 * III — Up to two target creatures each get +2/+2 and gain trample until end of turn.
 *
 * Chapter II is the "you may X. If you do, Y" idiom (cf. Witherbloom Charm): a [MayEffect] for the
 * "you may", wrapping an [IfYouDoEffect] whose action is a gather → choose-one → sacrifice pipeline
 * over the creatures you control, gating the tutor on the sacrifice actually happening. The
 * [FeasibilityCheck] suppresses the yes/no prompt entirely when you control no creature — the
 * chapter triggers on its own every turn, so an unanswerable prompt would be pure noise. The Beast
 * token from chapter I is the natural thing to feed it. Note the choose step is *not* a target: the
 * creature is picked as the chapter ability resolves, so nothing is locked in when it triggers.
 *
 * The tutor's "creature or basic land card" is a plain filter union, and `reveal = true` mirrors the
 * printed "reveal it" (the search is not optional, so declining to find simply shuffles).
 *
 * Chapter III fans an optional two-target requirement out with [ForEachTargetEffect] so each chosen
 * creature gets its own +2/+2 and its own trample grant — "up to two", so zero or one target is
 * legal and the chapter still resolves.
 */
val TheHuntsmansRedemption = card("The Huntsman's Redemption") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 3/3 green Beast creature token.\n" +
        "II — You may sacrifice a creature. If you do, search your library for a creature or basic " +
        "land card, reveal it, put it into your hand, then shuffle.\n" +
        "III — Up to two target creatures each get +2/+2 and gain trample until end of turn."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/a/2/a2d33cf3-c803-4e90-9d4b-fa34136b600e.jpg?1783914991",
        )
    }

    sagaChapter(2) {
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = Effects.Pipeline {
                    val creatures = gather(GameObjectFilter.Creature, player = Player.You)
                    val chosen = chooseExactly(
                        1,
                        from = creatures,
                        useTargetingUI = true,
                        prompt = "Choose a creature to sacrifice",
                    )
                    sacrifice(chosen)
                },
                ifYouDo = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Creature or GameObjectFilter.BasicLand,
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                ),
            ),
            descriptionOverride = "You may sacrifice a creature. If you do, search your library for a " +
                "creature or basic land card, reveal it, put it into your hand, then shuffle.",
            feasibility = FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.Creature.youControl()),
        )
    }

    sagaChapter(3) {
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = ForEachTargetEffect(
            listOf(
                Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27003577-e276-4ad5-b3e9-8523b166ad49.jpg?1783915080"
    }
}
