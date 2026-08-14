package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Doctor Doom
 * {4}{B}{B}
 * Legendary Creature — Human Scientist Villain
 * 3/3
 *
 * When Doctor Doom enters, create two 3/3 colorless Robot Villain artifact creature tokens
 * named Doombot.
 * As long as you control an artifact creature or a Plan, Doctor Doom has indestructible.
 * At the beginning of your end step, you draw a card and lose 1 life.
 *
 * The Doombots are a *named* token, so they come from the registered `PredefinedTokens.Doombot`
 * definition via [CreatePredefinedTokenEffect] rather than being re-spelled inline.
 *
 * "As long as you control an artifact creature or a Plan" is one [ConditionalStaticAbility] over a
 * single OR'd battlefield filter — both branches share the you-control gate, so the filter
 * collapses to one `CardPredicate.Or` and the whole clause stays a single Layer 6 effect that the
 * projector re-evaluates as the board changes. `Plan` is an MSH enchantment subtype; `Subtype` is
 * free-form, so no new vocabulary is needed for it.
 *
 * The end-step clause is *not* a drain: "you draw a card and lose 1 life" is a plain draw followed
 * by the controller losing life (not target opponent, the [Effects.LoseLife] default).
 */
val DoctorDoom = card("Doctor Doom") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Scientist Villain"
    power = 3
    toughness = 3
    oracleText = "When Doctor Doom enters, create two 3/3 colorless Robot Villain artifact " +
        "creature tokens named Doombot.\n" +
        "As long as you control an artifact creature or a Plan, Doctor Doom has indestructible.\n" +
        "At the beginning of your end step, you draw a card and lose 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreatePredefinedTokenEffect("Doombot", count = 2)
        description = "When Doctor Doom enters, create two 3/3 colorless Robot Villain artifact " +
            "creature tokens named Doombot."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.ArtifactCreature or GameObjectFilter.Permanent.withSubtype("Plan"),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "At the beginning of your end step, you draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "95"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b1f213a-e1d4-4a0f-954b-c83915698d98.jpg?1783902944"
    }
}
