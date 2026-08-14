package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kickoff Celebrations — Aetherdrift #135
 * {1}{R} · Enchantment
 *
 * Start your engines!
 * When this enchantment enters, you may discard a card. If you do, draw two cards.
 * Max speed — Sacrifice this enchantment: Creatures and Vehicles you control gain haste until end
 * of turn.
 *
 * The ETB is the standard "you may X. If you do, Y" nesting: [MayEffect] is the optional outer
 * wrapper (declining does nothing at all), [IfYouDoEffect] gates the draw on the discard actually
 * happening — so saying yes with an empty hand discards nothing and draws nothing.
 *
 * "Creatures and Vehicles you control" is one union filter rather than two grants, and it is
 * deliberately not narrowed to creatures: an uncrewed Vehicle is a noncreature artifact that still
 * picks up haste here, so crewing it later this turn gives a permanent that can attack immediately.
 */
val KickoffCelebrations = card("Kickoff Celebrations") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Start your engines!\n" +
        "When this enchantment enters, you may discard a card. If you do, draw two cards.\n" +
        "Max speed — Sacrifice this enchantment: Creatures and Vehicles you control gain haste " +
        "until end of turn."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = Patterns.Hand.discardCards(1),
                ifYouDo = Effects.DrawCards(2)
            )
        )
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.ForEachInGroup(
                GroupFilter(
                    (GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype("Vehicle")).youControl()
                ),
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
            )
            description = "Creatures and Vehicles you control gain haste until end of turn."
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e5590e1-0ac0-4bdd-815b-136bf24ced03.jpg?1783907880"
        ruling(
            "2025-02-07",
            "Start your engines! isn't a triggered ability. Increasing your speed to 1 is something " +
                "that happens as a state-based action as soon as you control a permanent with the " +
                "ability. Notably, this includes gaining control of a permanent with the ability that " +
                "another player controls."
        )
        ruling(
            "2025-02-07",
            "Your speed doesn't change until a spell or ability says so, such as the inherent " +
                "triggered ability that cares about opponents losing life during your turn. Notably, " +
                "losing control of permanents with start your engines! doesn't affect your speed."
        )
        ruling("2025-02-07", "A player “has max speed” if their speed is 4.")
    }
}
