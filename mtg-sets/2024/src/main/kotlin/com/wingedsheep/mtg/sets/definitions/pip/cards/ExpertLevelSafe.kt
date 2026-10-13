package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

val ExpertLevelSafe = card("Expert-Level Safe") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile the top two cards of your library face down.\n{1}, {T}: You and target opponent each secretly choose 1, 2, or 3. Then those choices are revealed. If they match, sacrifice this artifact and put all cards exiled with it into their owners' hands. Otherwise, exile the top card of your library face down."

    val exileTopCard = Effects.Pipeline {
        val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
        exile(top, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
    }
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(2)))
            exile(top, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
        }
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val opponent = target(Targets.Opponent)
        effect = Effects.SecretNumbers(
            participants = listOf(Player.You, opponent.asPlayer),
            minValue = 1, maxValue = DynamicAmounts.fixed(3),
            matchedEffect = Effects.Pipeline {
                // Capture the linked pile before sacrificing its source.
                val pile = gather(CardSource.FromLinkedExile())
                run(Effects.SacrificeTarget(EffectTarget.Self))
                run(Effects.ForEachInCollection(pile, Effects.Move(EffectTarget.IterationEntity, Zone.HAND)))
            },
            unmatchedEffect = exileTopCard,
        )
        description = "Secretly choose 1, 2, or 3 with target opponent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/499f4bdc-8b6a-4e87-92fd-8368537a6ced.jpg?1783912379"
        ruling("2024-03-08", "You don’t get to look at the cards exiled by Expert-Level Safe’s abilities.")
        ruling("2024-03-08", "To secretly choose the numbers, you and the target opponent should each write down your choice of 1, 2, or 3 without showing them to anyone else. Keep those choices secret until they’re simultaneously revealed.")
    }
}
