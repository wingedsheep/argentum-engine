package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wicked Guardian
 * {3}{B}
 * Creature — Human Noble
 * 4/2
 *
 * When this creature enters, you may have it deal 2 damage to another creature you control.
 * If you do, draw a card.
 *
 * The damage doesn't target: the creature is chosen as the trigger resolves, so it is a gather →
 * `chooseExactly(1)` pipeline inside [Effects.IfYouDo]. "If you do" reads the *choice*
 * (`CollectionNonEmpty`), not the damage dealt — per the 2019-10-04 ruling a prevented hit still
 * draws. With no other creature you control, nothing is chosen and no card is drawn.
 */
val WickedGuardian = card("Wicked Guardian") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Noble"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, you may have it deal 2 damage to another creature you control. " +
        "If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val candidates = gather(
                        CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Creature,
                            player = Player.You,
                            excludeSelf = true
                        )
                    )
                    // Named: the "if you do" check reads the choice from outside the pipeline.
                    val damaged = chooseExactly(
                        1,
                        from = candidates,
                        prompt = "Choose another creature you control to deal 2 damage to",
                        useTargetingUI = true,
                        name = "wickedGuardianDamaged"
                    )
                    run(Effects.ForEachInCollection(damaged, Effects.DealDamage(2, EffectTarget.IterationEntity)))
                },
                then = Effects.DrawCards(1),
                successCriterion = SuccessCriterion.CollectionNonEmpty("wickedGuardianDamaged")
            )
        )
        description = "When this creature enters, you may have it deal 2 damage to another creature you " +
            "control. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Matt Stewart"
        flavorText = "\"Some are born to greatness. You were born to scrub greatness's floors.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cd91b2-0f9b-4582-ad90-32fa3ee1fde7.jpg?1783932630"
        ruling(
            "2019-10-04",
            "If Wicked Guardian leaves the battlefield before its triggered ability resolves, you may still " +
                "have it deal 2 damage to another creature you control and draw a card."
        )
        ruling(
            "2019-10-04",
            "If the damage Wicked Guardian deals is prevented (perhaps because you chose for it to deal 2 " +
                "damage to a creature with protection from black), you still draw a card."
        )
    }
}
