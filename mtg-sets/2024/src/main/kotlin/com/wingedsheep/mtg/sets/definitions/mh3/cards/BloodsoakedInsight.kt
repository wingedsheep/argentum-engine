package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bloodsoaked Insight {5}{B/R}{B/R} // Sanguine Morass
 * Sorcery
 * This spell costs {1} less to cast for each 1 life your opponents have lost this turn.
 * Target opponent exiles the top three cards of their library. Until the end of your next turn,
 * you may play those cards. If you cast a spell this way, mana of any type can be spent to cast it.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {B} or {R}.
 *
 * The discount is a self-cast dynamic reduction summing every opponent's life-lost-this-turn
 * total; it only reduces the generic part, so the two hybrid pips are always paid.
 */
private val BloodsoakedInsightFront = card("Bloodsoaked Insight") {
    manaCost = "{5}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast for each 1 life your opponents have lost this turn.\n" +
        "Target opponent exiles the top three cards of their library. Until the end of your next turn, " +
        "you may play those cards. If you cast a spell this way, mana of any type can be spent to cast it."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.lifeLostThisTurn(Player.EachOpponent)),
            ),
        )
    }

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(count = 3, player = opponent.asPlayer))
            exile(exiled, opponent.asPlayer)
            run(
                Effects.GrantMayPlayFromExile(
                    from = exiled,
                    expiry = MayPlayExpiry.UntilEndOfNextTurn,
                    withAnyManaType = true,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a08e0d2-1e60-47f5-9228-4c11a127089d.jpg?1783911225"
        ruling("2024-06-07", "You pay all costs and follow all normal timing rules for cards played with the permission granted by Bloodsoaked Insight. For example, if one of the exiled cards is a land card, you may play it only during your main phase while the stack is empty.")
    }
}

private val SanguineMorassBack = card("Sanguine Morass") {
    typeLine = "Land"
    colorIdentity = "BR"
    oracleText = "This land enters tapped.\n{T}: Add {B} or {R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "David Álvarez"
        flavorText = "All who enter the bog despair, for the first thing it steals is hope."
        imageUri = "https://cards.scryfall.io/normal/back/0/a/0a08e0d2-1e60-47f5-9228-4c11a127089d.jpg?1783911225"
    }
}

val BloodsoakedInsight: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = BloodsoakedInsightFront,
    backFace = SanguineMorassBack,
)
