package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hand of Vecna
 * {3}
 * Legendary Artifact — Equipment
 * At the beginning of combat on your turn, equipped creature or a creature you control named Vecna
 * gets +X/+X until end of turn, where X is the number of cards in your hand.
 * Equip—Pay 1 life for each card in your hand.
 * Equip {2}
 *
 * The combat trigger doesn't target: as it resolves, the controller chooses one creature among the
 * equipped creature (whoever controls it) and the creatures they control named Vecna (2021-07-23
 * ruling), so it is a gather → `chooseExactly(1)` → pump pipeline. X is read once, as the pump
 * resolves, and stays locked in (second ruling). With no candidate the pump simply has nothing to
 * apply to. On the Vecna token The Book of Vile Darkness creates, the copied trigger's source is
 * Vecna itself — attached to nothing — so the "named Vecna" branch picks Vecna.
 */
val HandOfVecna = card("Hand of Vecna") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "At the beginning of combat on your turn, equipped creature or a creature you control named Vecna " +
        "gets +X/+X until end of turn, where X is the number of cards in your hand.\n" +
        "Equip—Pay 1 life for each card in your hand.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Pipeline(
            descriptionOverride = "Equipped creature or a creature you control named Vecna gets +X/+X until " +
                "end of turn, where X is the number of cards in your hand."
        ) {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature.attachedToBySource() or
                        GameObjectFilter.Creature.youControl().named("Vecna"),
                    player = Player.Each,
                )
            )
            val chosen = chooseExactly(
                1,
                from = candidates,
                prompt = "Choose a creature to get +X/+X, where X is the number of cards in your hand",
                useTargetingUI = true,
            )
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.ModifyStats(
                        DynamicAmounts.cardsInYourHand(),
                        DynamicAmounts.cardsInYourHand(),
                        EffectTarget.IterationEntity,
                    )
                )
            )
        }
    }

    activatedAbility {
        isEquipAbility = true
        cost = Costs.PayLife(DynamicAmounts.cardsInYourHand())
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        timing = TimingRule.SorcerySpeed
        description = "Equip—Pay 1 life for each card in your hand."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddd0beb7-80dc-4665-bab2-747c795d97db.jpg?1783926438"
        ruling(
            "2021-07-23",
            "If you control both Vecna and Hand of Vecna equipped to another creature, you choose which " +
                "creature to apply the bonus to as the triggered ability resolves."
        )
        ruling(
            "2021-07-23",
            "Hand of Vecna's triggered ability uses the number of cards in your hand as it resolves to " +
                "determine the bonus the creature gets. It won't change if the number of cards in your hand " +
                "changes later in the turn."
        )
    }
}
