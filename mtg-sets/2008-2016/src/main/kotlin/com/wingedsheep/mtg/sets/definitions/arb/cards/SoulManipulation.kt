package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul Manipulation
 * {1}{U}{B}
 * Instant
 *
 * Choose one or both —
 * • Counter target creature spell.
 * • Return target creature card from your graveyard to your hand.
 *
 * "Choose one or both" is `modal(chooseCount = 2, minChooseCount = 1)` — `chooseCount` is the
 * maximum, `minChooseCount` the floor (CR 700.2). The first mode is [Effects.CounterSpell] over
 * [Targets.CreatureSpell]; the second is [Effects.ReturnToHand] over
 * [Targets.CreatureCardInYourGraveyard], whose requirement already carries the graveyard zone and
 * the owned-by-you predicate.
 */
val SoulManipulation = card("Soul Manipulation") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n" +
        "• Counter target creature spell.\n" +
        "• Return target creature card from your graveyard to your hand."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Counter target creature spell") {
                target("target", Targets.CreatureSpell)
                effect = Effects.CounterSpell()
            }
            mode("Return target creature card from your graveyard to your hand") {
                val t = target("target", Targets.CreatureCardInYourGraveyard)
                effect = Effects.ReturnToHand(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Carl Critchlow"
        flavorText = "\"Birth and death are both reversible.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcd3cb05-c6f9-435a-a0e7-1f85da4a36eb.jpg"
        ruling("2009-05-01", "You may choose just the first mode (targeting a creature spell), just the second mode (targeting a creature card in your graveyard), or both modes (targeting a creature spell and a creature card in your graveyard). You can’t choose a mode unless there’s a legal target for it.")
        ruling("2009-05-01", "If you choose both modes, you choose their targets at the same time. You can’t counter your own creature spell and then return that card from your graveyard to your hand.")
    }
}
