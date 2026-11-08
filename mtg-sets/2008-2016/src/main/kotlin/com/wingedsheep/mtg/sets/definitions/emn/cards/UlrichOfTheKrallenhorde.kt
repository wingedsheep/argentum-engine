package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ulrich of the Krallenhorde // Ulrich, Uncontested Alpha (Eldritch Moon #191)
 *
 * Front: {3}{R}{G} Legendary Creature — Human Werewolf 4/4
 * Whenever this creature enters or transforms into Ulrich of the Krallenhorde, target creature
 * gets +4/+4 until end of turn.
 * At the beginning of each upkeep, if no spells were cast last turn, transform Ulrich of the
 * Krallenhorde.
 *
 * Back: Legendary Creature — Werewolf 6/6 (red and green color indicator)
 * Whenever this creature transforms into Ulrich, Uncontested Alpha, you may have it fight target
 * non-Werewolf creature you don't control.
 * At the beginning of each upkeep, if a player cast two or more spells last turn, transform Ulrich.
 *
 * - "Enters or transforms into" is two triggers on the front face, as on Huntmaster of the Fells:
 *   `Triggers.self.enters()` and `Triggers.self.transforms(false)` (a back-to-front flip only).
 * - The back face's fight trigger is `Triggers.self.transforms(true)`. The target is chosen as the
 *   trigger goes on the stack; `optional = true` puts the "you may" on resolution (ruling). If Ulrich
 *   has left the battlefield by then, the fight deals no damage either way.
 * - "You don't control" is `Not(ControlledByYou)`, not "an opponent controls", so it stays faithful
 *   in any format.
 */
private val UlrichOfTheKrallenhordeFront = card("Ulrich of the Krallenhorde") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Werewolf"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature enters or transforms into Ulrich of the Krallenhorde, " +
        "target creature gets +4/+4 until end of turn.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform Ulrich of " +
        "the Krallenhorde."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(4, 4, creature)
        description = "Whenever this creature enters, target creature gets +4/+4 until end of turn."
    }
    triggeredAbility {
        trigger = Triggers.self.transforms(false)
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(4, 4, creature)
        description = "Whenever this creature transforms into Ulrich of the Krallenhorde, target " +
            "creature gets +4/+4 until end of turn."
    }
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "191"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e2011f0-a640-4579-bd67-1dfbc09b8c09.jpg?1783937433"
        ruling(
            "2016-07-13",
            "The abilities that transform a Werewolf back and forth look at the entire previous " +
                "turn, even if the Werewolf with that ability wasn't on the battlefield for some or " +
                "all of that turn."
        )
    }
}

private val UlrichUncontestedAlpha = card("Ulrich, Uncontested Alpha") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "RG"
    typeLine = "Legendary Creature — Werewolf"
    power = 6
    toughness = 6
    oracleText = "Whenever this creature transforms into Ulrich, Uncontested Alpha, you may have " +
        "it fight target non-Werewolf creature you don't control.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, " +
        "transform Ulrich."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature
                    .notSubtype(Subtype("Werewolf"))
                    .withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou))
            )
        )
        optional = true
        effect = Effects.Fight(EffectTarget.Self, theirs)
        description = "Whenever this creature transforms into Ulrich, Uncontested Alpha, you may " +
            "have it fight target non-Werewolf creature you don't control."
    }
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "191"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/back/3/e/3e2011f0-a640-4579-bd67-1dfbc09b8c09.jpg?1783937433"
        ruling(
            "2016-07-13",
            "You choose the target of the Ulrich, Uncontested Alpha's first ability as it goes on " +
                "the stack, but you choose whether or not to fight as that ability resolves."
        )
        ruling(
            "2016-07-13",
            "If the targeted creature is an illegal target when Ulrich, Uncontested Alpha's first " +
                "ability tries to resolve, the ability doesn't resolve."
        )
        ruling(
            "2016-07-13",
            "If Ulrich is no longer on the battlefield as Ulrich, Uncontested Alpha's first ability " +
                "resolves, the targeted creature won't deal or be dealt damage."
        )
        ruling(
            "2016-07-13",
            "Ulrich, Uncontested Alpha's last ability only triggers if a single player has cast two " +
                "or more spells during the previous turn. If multiple players each cast just one " +
                "spell during the previous turn, the ability won't trigger."
        )
    }
}

val UlrichOfTheKrallenhorde: CardDefinition =
    CardDefinition.doubleFacedCreature(UlrichOfTheKrallenhordeFront, UlrichUncontestedAlpha)
