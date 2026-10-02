package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.OptionalSkipTurnWith

val TimeVault = card("Time Vault") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\nThis artifact doesn't untap during your untap step.\nIf you would begin your turn while this artifact is tapped, you may skip that turn instead. If you do, untap this artifact.\n{T}: Take an extra turn after this one."
    flags(AbilityFlag.DOESNT_UNTAP)
    replacementEffect(EntersTapped())
    replacementEffect(OptionalSkipTurnWith(
        Effects.Pipeline(descriptionOverride = "Untap this artifact") {
            run(Effects.Untap(EffectTarget.Self))
        },
        restrictions = listOf(Conditions.SourceIsTapped)
    ))
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.TakeExtraTurn()
        description = "Take an extra turn"
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "274"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/902441dc-c976-4c92-b897-6376eaa0fe38.jpg?1783948660"
        ruling("2022-12-08", "If multiple \"extra turn\" effects resolve in the same turn, take them in the reverse of the order that the effects resolved. In other words, the most recently created extra turn is taken first.")
        ruling("2008-10-01", "The choice of whether or not to skip a turn is made as that turn would begin, and only if Time Vault is tapped at that time. If you choose to skip your turn, Time Vault untaps before anything else happens in the next turn.")
        ruling("2008-10-01", "This wording means that you no longer end up skipping multiple turns if you find a way to activate the ability multiple times.")
    }
}
