package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tezzeret, Master of Metal — Aether Revolt #190 (Planeswalker Deck)
 * {4}{U}{B} · Legendary Planeswalker — Tezzeret · Loyalty 5
 *
 * +1: Reveal cards from the top of your library until you reveal an artifact card. Put that card
 *     into your hand and the rest on the bottom of your library in a random order.
 * −3: Target opponent loses life equal to the number of artifacts you control.
 * −8: Gain control of all artifacts and creatures target opponent controls.
 *
 * The +1 is Ajani, Valiant Protector's reveal-until shape with an artifact filter — with no artifact
 * card in the library the whole library is revealed and goes to the bottom in a random order (the
 * ruling's "reveal your library and then randomize it"). The −3 counts artifacts as it resolves. The
 * −8 is Ashiok, Sculptor of Fears' "gain control of all … target opponent controls" over the
 * artifact-or-creature union, with a permanent control change ("lasts indefinitely").
 */
val TezzeretMasterOfMetal = card("Tezzeret, Master of Metal") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 5
    oracleText = "+1: Reveal cards from the top of your library until you reveal an artifact card. " +
        "Put that card into your hand and the rest on the bottom of your library in a random order.\n" +
        "−3: Target opponent loses life equal to the number of artifacts you control.\n" +
        "−8: Gain control of all artifacts and creatures target opponent controls."

    // +1: Reveal until an artifact card; it goes to hand, the rest to the bottom in a random order.
    loyaltyAbility(+1) {
        effect = Patterns.Library.revealUntilMatchToHand(filter = GameObjectFilter.Artifact)
        description = "Reveal cards from the top of your library until you reveal an artifact card. " +
            "Put that card into your hand and the rest on the bottom of your library in a random order."
    }

    // −3: Target opponent loses life equal to the number of artifacts you control.
    loyaltyAbility(-3) {
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            opponent
        )
        description = "Target opponent loses life equal to the number of artifacts you control."
    }

    // −8: Gain control of all artifacts and creatures target opponent controls.
    loyaltyAbility(-8) {
        val opponent = target(Targets.Opponent)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.CreatureOrArtifact.targetPlayerControls(opponent)),
            Effects.GainControl(EffectTarget.IterationEntity, Duration.Permanent)
        )
        description = "Gain control of all artifacts and creatures target opponent controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "190"
        inBooster = false
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7c6614c-cc2b-4e5b-9c0d-ce8e4b2d8ea7.jpg?1783936714"

        ruling("2017-02-09", "If you don't have an artifact card in your library while resolving Tezzeret's first ability, you'll reveal your library and then randomize it.")
        ruling("2017-02-09", "The effect of Tezzeret's third ability lasts indefinitely.")
    }
}
