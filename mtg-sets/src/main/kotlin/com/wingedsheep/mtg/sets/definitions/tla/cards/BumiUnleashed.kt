package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bumi, Unleashed — {3}{R}{G}
 * Legendary Creature — Human Noble Ally
 * 5/4
 * Trample
 * When Bumi enters, earthbend 4.
 * Whenever Bumi deals combat damage to a player, untap all lands you control. After this phase,
 * there is an additional combat phase. Only land creatures can attack during that combat phase.
 *
 * The ETB reuses [Effects.Earthbend] (animate a target land into a 0/0 creature-land, add four
 * +1/+1 counters, grant haste and the return-tapped self-trigger). The combat-damage trigger is a
 * single composite of three atoms:
 *  - untap every land you control — `ForEachInGroup` over [GameObjectFilter.Land] + [Effects.Untap];
 *  - [Effects.AddCombatPhaseRestrictedTo] with a "land creature" filter
 *    (`GameObjectFilter.Creature and GameObjectFilter.Land`) — inserts one extra combat phase in
 *    which only creatures that are also lands may be declared as attackers.
 * "Land creatures" are animated lands (earthbended lands, manlands), matched with projected state.
 */
val BumiUnleashed = card("Bumi, Unleashed") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 5
    toughness = 4
    oracleText = "Trample\n" +
        "When Bumi enters, earthbend 4.\n" +
        "Whenever Bumi deals combat damage to a player, untap all lands you control. After this " +
        "phase, there is an additional combat phase. Only land creatures can attack during that " +
        "combat phase."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(4, land)
        description = "When Bumi enters, earthbend 4."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Land.youControl()),
                Effects.Untap(EffectTarget.Self),
            ),
            Effects.AddCombatPhaseRestrictedTo(GameObjectFilter.Creature and GameObjectFilter.Land),
        )
        description = "Whenever Bumi deals combat damage to a player, untap all lands you control. " +
            "After this phase, there is an additional combat phase. Only land creatures can attack " +
            "during that combat phase."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Toni Infante"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b20a5185-7e31-4fc9-be11-9423bfc389bf.jpg?1764121501"
    }
}
