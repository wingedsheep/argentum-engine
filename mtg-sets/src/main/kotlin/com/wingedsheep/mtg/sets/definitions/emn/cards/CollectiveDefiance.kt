package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Collective Defiance — Eldritch Moon #123. */
val CollectiveDefiance = card("Collective Defiance") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Escalate {1} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or more —\n" +
        "• Target player discards all the cards in their hand, then draws that many cards.\n" +
        "• Collective Defiance deals 4 damage to target creature.\n" +
        "• Collective Defiance deals 3 damage to target opponent or planeswalker."

    spell {
        modal(chooseCount = 3, minChooseCount = 1, additionalManaCostPerExtraMode = "{1}") {
            mode("Target player discards their hand, then draws that many cards.") {
                val player = target("wheel player", TargetPlayer())
                effect = Effects.Composite(
                    Patterns.Hand.discardHand(player),
                    Effects.DrawCards(DynamicAmount.VariableReference("discardedHand_count"), player),
                )
            }
            mode("Collective Defiance deals 4 damage to target creature.") {
                val creature = target("damage creature", TargetCreature())
                effect = Effects.DealDamage(4, creature)
            }
            mode("Collective Defiance deals 3 damage to target opponent or planeswalker.") {
                val opponentOrPlaneswalker = target(
                    "damage opponent or planeswalker",
                    TargetOpponentOrPlaneswalker(),
                )
                effect = Effects.DealDamage(3, opponentOrPlaneswalker)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8960883f-3813-412b-9a5b-f8cf8d566fac.jpg?1783937466"
        ruling("2016-07-13", "You choose all of your modes at once. You can't wait to perform one mode's actions and then decide to choose more modes.")
        ruling("2016-07-13", "If one target of an escalate spell becomes illegal, the other targets will still be affected. If all of the targets become illegal, the spell won't resolve.")
    }
}
