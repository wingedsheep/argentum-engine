package com.wingedsheep.mtg.sets.definitions.bfz.cards

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
 * Slab Hammer — Battle for Zendikar #227
 * {2} · Artifact — Equipment
 *
 * Whenever equipped creature attacks, you may return a land you control to its owner's hand.
 * If you do, the creature gets +2/+2 until end of turn.
 * Equip {2}
 *
 * The attack trigger is Sword of the Animist's `Triggers.attached.attacks()`, whose triggering
 * entity is the attacking equipped creature — so "the creature" stays the one that attacked even
 * if the Hammer moves before resolution. The land isn't targeted; it is chosen on resolution
 * (Temur Sabertooth's gather → `chooseExactly(1)` → `toHand` inside [Effects.IfYouDo]). The gate
 * reads the named choice rather than the growth of your own hand, because a land you control but
 * don't own goes to its owner's hand.
 */
val SlabHammer = card("Slab Hammer") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, you may return a land you control to its owner's hand. " +
        "If you do, the creature gets +2/+2 until end of turn.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
                    val returned = chooseExactly(
                        1,
                        from = lands,
                        prompt = "Return a land you control to its owner's hand",
                        useTargetingUI = true,
                        name = "returnedLand"
                    )
                    toHand(returned)
                },
                then = Effects.ModifyStats(2, 2, EffectTarget.TriggeringEntity),
                successCriterion = SuccessCriterion.CollectionNonEmpty("returnedLand")
            )
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Joseph Meehan"
        flavorText = "Neither subtle nor pretty."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18c64671-9bdd-475f-8def-7575775bf900.jpg?1783938176"
    }
}
