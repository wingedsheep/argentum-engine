package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brain in a Jar — Shadows over Innistrad #252
 * {2} · Artifact
 *
 * {1}, {T}: Put a charge counter on this artifact, then you may cast an instant or sorcery spell
 * with mana value equal to the number of charge counters on this artifact from your hand without
 * paying its mana cost.
 * {3}, {T}, Remove X charge counters from this artifact: Scry X.
 *
 * - The first ability adds the counter first, then runs the Sram's Expertise / Twinning Glass
 *   free-cast hand pipeline, so the new counter is counted (ruling 2016-04-08). The mana-value
 *   match is Instrument of the Bards' `manaValueEqualsDynamic(countersOnSelf(...))`, read as the
 *   pipeline gathers. If the Jar has left the battlefield, the counter can't be placed and the
 *   self counter count falls back to its last-known counters (the second 2016-04-08 ruling).
 *   `chooseUpTo(1)` is the "you may"; the spell is cast during resolution, so timing is ignored.
 * - The second ability's X is paid off the Jar's own charge counters
 *   (`RemoveXCounters(self = true)`, as on Geistflame Reservoir) and read back by the dynamic Scry.
 */
val BrainInAJar = card("Brain in a Jar") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Put a charge counter on this artifact, then you may cast an instant or sorcery " +
        "spell with mana value equal to the number of charge counters on this artifact from your hand " +
        "without paying its mana cost.\n" +
        "{3}, {T}, Remove X charge counters from this artifact: Scry X."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self) then Effects.Pipeline {
            val candidates = gather(
                CardSource.FromZone(
                    Zone.HAND,
                    Player.You,
                    filter = GameObjectFilter.InstantOrSorcery.manaValueEqualsDynamic(
                        DynamicAmounts.countersOnSelf(CounterType.CHARGE)
                    )
                )
            )
            val chosen = chooseUpTo(
                1,
                from = candidates,
                prompt = "You may cast an instant or sorcery with mana value equal to the charge counters on Brain in a Jar",
                selectedLabel = "Cast without paying its mana cost"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
        description = "{1}, {T}: Put a charge counter on Brain in a Jar, then you may cast an instant or " +
            "sorcery with mana value equal to its charge counters from your hand for free"
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.CHARGE, self = true),
        )
        effect = Effects.Scry(DynamicAmounts.xValue())
        description = "{3}, {T}, Remove X charge counters from Brain in a Jar: Scry X."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88ecfcbe-e8db-4f08-aa8b-5b7b3e6c6ce7.jpg?1783937708"
        ruling(
            "2016-04-08",
            "When resolving the first ability of Brain in a Jar, the newly-placed charge counter will be counted " +
                "when determining what spells you may cast. No player may take any action between you placing the " +
                "counter and choosing which spell to cast."
        )
        ruling(
            "2016-04-08",
            "If Brain in a Jar leaves the battlefield before its first ability resolves, use the number of " +
                "counters on it at the moment it left to determine what spell you may cast. That number won't " +
                "change because you can't put a new counter on Brain in a Jar."
        )
        ruling(
            "2016-04-08",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs, such as awaken costs. You can, however, pay additional costs, such as kicker costs. If the " +
                "card has any mandatory additional costs, such as that of Lightning Axe, you must pay those to cast the card."
        )
        ruling("2016-04-08", "If the card has {X} in its mana cost, you must choose 0 as the value of X.")
    }
}
