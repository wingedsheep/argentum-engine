package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

// Current Oracle labels both piles; flying is checked when a blocker is declared.
val RagingRiver = card("Raging River") {
    manaCost = "{R}{R}"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures you control attack, each defending player divides all creatures without flying they control into a \"left\" pile and a \"right\" pile. Then, for each attacking creature you control, choose \"left\" or \"right.\" That creature can't be blocked this combat except by creatures with flying and creatures in a pile with the chosen label."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.Pipeline {
            // Finish every defending player's partition before choosing sides for any attacker.
            val (left, right) = forEachPlayerCollecting(Player.EachDefendingPlayer) {
                val defenders = gather(CardSource.BattlefieldMatching(
                    GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING).youControl()
                ))
                val piles = chooseAnyNumberSplit(
                    defenders,
                    prompt = "Select creatures for the left pile; the rest form the right pile",
                    selectedLabel = "left",
                    remainderLabel = "right",
                    useTargetingUI = true
                )
                listOf(piles.selected, piles.remainder)
            }
            run(Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl().attacking()),
                Effects.Pipeline {
                    val piles = choosePile(left, right,
                        pileALabel = "left", pileBLabel = "right",
                        prompt = "Choose left or right for this attacking creature")
                    run(Effects.GrantCantBeBlockedExceptByCollection(
                        target = EffectTarget.IterationEntity,
                        collection = piles.chosen,
                        alternativeFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
                        duration = Duration.EndOfCombat
                    ))
                }
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61e4f56d-1f4f-49f2-8534-0d09196a3327.jpg?1783948682"
        ruling("2008-05-01", "If a creature is put onto the battlefield attacking after the ability resolves, it can be blocked by any creature that could normally block it (including creatures that entered after the ability resolved).")
    }
}
