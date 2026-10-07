package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grothama, All-Devouring {3}{G}{G}
 * Legendary Creature — Wurm
 * 10/8
 *
 * Other creatures have "Whenever this creature attacks, you may have it fight
 * Grothama, All-Devouring."
 * When Grothama leaves the battlefield, each player draws cards equal to the
 * amount of damage dealt to Grothama this turn by sources they controlled.
 *
 * The "have it fight Grothama" granted ability is modeled with a `TargetCreature`
 * requirement filtered by name — the oracle text names Grothama literally, so the
 * granted ability targets a creature named "Grothama, All-Devouring". The optional
 * `may` clause is honored by the engine's standard target-skip path: when the
 * controller of the granted trigger declines to choose, the trigger resolves with
 * no effect.
 *
 * The LTB draw effect reads a new per-source-controller damage tracker on Grothama
 * (captured as last-known info on the leave-battlefield event). See
 * `DamageDealtByPlayersThisTurnComponent` and
 * `EachPlayerDrawsForDamageDealtToSourceEffect` in the engine.
 */
val GrothamaAllDevouring = card("Grothama, All-Devouring") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Wurm"
    power = 10
    toughness = 8
    oracleText = "Other creatures have \"Whenever this creature attacks, you may have it " +
        "fight Grothama, All-Devouring.\"\n" +
        "When Grothama, All-Devouring leaves the battlefield, each player draws cards " +
        "equal to the amount of damage dealt to Grothama this turn by sources they controlled."

    val grothamaTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Creature.named("Grothama, All-Devouring")))

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                val grothama = target(grothamaTarget)
                effect = Effects.May(Effects.Fight(EffectTarget.Self, grothama))
            },
            filter = GroupFilter.AllCreatures.other(),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.EachPlayerDrawsForDamageDealtToSource()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab8935b1-ec87-4330-9952-9ef8cd344531.jpg?1783934853"
    }
}
