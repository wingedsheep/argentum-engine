package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kefnet's Monument
 * {3}
 * Legendary Artifact
 * Blue creature spells you cast cost {1} less to cast.
 * Whenever you cast a creature spell, target creature an opponent controls doesn't untap during
 * its controller's next untap step.
 *
 * The [OketrasMonument] cost line, and the [com.wingedsheep.mtg.sets.definitions.drk.cards.BarlsCage]
 * "doesn't untap during its controller's next untap step" lock on a cast trigger. Only *blue*
 * creature spells are cheaper; the trigger fires on any creature spell you cast.
 */
val KefnetsMonument = card("Kefnet's Monument") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "Blue creature spells you cast cost {1} less to cast.\n" +
        "Whenever you cast a creature spell, target creature an opponent controls doesn't untap during its controller's next untap step."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.withColor(Color.BLUE)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Titus Lunter"
        flavorText = "\"The worthy shall cultivate a nimble mind to perceive the glorious wonders that await them.\"\n—Monument inscription"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5d20f0a-aa7c-4d40-acb2-2b86666afd53.jpg"
    }
}
