package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sigil of Valor — Magic Origins #239 (canonical printing)
 * {2} · Artifact — Equipment
 *
 * Whenever equipped creature attacks alone, it gets +1/+1 until end of turn for each other
 * creature you control.
 * Equip {1}
 *
 * The Equipment's own attack trigger (`Triggers.attached.attacks` + [AttackPredicate.Alone]) binds
 * the lone attacker as [EffectTarget.TriggeringEntity], so the bonus lands on that creature even if
 * the Sigil is moved in response. "Other" is relative to the equipped creature, not the Sigil: the
 * group is the creatures you control minus the triggering entity
 * ([GroupFilter.otherThanTriggeringEntity]). The group is gathered once as the ability resolves and
 * each member adds one +1/+1 until end of turn, so the bonus is fixed at resolution and doesn't
 * change if the number of creatures you control does (2015-06-22 ruling).
 */
val SigilOfValor = card("Sigil of Valor") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks alone, it gets +1/+1 until end of turn for each other " +
        "creature you control.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    // Whenever equipped creature attacks alone, it gets +1/+1 until end of turn for each other
    // creature you control.
    triggeredAbility {
        trigger = Triggers.attached.attacks(setOf(AttackPredicate.Alone))
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl.otherThanTriggeringEntity(),
            effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
        )
        description = "Whenever equipped creature attacks alone, it gets +1/+1 until end of turn for each " +
            "other creature you control."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f134f242-cbb5-4a3d-bfd4-46e8b8669b4b.jpg?1783938307"
        ruling(
            "2015-06-22",
            "Count the number of creatures you control other than the equipped creature as Sigil of Valor's " +
                "ability resolves to determine the amount of the bonus. Once the ability resolves, the bonus " +
                "won't change, even if the number of creatures you control does."
        )
        ruling(
            "2015-06-22",
            "A creature attacks alone if it's the only creature declared as an attacker during the declare " +
                "attackers step (including creatures controlled by your teammates, if applicable). For example, " +
                "Sigil of Valor's ability won't trigger if you attack with multiple creatures and all but one " +
                "of them are removed from combat."
        )
    }
}
