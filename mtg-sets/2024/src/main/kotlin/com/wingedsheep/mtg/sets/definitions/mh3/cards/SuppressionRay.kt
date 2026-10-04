package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Suppression Ray {3}{W/U}{W/U} // Orderly Plaza
 * Sorcery
 * Tap all creatures target player controls. You may pay any amount of {E}. If you do, choose up
 * to that many creatures tapped this way. Put a stun counter on each of them.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {W} or {U}.
 *
 * "Creatures tapped this way" are only the ones that were untapped as the spell began to resolve
 * (2024-06-07 ruling), so the pipeline gathers the target player's *untapped* creatures, taps that
 * collection, and later chooses stun recipients from it. The {E} payment is a resolution-time
 * "any amount" ([Effects.PayCounters]); the paid number caps the up-to choice, so paying zero (or
 * having no energy) puts no stun counters anywhere.
 */
private val SuppressionRayFront = card("Suppression Ray") {
    manaCost = "{3}{W/U}{W/U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Tap all creatures target player controls. You may pay any amount of {E}. If you do, " +
        "choose up to that many creatures tapped this way. Put a stun counter on each of them. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)"

    spell {
        target(Targets.Player)
        effect = Effects.Pipeline {
            val tappedThisWay = gather(
                GameObjectFilter.Creature.untapped(),
                player = Player.TargetPlayer,
            )
            run(Effects.TapCollection(tappedThisWay))
            val paid = runStoringNumber { Effects.PayCounters(CounterType.ENERGY, storeAmountAs = it) }
            // "If you do" — paying zero {E} is not paying, so no (empty) choice is offered.
            run(Effects.If(
                condition = Conditions.CompareAmounts(paid.amount, ComparisonOperator.GTE, 1),
                then = Effects.Pipeline {
                    val stunned = chooseUpTo(
                        paid.amount,
                        from = tappedThisWay,
                        useTargetingUI = true,
                        prompt = "Choose creatures tapped this way to put a stun counter on",
                        selectedLabel = "Stun",
                    )
                    run(Effects.AddCountersToCollection(stunned, CounterType.STUN, 1))
                }
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Gabor Szikszai"
        flavorText = "The Consulate's aether spotlights have a setting to incapacitate trespassers on contact."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cccd328-457a-48ab-97fb-4bc319db2e60.jpg?1783911225"
        ruling("2024-06-07", "\"Creatures tapped this way\" means creatures that became tapped as a result of Suppression Ray's effect, not creatures that were already tapped before Suppression Ray began to resolve. For example, if a player controls two tapped creatures and one untapped creature and you cast Suppression Ray targeting that player, you'll only be able to choose the creature that was untapped to get a stun counter (as long as you paid at least one {E}).")
    }
}

private val OrderlyPlazaBack = card("Orderly Plaza") {
    typeLine = "Land"
    colorIdentity = "WU"
    oracleText = "This land enters tapped.\n{T}: Add {W} or {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Gabor Szikszai"
        flavorText = "\"Chaos fuels the flames of violence. Only through order can peace be assured.\"\n—Dovin Baan"
        imageUri = "https://cards.scryfall.io/normal/back/0/c/0cccd328-457a-48ab-97fb-4bc319db2e60.jpg?1783911225"
    }
}

val SuppressionRay: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SuppressionRayFront,
    backFace = OrderlyPlazaBack,
)
