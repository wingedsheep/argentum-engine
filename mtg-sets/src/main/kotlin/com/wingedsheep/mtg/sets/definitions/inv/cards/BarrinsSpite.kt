package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Barrin's Spite
 * {2}{U}{B}
 * Sorcery
 * Choose two target creatures controlled by the same player. Their controller chooses
 * and sacrifices one of them. Return the other to its owner's hand.
 *
 * Composed entirely from the pipeline primitives:
 *   Gather(ChosenTargets) → SelectFromCollection (the targets' controller picks one) →
 *   MoveCollection(Sacrifice the chosen) + MoveCollection(return the rest to hand).
 *
 * The "controlled by the same player" constraint is enforced at cast time via
 * [TargetCreature]'s `sameController` flag. The deciding player is the creatures'
 * controller — possibly the spell's controller, since you may target your own creatures —
 * resolved by [Chooser.ControllerOfSelection].
 *
 * Ruling (2008-05-01): the controller chooses which one to sacrifice as the spell
 * resolves; if only one of the two targets is still legal at that time, that one must be
 * sacrificed. This falls out for free — the stack resolver strips illegal targets before
 * the effect runs, so the gathered collection holds only the surviving creature(s):
 * ChooseExactly(1) auto-picks it for sacrifice and the (empty) remainder bounces nothing.
 */
val BarrinsSpite = card("Barrin's Spite") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Choose two target creatures controlled by the same player. Their controller chooses and sacrifices one of them. Return the other to its owner's hand."

    spell {
        target = TargetCreature(count = 2, sameController = true)
        effect = Effects.Composite(
            listOf(
                // 1. Reference the two targeted creatures (still on the battlefield).
                GatherCardsEffect(
                    source = CardSource.ChosenTargets,
                    storeAs = "spiteCreatures"
                ),
                // 2. Their controller chooses one to sacrifice; the other is the remainder.
                SelectFromCollectionEffect(
                    from = "spiteCreatures",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.ControllerOfSelection,
                    storeSelected = "sacrificed",
                    storeRemainder = "returned",
                    useTargetingUI = true,
                    prompt = "Choose one of the two creatures to sacrifice"
                ),
                // 3. Sacrifice the chosen creature (owner's graveyard, fires sacrifice triggers).
                MoveCollectionEffect(
                    from = "sacrificed",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice
                ),
                // 4. Return the other to its owner's hand.
                MoveCollectionEffect(
                    from = "returned",
                    destination = CardDestination.ToZone(Zone.HAND)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Terese Nielsen"
        flavorText = "\"Only vengeance matters now.\"\n—Barrin"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d8ec4dc-c74a-4d49-856e-95703675fe9b.jpg?1562916937"
    }
}
