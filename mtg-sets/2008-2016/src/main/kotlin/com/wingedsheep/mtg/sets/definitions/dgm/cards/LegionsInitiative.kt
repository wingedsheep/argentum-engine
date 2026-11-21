package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legion's Initiative
 * {R}{W}
 * Enchantment
 * Red creatures you control get +1/+0.
 * White creatures you control get +0/+1.
 * {R}{W}, Exile this enchantment: Exile all creatures you control. At the beginning of the next
 * combat, return those cards to the battlefield under their owner's control and those creatures
 * gain haste until end of turn.
 *
 * The two anthems are separate static abilities, so a red-and-white creature gets +1/+1 (ruling).
 *
 * The exiled pile rides the delayed trigger (`carryCollections`, CR 603.7c "those cards"): a token
 * ceases to exist in exile and a card that has since left exile is dropped when it fires. The return
 * is a *tracked* move, so haste goes only to the creatures that actually came back. "The next
 * combat" is the next beginning of combat step of any player's turn; activated during a beginning
 * of combat step, the trigger waits for the following one, since that step has already begun.
 */
val LegionsInitiative = card("Legion's Initiative") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "Red creatures you control get +1/+0.\n" +
        "White creatures you control get +0/+1.\n" +
        "{R}{W}, Exile this enchantment: Exile all creatures you control. At the beginning of the next " +
        "combat, return those cards to the battlefield under their owner's control and those creatures " +
        "gain haste until end of turn."

    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter(GameObjectFilter.Creature.withColor(Color.RED).youControl()))
    }

    staticAbility {
        ability = ModifyStats(0, 1, GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE).youControl()))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}{W}"), Costs.ExileSelf)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.BattlefieldMatching(GameObjectFilter.Creature, player = Player.You))
            exile(exiled)
            run(Effects.CreateDelayedTrigger(
                step = Step.BEGIN_COMBAT,
                effect = Effects.Pipeline {
                    val returned = moveTracked(
                        exiled,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        underOwnersControl = true
                    )
                    run(Effects.ForEachInCollection(
                        returned,
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn)
                    ))
                },
                carryCollections = listOf(exiled.key)
            ))
        }
        description = "{R}{W}, Exile this enchantment: Exile all creatures you control. At the beginning of the next " +
            "combat, return those cards to the battlefield under their owner's control and those creatures " +
            "gain haste until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "81"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/672051a6-d232-4546-842a-369d412c38d2.jpg?1783940026"
        ruling("2013-04-15", "The beginning of the next combat will happen even if the player whose turn it is doesn't intend to attack with any creatures.")
        ruling("2013-04-15", "A creature that's both red and white will get +1/+1 from Legion's Initiative.")
    }
}
