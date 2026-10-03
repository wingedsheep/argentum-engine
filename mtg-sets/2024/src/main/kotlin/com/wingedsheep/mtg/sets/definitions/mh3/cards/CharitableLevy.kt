package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charitable Levy — Modern Horizons 3 #21
 * {1}{W} · Enchantment · Uncommon
 *
 * Noncreature spells cost {1} more to cast.
 * Whenever a player casts a noncreature spell, put a collection counter on this enchantment. Then
 * if there are three or more collection counters on it, sacrifice it. If you do, draw a card, then
 * you may search your library for a Plains card, put it onto the battlefield tapped, then shuffle.
 *
 * The tax is the symmetric Thalia/Glowrider [ModifySpellCost]. The counter check is part of the
 * cast trigger's own resolution ("Then if …"), not a separate state trigger, so it is an
 * [Effects.If] after the counter is added. The sacrifice is gated with [Effects.IfYouDo] on
 * [SuccessCriterion.PermanentsSacrificed]: `SacrificeTargetEffect` refuses when the resolving
 * player no longer controls the enchantment, which is exactly the third ruling. "A Plains card"
 * is the Plains *subtype* (dual lands with the type qualify), and the optional search is
 * [Effects.May] around the search pattern, so declining means no search and no shuffle.
 */
val CharitableLevy = card("Charitable Levy") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Noncreature spells cost {1} more to cast.\n" +
        "Whenever a player casts a noncreature spell, put a collection counter on this enchantment. " +
        "Then if there are three or more collection counters on it, sacrifice it. If you do, draw a " +
        "card, then you may search your library for a Plains card, put it onto the battlefield " +
        "tapped, then shuffle."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Noncreature),
            modification = CostModification.IncreaseGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.COLLECTION, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.COLLECTION, 3),
                then = Effects.IfYouDo(
                    action = Effects.SacrificeTarget(EffectTarget.Self),
                    then = Effects.DrawCards(1) then Effects.May(
                        Patterns.Library.searchLibrary(
                            filter = Filters.PlainsCard,
                            count = 1,
                            destination = SearchDestination.BATTLEFIELD,
                            entersTapped = true
                        )
                    ),
                    successCriterion = SuccessCriterion.PermanentsSacrificed,
                )
            )
        description = "Whenever a player casts a noncreature spell, put a collection counter on this " +
            "enchantment. Then if there are three or more collection counters on it, sacrifice it. If " +
            "you do, draw a card, then you may search your library for a Plains card, put it onto the " +
            "battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a5cab75-546c-4760-97b7-4591de9e6662.jpg?1783911303"
        ruling(
            "2024-06-07",
            "Charitable Levy's triggered ability resolves before the spell that causes it to trigger. " +
                "The ability will resolve even if that spell is countered or otherwise leaves the stack."
        )
        ruling(
            "2024-06-07",
            "In the rare case where the player who controlled Charitable Levy as its last ability " +
                "triggered doesn't control it as it resolves, that player won't be able to sacrifice " +
                "Charitable Levy even if it has three or more collection counters on it."
        )
    }
}
