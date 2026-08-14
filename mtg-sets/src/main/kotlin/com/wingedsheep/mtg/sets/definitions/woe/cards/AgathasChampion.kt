package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Agatha's Champion
 * {4}{G}
 * Creature — Human Knight
 * 4/4
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Trample
 * When this creature enters, if it was bargained, it fights up to one target creature you don't
 * control.
 *
 * The permanent shape of bargain (CR 702.166b), the same one [HighFaeNegotiator] uses: the bargained
 * fact is stamped on the spell as it's cast and rides the permanent it becomes, so the enters
 * trigger can still read it. Modelled as an intervening-'if' clause (CR 603.4) on
 * [Conditions.WasBargained] — an unbargained cast never puts the ability on the stack at all, and
 * per the WOE ruling you may still bargain the spell even when no legal fight target exists.
 *
 * Unlike [CurseOfTheWerefox], the fight target here is chosen when the *enters* ability goes on the
 * stack (a normal triggered ability, not a reflexive one), so no pipeline snapshot is needed:
 * combatant one is [EffectTarget.Self] and combatant two is the named target. `optional = true` on
 * [TargetCreature] is the "up to one" — declining simply skips the fight, and if the chosen creature
 * has become an illegal target by resolution the ability is countered for having no legal targets.
 */
val AgathasChampion = card("Agatha's Champion") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Trample\n" +
        "When this creature enters, if it was bargained, it fights up to one target creature you " +
        "don't control. (Each deals damage equal to its power to the other.)"

    bargain()

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasBargained
        val foe = target(
            "up to one target creature you don't control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls),
        )
        effect = Effects.Fight(EffectTarget.Self, foe)
        description = "When this creature enters, if it was bargained, it fights up to one target " +
            "creature you don't control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92652c41-1239-4299-a486-11fe1a96e912.jpg?1783915084"

        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "You can bargain a permanent spell even if you won't be able to choose targets for an " +
                "enters-the-battlefield ability of that permanent once the spell resolves."
        )
        ruling(
            "2023-09-01",
            "If a card or token enters the battlefield as a copy of a permanent that's already on " +
                "the battlefield, the new permanent isn't bargained, even if the original was."
        )
    }
}
