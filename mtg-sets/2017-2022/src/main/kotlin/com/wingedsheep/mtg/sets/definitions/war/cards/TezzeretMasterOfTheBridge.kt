package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tezzeret, Master of the Bridge — War of the Spark #275
 * {4}{U}{B} · Legendary Planeswalker — Tezzeret · Loyalty 5
 *
 * Creature and planeswalker spells you cast have affinity for artifacts. (They cost {1} less to
 * cast for each artifact you control.)
 * +2: Tezzeret deals X damage to each opponent, where X is the number of artifacts you control.
 *     You gain X life.
 * −3: Return target artifact card from your graveyard to your hand.
 * −8: Exile the top ten cards of your library. Put all artifact cards from among them onto the
 *     battlefield.
 *
 * Granted affinity is the generic reduction `ReduceGenericBy(ArtifactsYouControl)` on every creature
 * or planeswalker spell you cast (Pearl-Ear's "affinity for Auras" shape); it reduces generic mana
 * only and applies after any cost increases, as the rulings require. Tezzeret's own spell doesn't
 * get it — the static only functions once he's on the battlefield.
 */
val TezzeretMasterOfTheBridge = card("Tezzeret, Master of the Bridge") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 5
    oracleText = "Creature and planeswalker spells you cast have affinity for artifacts. (They cost {1} less to cast for each artifact you control.)\n" +
        "+2: Tezzeret deals X damage to each opponent, where X is the number of artifacts you control. You gain X life.\n" +
        "−3: Return target artifact card from your graveyard to your hand.\n" +
        "−8: Exile the top ten cards of your library. Put all artifact cards from among them onto the battlefield."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.CreatureOrPlaneswalker),
            modification = CostModification.ReduceGenericBy(CostReductionSource.ArtifactsYouControl),
        )
    }

    loyaltyAbility(+2) {
        val artifacts = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
        effect = Effects.DealDamage(artifacts, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(artifacts)
        description = "Tezzeret deals X damage to each opponent, where X is the number of artifacts you control. You gain X life."
    }

    loyaltyAbility(-3) {
        val artifactCard = target(TargetFilter.ArtifactInYourGraveyard)
        effect = Effects.ReturnToHand(artifactCard)
        description = "Return target artifact card from your graveyard to your hand."
    }

    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(10))
            exile(exiled)
            val artifacts = filter(exiled, GameObjectFilter.Artifact)
            move(artifacts, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        description = "Exile the top ten cards of your library. Put all artifact cards from among them onto the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "275"
        inBooster = false
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ee1eea2-961f-445d-b035-6baed454f289.jpg?1783933355"

        ruling("2019-05-03", "Tezzeret himself doesn't have affinity for artifacts.")
        ruling("2019-05-03", "Affinity can reduce only generic mana in a spell's cost. It can't reduce color requirements.")
        ruling("2019-05-03", "If there are any additional costs or cost increases that would apply to a spell with affinity, apply those before applying any cost reductions.")
        ruling("2019-05-03", "In a Two-Headed Giant game, Tezzeret's first loyalty ability causes the opposing team to lose twice X life and you gain X life.")
    }
}
