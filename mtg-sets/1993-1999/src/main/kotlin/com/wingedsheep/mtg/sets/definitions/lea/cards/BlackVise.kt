package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Vise — Limited Edition Alpha #233
 * {1} · Artifact
 *
 * As this artifact enters, choose an opponent.
 * At the beginning of the chosen player's upkeep, this artifact deals X damage to that player,
 * where X is the number of cards in their hand minus 4.
 *
 * The Rack's shape: [EntersWithChoice]`(ChoiceType.OPPONENT)` stores the chosen player on the
 * permanent, `Triggers.chosenOpponent.beginningOf(Step.UPKEEP)` fires only on that player's upkeep,
 * and both the damage recipient and the hand count read [Player.ChosenOpponent]. With four or fewer
 * cards in hand X is zero or negative and no damage is dealt.
 */
val BlackVise = card("Black Vise") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose an opponent.\n" +
        "At the beginning of the chosen player's upkeep, this artifact deals X damage to that " +
        "player, where X is the number of cards in their hand minus 4."

    replacementEffect(EntersWithChoice(ChoiceType.OPPONENT))

    triggeredAbility {
        trigger = Triggers.chosenOpponent.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.zone(Player.ChosenOpponent, Zone.HAND).count() - 4,
            target = EffectTarget.PlayerRef(Player.ChosenOpponent)
        )
        description = "At the beginning of the chosen player's upkeep, Black Vise deals damage to " +
            "that player equal to the number of cards in their hand minus 4."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76ac72f8-5b1e-4d67-a796-ef69cde27424.jpg?1783948669"
        ruling(
            "2009-10-01",
            "If the chosen player has four or fewer cards in their hand as Black Vise's ability " +
                "resolves, the ability just won't do anything that turn."
        )
        ruling(
            "2004-10-04",
            "You choose one opposing player as it enters and it only affects that one player. This " +
                "choice is not changed even if Black Vise changes controllers. It becomes useless but " +
                "stays on the battlefield if that player leaves the game."
        )
    }
}
